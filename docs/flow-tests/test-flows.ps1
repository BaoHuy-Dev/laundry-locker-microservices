# ============================================================================
# Smart Laundry Locker — end-to-end flow tests (simulated IoT/drone)
#
# Prereqs (see README.md in this folder):
#   1) docker compose up -d            (backend @ http://localhost:18080)
#   2) python -u simulate_demo_cabinet.py   (in ../smart-locker-iot — answers
#      MQTT open commands; needs internet for the public HiveMQ broker)
# Run:  powershell -ExecutionPolicy Bypass -File .\test-flows.ps1
#
# Roles under test: CUSTOMER, ADMIN (bootstrap account), TECHNICIAN (locker
# upkeep + IoT), MAINTENANCE (drone team). MANAGER/STAFF were removed.
# ============================================================================
$ErrorActionPreference = 'Continue'
$GW = 'http://localhost:18080'
# Matches docker-compose defaults (override via BOOTSTRAP_ADMIN_* in .env).
$ADMIN_EMAIL = 'admin@lockerly.local'
$ADMIN_PASSWORD = 'Admin@123456'
$script:results = New-Object System.Collections.ArrayList

function Api {
  param([string]$Method, [string]$Path, [string]$Token, $Body)
  $headers = @{}
  if ($Token) { $headers['Authorization'] = "Bearer $Token" }
  $params = @{ Method = $Method; Uri = "$GW$Path"; Headers = $headers; TimeoutSec = 90 }
  if ($null -ne $Body) {
    $params['Body'] = ($Body | ConvertTo-Json -Depth 8)
    $params['ContentType'] = 'application/json; charset=utf-8'
  }
  try {
    $resp = Invoke-RestMethod @params
    return @{ ok = $true; status = 200; code = $resp.code; msg = $resp.message; data = $resp.data }
  } catch {
    $status = 0; $body = $null
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode
      try {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd() | ConvertFrom-Json
      } catch {}
    }
    return @{ ok = $false; status = $status; code = $(if($body){$body.code}); msg = $(if($body){$body.message}else{$_.Exception.Message}); data = $null }
  }
}

function Check {
  param([string]$Name, [bool]$Cond, [string]$Extra = '')
  $tag = if ($Cond) { 'PASS' } else { 'FAIL' }
  $line = "[$tag] $Name $Extra"
  Write-Host $line
  [void]$script:results.Add($line)
}

function Register {
  param([string]$Email, [string]$Phone, [string]$First, [string]$Pass)
  return Api POST '/api/auth/register' $null @{ email = $Email; phoneNumber = $Phone; firstName = $First; lastName = 'Test'; password = $Pass }
}
function Login {
  param([string]$Identifier, [string]$Pass)
  return Api POST '/api/auth/login' $null @{ identifier = $Identifier; password = $Pass }
}

# Wait until the gateway routes are warm (services register with Eureka lazily).
$ready = $false
for ($i = 0; $i -lt 36 -and -not $ready; $i++) {
  try { $null = Invoke-RestMethod "$GW/api/lockers" -TimeoutSec 8; $ready = $true }
  catch { Start-Sleep -Seconds 5 }
}
if (-not $ready) { Write-Host 'Gateway/routes not ready after 180s — aborting.'; exit 1 }

$suffix = Get-Date -Format 'HHmmss'
$PASS = 'Passw0rd!123'
$users = @{
  cust1 = @{ email = "cust1.$suffix@test.local"; phone = "090$suffix"+"1" }
  cust2 = @{ email = "cust2.$suffix@test.local"; phone = "090$suffix"+"2" }
  tech  = @{ email = "tech.$suffix@test.local";  phone = "090$suffix"+"4" }
  maint = @{ email = "maint.$suffix@test.local"; phone = "090$suffix"+"5" }
}

Write-Host "`n=== 0. SEED: bootstrap admin + register users + assign roles ==="
$r = Login $ADMIN_EMAIL $ADMIN_PASSWORD
Check 'bootstrap ADMIN login works (no manual SQL)' ($r.ok -and $r.data.roles -contains 'ADMIN') "(roles=$($r.data.roles -join ','))"
$ADM = $r.data.accessToken

foreach ($k in @('cust1','cust2','tech','maint')) {
  $r = Register $users[$k].email $users[$k].phone $k $PASS
  Check "register $k" $r.ok "(userId=$($r.data.userId))"
  $users[$k].userId = $r.data.userId
  $users[$k].token = $r.data.accessToken
}

$r = Api PUT "/api/admin/users/$($users.tech.userId)/roles" $ADM @{ roles = @('TECHNICIAN') }
Check 'admin sets TECHNICIAN role' $r.ok "($($r.code))"
$r = Api PUT "/api/admin/users/$($users.maint.userId)/roles" $ADM @{ roles = @('MAINTENANCE') }
Check 'admin sets MAINTENANCE role' $r.ok "($($r.code))"
$r = Login $users.tech.email $PASS;  $users.tech.token = $r.data.accessToken
Check 'tech re-login has TECHNICIAN' ($r.data.roles -contains 'TECHNICIAN') ''
$r = Login $users.maint.email $PASS; $users.maint.token = $r.data.accessToken
Check 'maint re-login has MAINTENANCE' ($r.data.roles -contains 'MAINTENANCE') ''

$r = Api POST "/api/admin/wallet/$($users.cust1.userId)/adjust" $ADM @{ amount = 500000; reason = 'test seed' }
Check 'admin tops up cust1 wallet' $r.ok "($($r.code))"

$r = Api GET '/api/lockers' $null $null
$locker = $r.data | Where-Object { $_.code -eq 'CAB-DEMO-01' } | Select-Object -First 1
Check 'demo locker CAB-DEMO-01 exists' ($null -ne $locker) "(id=$($locker.id))"
$LID = $locker.id
$r = Api GET "/api/lockers/$LID/layout" $null $null
$cells = $r.data.cells
Check 'layout has 10 cells + landing pad' (($cells.Count -eq 10) -and ($r.data.landingPad -eq $true)) "(cells=$($cells.Count))"
$stdBox   = $cells | Where-Object { $_.cellType -eq 'STANDARD' -and $_.status -eq 'AVAILABLE' } | Select-Object -First 1
$droneBox = $cells | Where-Object { $_.cellType -eq 'DRONE' -and $_.status -eq 'AVAILABLE' } | Select-Object -First 1

# ============================================================================
Write-Host "`n=== 2.1 BOOKING: SEND -> pay -> confirm -> IoT unlock -> complete -> admin stats ==="
$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; receiverPhone = $users.cust2.phone; receiverName = 'Cust2'; note = 'flow-2.1' }
Check 'SEND order created' $r.ok "(id=$($r.data.id) box=$($r.data.sendBoxId) status=$($r.data.status))"
$O1 = $r.data.id; $O1BOX = $r.data.sendBoxId

$r = Api POST '/api/payments/checkout' $users.cust1.token @{ orderId = $O1; method = 'WALLET' }
Check 'checkout WALLET completes' ($r.ok -and ($r.data.status -in @('COMPLETED','PAID'))) "(status=$($r.data.status))"
$r = Api GET "/api/payments/order/$O1" $users.cust1.token $null
Check 'payment recorded for order' ($r.ok -and $r.data.Count -ge 1) "(n=$($r.data.Count))"

$r = Api PUT "/api/orders/$O1/confirm" $users.cust1.token $null
Check 'confirm drop -> STORING (new pickup PIN for receiver)' ($r.ok -and $r.data.status -eq 'STORING') "(status=$($r.data.status))"
$r = Api GET "/api/orders/$O1" $users.cust1.token $null
$PIN1 = $r.data.pinCode
Check 'pickup PIN issued' (-not [string]::IsNullOrEmpty($PIN1)) "(pin=$PIN1)"

$r = Api POST '/api/iot/unlock' $users.cust2.token @{ lockerId = $LID; boxId = $O1BOX; pinCode = $PIN1 }
Check 'IoT unlock accepted (MQTT -> simulator)' $r.ok "(code=$($r.code))"
Start-Sleep -Seconds 4   # let the simulated door report back

$r = Api PUT "/api/orders/$O1/complete" $users.cust2.token $null
Check 'receiver completes pickup' ($r.ok -and $r.data.status -eq 'COMPLETED') "(status=$($r.data.status))"

$r = Api GET '/api/admin/dashboard/overview' $ADM $null
Check 'admin dashboard overview' ($r.ok -and $r.data.totalOrders -ge 1) "(totalOrders=$($r.data.totalOrders))"
$r = Api GET '/api/admin/orders/statistics' $ADM $null
Check 'admin order statistics' $r.ok ''
$r = Api GET '/api/admin/orders/revenue' $ADM $null
Check 'admin revenue' $r.ok "($(($r.data | ConvertTo-Json -Compress -Depth 3)))"
$r = Api GET '/api/admin/lockers/stats' $ADM $null
Check 'admin locker stats (web parity endpoint)' $r.ok "(n=$($r.data.Count))"
$r = Api GET '/api/admin/iot/device-status' $ADM $null
Check 'admin IoT device status (web parity endpoint)' $r.ok "(n=$($r.data.Count))"

Write-Host "`n--- 2.1b RENTAL: create -> pay -> confirm -> extend -> end ---"
$r = Api POST '/api/orders/rental' $users.cust1.token @{ lockerId = $LID; cellType = 'STANDARD'; hours = 2; note = 'flow-2.1b' }
Check 'RENTAL order created' $r.ok "(id=$($r.data.id) box=$($r.data.sendBoxId))"
$O2 = $r.data.id; $O2BOX = $r.data.sendBoxId
$r = Api POST '/api/payments/checkout' $users.cust1.token @{ orderId = $O2; method = 'WALLET' }
Check 'rental checkout WALLET' $r.ok "(status=$($r.data.status))"
$r = Api PUT "/api/orders/$O2/confirm" $users.cust1.token $null
Check 'rental confirm -> STORING' ($r.ok -and $r.data.status -eq 'STORING') ''
$r = Api POST "/api/orders/$O2/extend-rental" $users.cust1.token @{ hours = 1 }
Check 'rental extended +1h' $r.ok "($($r.code))"
$r = Api GET "/api/orders/$O2" $users.cust1.token $null
$PIN2 = $r.data.pinCode
$r = Api POST '/api/iot/unlock' $users.cust1.token @{ lockerId = $LID; boxId = $O2BOX; pinCode = $PIN2 }
Check 'rental IoT unlock accepted' $r.ok "(code=$($r.code))"
$r = Api POST "/api/orders/$O2/pickup-storage" $users.cust1.token $null
Check 'rental ended (pickup-storage)' $r.ok "(status=$($r.data.status))"

# ============================================================================
Write-Host "`n=== 2.2 FAULT REPORT: user -> TECHNICIAN -> admin ==="
$faultBoxId = $stdBox.id
$r = Api POST "/api/boxes/$faultBoxId/fault" $users.cust1.token @{ reason = 'Cua bi ket - test flow 2.2' }
Check 'customer reports box fault' $r.ok "(box=$faultBoxId $($r.code))"

$r = Api GET '/api/maintenance/faults' $users.tech.token $null
Check 'TECHNICIAN sees fault cells' ($r.ok -and $r.data.Count -ge 1) "(n=$($r.data.Count))"
$r = Api GET '/api/maintenance/reports' $users.tech.token $null
Check 'TECHNICIAN sees report queue' $r.ok "(n=$($r.data.Count))"
$report = $r.data | Where-Object { $_.boxId -eq $faultBoxId } | Sort-Object id -Descending | Select-Object -First 1
if (-not $report) { $report = $r.data | Select-Object -First 1 }
$RID = $report.id

$r = Api PUT "/api/maintenance/reports/$RID/claim" $users.tech.token $null
Check 'TECHNICIAN claims report' $r.ok "(id=$RID status=$($r.data.status))"
$r = Api POST "/api/maintenance/reports/$RID/logs" $users.tech.token @{ note = 'Da kiem tra, thay khoa moi' }
Check 'TECHNICIAN adds work log' $r.ok ''
$r = Api PUT "/api/maintenance/reports/$RID/resolve" $users.tech.token $null
Check 'TECHNICIAN resolves report' ($r.ok -and $r.data.status -eq 'RESOLVED') "(status=$($r.data.status))"

$r = Api GET '/api/lockers/my-reports' $users.cust1.token $null
Check 'customer sees resolved report' ($r.ok -and ($r.data | Where-Object { $_.id -eq $RID }).status -eq 'RESOLVED') ''
$r = Api POST "/api/lockers/reports/$RID/rate" $users.cust1.token @{ rating = 5; comment = 'Sua nhanh' }
Check 'customer rates technician' $r.ok "($($r.code))"
$r = Api GET '/api/maintenance/my-rating-average' $users.tech.token $null
Check 'TECHNICIAN sees rating average' ($r.ok -and $r.data.count -ge 1) "(avg=$($r.data.average) n=$($r.data.count))"
$r = Api GET '/api/maintenance/reports' $ADM $null
Check 'ADMIN views all reports' $r.ok "(n=$($r.data.Count))"

Write-Host "`n--- RBAC checks (role split; MANAGER/STAFF removed) ---"
$r = Api GET '/api/maintenance/reports' $users.cust1.token $null
Check 'CUSTOMER blocked from maintenance API (403)' ($r.status -eq 403) "(status=$($r.status))"
$r = Api GET '/api/maintenance/drones' $users.tech.token $null
Check 'TECHNICIAN blocked from drone fleet (403)' ($r.status -eq 403) "(status=$($r.status))"
$r = Api GET '/api/maintenance/drone-deliveries' $users.tech.token $null
Check 'TECHNICIAN blocked from drone-delivery queue (403)' ($r.status -eq 403) "(status=$($r.status))"
$r = Api GET '/api/maintenance/drones' $users.maint.token $null
Check 'MAINTENANCE allowed on drone fleet' $r.ok "(n=$($r.data.Count))"
$r = Api POST "/api/maintenance/lockers/$LID/landing-pad" $users.tech.token @{ status = 'MAINTENANCE'; reason = 'test' }
Check 'TECHNICIAN updates landing pad' $r.ok "($($r.code))"
$r = Api POST "/api/maintenance/lockers/$LID/landing-pad" $users.tech.token @{ status = 'OK' }
Check 'TECHNICIAN restores landing pad OK' $r.ok ''
$r = Api GET '/api/manage/orders' $ADM $null
Check 'retired /api/manage/** is gone (404)' ($r.status -eq 404) "(status=$($r.status))"
$r = Api GET '/api/staff/orders' $ADM $null
Check 'retired /api/staff/** is gone (404)' ($r.status -eq 404) "(status=$($r.status))"

# ============================================================================
Write-Host "`n=== 2.3 DRONE DELIVERY: user request -> MAINTENANCE dispatches -> delivered -> admin views ==="
# Direct SEND booking on a DRONE cell stays forbidden — and now returns the
# clean business error instead of a wrapped 500.
$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; boxId = $droneBox.id; receiverPhone = $users.cust2.phone; note = 'direct book' }
Check 'direct DRONE-cell booking rejected with clean 400' (($r.status -eq 400) -and ($r.code -eq 'DRONE_CELL_RESTRICTED')) "(status=$($r.status) code=$($r.code))"

# Real path: the customer files a drone-delivery request (mobile DroneBookingSheet).
$r = Api POST '/api/drone-deliveries' $users.cust1.token @{ lockerId = $LID; boxId = $droneBox.id; description = 'Tai lieu gap - flow 2.3' }
Check 'customer creates drone-delivery request' ($r.ok -and $r.data.status -eq 'PENDING') "(id=$($r.data.id) box#$($r.data.boxNumber))"
$DD = $r.data.id

$r = Api GET '/api/drone-deliveries/my' $users.cust1.token $null
Check 'customer sees own request' ($r.ok -and (@($r.data | Where-Object { $_.id -eq $DD })).Count -ge 1) "(n=$(@($r.data).Count))"

$r = Api GET '/api/maintenance/drone-deliveries?status=PENDING' $users.maint.token $null
Check 'MAINTENANCE sees PENDING queue' ($r.ok -and (@($r.data | Where-Object { $_.id -eq $DD })).Count -ge 1) "(n=$(@($r.data).Count))"

$r = Api GET '/api/maintenance/drones' $users.maint.token $null
$drone = $r.data | Where-Object { $_.status -eq 'IDLE' } | Select-Object -First 1
Check 'an IDLE drone is available' ($null -ne $drone) "(drone=$($drone.code))"
$DID = $drone.id

$r = Api POST "/api/maintenance/drone-deliveries/$DD/dispatch" $users.maint.token @{ droneUnitId = $DID }
Check 'MAINTENANCE dispatches request with drone' ($r.ok -and $r.data.status -eq 'DISPATCHED' -and $r.data.droneCode -eq $drone.code) "(drone=$($r.data.droneCode))"
$r = Api GET '/api/maintenance/drones' $users.maint.token $null
$after = $r.data | Where-Object { $_.id -eq $DID }
Check 'dispatched drone switched to IN_FLIGHT' ($after.status -eq 'IN_FLIGHT') "(status=$($after.status))"

$r = Api POST "/api/maintenance/drone-deliveries/$DD/complete" $users.maint.token $null
Check 'MAINTENANCE completes delivery' ($r.ok -and $r.data.status -eq 'DELIVERED') ''
$r = Api GET '/api/maintenance/drones' $users.maint.token $null
$after = $r.data | Where-Object { $_.id -eq $DID }
Check 'drone returned to IDLE after delivery' ($after.status -eq 'IDLE') "(status=$($after.status))"
$r = Api GET '/api/drone-deliveries/my' $users.cust1.token $null
$mine = @($r.data) | Where-Object { $_.id -eq $DD }
Check 'customer sees request DELIVERED' ($mine.status -eq 'DELIVERED') ''

# Customer cancel path on a fresh request.
$r = Api POST '/api/drone-deliveries' $users.cust1.token @{ lockerId = $LID; description = 'se huy' }
$DD2 = $r.data.id
$r = Api PUT "/api/drone-deliveries/$DD2/cancel" $users.cust1.token $null
Check 'customer cancels PENDING request' ($r.ok -and $r.data.status -eq 'CANCELED') ''

$r = Api GET '/api/admin/drones' $ADM $null
Check 'ADMIN views drone fleet' ($r.ok -and $r.data.Count -ge 3) "(n=$($r.data.Count))"

# ============================================================================
Write-Host "`n=== 2.4 SUB-FLOWS ==="
$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; receiverPhone = $users.cust2.phone; note = 'to-cancel' }
$O4 = $r.data.id
$r = Api PUT "/api/orders/$O4/cancel" $users.cust1.token $null
Check 'cancel unpaid order' ($r.ok -and $r.data.status -eq 'CANCELED') "(status=$($r.data.status))"

$r = Api POST "/api/orders/$O1/reorder" $users.cust1.token $null
Check 'reorder from completed order' $r.ok "(newId=$($r.data.id))"
if ($r.ok) { $null = Api PUT "/api/orders/$($r.data.id)/cancel" $users.cust1.token $null }

$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; receiverPhone = $users.cust1.phone; note = 'to-delegate' }
$O5 = $r.data.id; $O5BOX = $r.data.sendBoxId
$null = Api POST '/api/payments/checkout' $users.cust1.token @{ orderId = $O5; method = 'WALLET' }
$null = Api PUT "/api/orders/$O5/confirm" $users.cust1.token $null
$r = Api POST "/api/orders/$O5/delegate" $users.cust1.token @{ phone = $users.cust2.phone; name = 'Cust2 nhan ho'; note = 'ban ron' }
Check 'delegate pickup to another phone' $r.ok "($($r.code))"

# Sai PIN: backend soft-fail (HTTP 200, accepted=false, không gửi lệnh MQTT).
$r = Api POST '/api/iot/unlock' $users.cust2.token @{ lockerId = $LID; boxId = $O5BOX; pinCode = '000000' }
Check 'wrong PIN unlock rejected (accepted=false)' ($r.ok -and $r.data.accepted -eq $false) "(msg=$($r.data.message))"
$null = Api PUT "/api/orders/$O5/complete" $users.cust2.token $null

$r = Api GET "/api/lockers/$LID/layout" $null $null
$freeBox = $r.data.cells | Where-Object { $_.cellType -eq 'STANDARD' -and $_.status -eq 'AVAILABLE' } | Select-Object -First 1
$B = $freeBox.id
$r = Api POST "/api/maintenance/boxes/$B/out-of-service" $users.tech.token @{ reason = 'bao tri dinh ky' }
Check 'tech puts box OUT_OF_SERVICE' $r.ok "($($r.code))"
$r = Api POST "/api/maintenance/boxes/$B/return-to-service" $users.tech.token $null
Check 'tech returns box to service' $r.ok ''
$r = Api POST "/api/maintenance/boxes/$B/cleaning" $users.tech.token $null
Check 'tech marks box CLEANING' $r.ok ''
$r = Api POST "/api/maintenance/boxes/$B/return-to-service" $users.tech.token $null
Check 'tech finishes cleaning' $r.ok ''
$r = Api POST "/api/maintenance/boxes/$B/force-open" $users.tech.token $null
Check 'tech force-opens box (MASTER audit)' $r.ok "($($r.code))"

$r = Api GET '/api/maintenance/box-anomalies' $users.tech.token $null
Check 'tech reads box anomalies' $r.ok "(n=$($r.data.Count))"

$r = Api POST '/api/admin/scheduler/auto-cancel' $ADM $null
Check 'admin scheduler auto-cancel' $r.ok "($(($r.data | ConvertTo-Json -Compress -Depth 2)))"
$r = Api POST '/api/admin/scheduler/release-boxes' $ADM $null
Check 'admin scheduler release-boxes' $r.ok ''

$r = Register "extra.$suffix@test.local" "090$suffix"+"6" 'extra' $PASS
Check 'new register defaults to CUSTOMER only' ($r.data.roles.Count -eq 1 -and $r.data.roles -contains 'CUSTOMER') "(roles=$($r.data.roles -join ','))"

# ============================================================================
Write-Host "`n=== 2.5 VOUCHER: admin CRUD theo kiosk -> user luu ma -> ap don -> gioi han luot ==="
$VC = "VC$suffix"
$promoBody = @{ code = $VC; name = 'Voucher test kiosk'; discountType = 'PERCENTAGE'; discountValue = 10; maxDiscountAmount = 5000; stackable = $false; status = 'ACTIVE'; lockerId = $LID; totalUsageLimit = 10; perUserLimit = 1 }
$r = Api POST '/api/admin/promotions' $ADM $promoBody
Check 'admin creates locker-scoped promotion' ($r.ok -and $r.data.lockerId -eq $LID) "(id=$($r.data.id) code=$VC)"
$PROMO = $r.data.id

$promoBody.name = 'Voucher test kiosk (sua)'
$r = Api PUT "/api/admin/promotions/$PROMO" $ADM $promoBody
Check 'admin updates promotion' ($r.ok -and $r.data.name -like '*sua*') ''

$r = Api GET "/api/promotions/validate/${VC}?lockerId=$LID" $users.cust1.token $null
Check 'validate dung tu -> valid' ($r.ok -and $r.data.valid -eq $true) ''
$other = (Api GET '/api/lockers' $null $null).data | Where-Object { $_.id -ne $LID } | Select-Object -First 1
$r = Api GET "/api/promotions/validate/${VC}?lockerId=$($other.id)" $users.cust1.token $null
Check 'validate sai tu -> invalid + reason' ($r.ok -and $r.data.valid -eq $false -and $r.data.reason) "($($r.data.reason))"

$r = Api GET '/api/promotions/vouchers/my' $null $null
Check 'vouchers/my khong JWT -> 401' ($r.status -eq 401) "(status=$($r.status))"
$r = Api POST "/api/promotions/$PROMO/claim" $users.cust1.token $null
Check 'user luu ma vao vi (SAVED)' ($r.ok -and $r.data.status -eq 'SAVED') "($($r.code))"
$r = Api POST "/api/promotions/$PROMO/claim" $users.cust1.token $null
Check 'luu lai lan 2 van OK (idempotent)' $r.ok ''

$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; receiverPhone = $users.cust2.phone; note = 'voucher-test'; promotionCode = $VC }
Check 'don ap voucher: giam 10% (1500d), tong 13500d' ($r.ok -and $r.data.discount -eq 1500 -and $r.data.totalPrice -eq 13500) "(discount=$($r.data.discount) total=$($r.data.totalPrice))"
$OV = $r.data.id
$r = Api GET '/api/promotions/vouchers/my' $users.cust1.token $null
$mine = @($r.data | Where-Object { $_.promotionId -eq $PROMO }) | Select-Object -First 1
Check 'voucher chuyen USED sau khi ap' ($mine.status -eq 'USED') "(status=$($mine.status))"

$r = Api POST '/api/orders/send' $users.cust1.token @{ lockerId = $LID; receiverPhone = $users.cust2.phone; note = 'voucher-test-2'; promotionCode = $VC }
Check 'dung lai ma -> 400 PROMOTION_INVALID (per-user limit)' ($r.status -eq 400 -and $r.code -eq 'PROMOTION_INVALID') "(msg=$($r.msg))"

$r = Api PUT "/api/orders/$OV/cancel" $users.cust1.token $null
Check 'huy don co voucher' $r.ok ''
$r = Api GET '/api/promotions/vouchers/my' $users.cust1.token $null
$mine = @($r.data | Where-Object { $_.promotionId -eq $PROMO }) | Select-Object -First 1
Check 'huy don -> hoan luot, voucher ve SAVED' ($mine.status -eq 'SAVED') "(status=$($mine.status))"

$r = Api DELETE "/api/admin/promotions/$PROMO" $ADM $null
Check 'delete blocked once promotion has usage history (400)' ($r.status -eq 400 -and $r.code -eq 'PROMOTION_HAS_HISTORY') "(status=$($r.status) code=$($r.code))"
$promoBody.status = 'INACTIVE'
$r = Api PUT "/api/admin/promotions/$PROMO" $ADM $promoBody
Check 'admin deactivates instead of deleting' ($r.ok -and $r.data.status -eq 'INACTIVE') ''

$r = Api POST '/api/admin/promotions' $ADM @{ code = "VCEMPTY$suffix"; name = 'Chua ai dung'; discountType = 'FIXED_AMOUNT'; discountValue = 1000; status = 'ACTIVE' }
$EMPTY_PROMO = $r.data.id
$r = Api DELETE "/api/admin/promotions/$EMPTY_PROMO" $ADM $null
Check 'admin deletes promotion with no usage history' $r.ok ''

# ============================================================================
Write-Host "`n=== SUMMARY ==="
$pass = ($script:results | Where-Object { $_ -like '`[PASS`]*' }).Count
$fail = ($script:results | Where-Object { $_ -like '`[FAIL`]*' }).Count
Write-Host "PASS=$pass FAIL=$fail TOTAL=$($script:results.Count)"
if ($fail -gt 0) {
  Write-Host "`nFailed checks:"
  $script:results | Where-Object { $_ -like '`[FAIL`]*' } | ForEach-Object { Write-Host "  $_" }
  exit 1
}
