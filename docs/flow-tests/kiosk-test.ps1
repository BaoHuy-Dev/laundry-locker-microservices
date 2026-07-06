# Kiosk simulation e2e: mobile order -> kiosk opens box by PIN / QR / code (no JWT).
$ErrorActionPreference = 'Continue'
$GW = 'http://localhost:18080'
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
      try { $body = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() | ConvertFrom-Json } catch {}
    }
    return @{ ok = $false; status = $status; code = $(if($body){$body.code}); msg = $(if($body){$body.message}else{$_.Exception.Message}); data = $null }
  }
}
function Check { param([string]$Name, [bool]$Cond, [string]$Extra = '')
  $tag = if ($Cond) { 'PASS' } else { 'FAIL' }
  Write-Host "[$tag] $Name $Extra"
  [void]$script:results.Add($tag)
}

# warm-up: dung unlock-with-code voi ma rac â€” KHONG ghi failed-attempt vao box
# nao (verify-pin voi PIN rac se cong don lockout cho box!).
$warm = $false
for ($i = 0; $i -lt 24 -and -not $warm; $i++) {
  $r = Api POST '/api/iot/unlock-with-code' $null @{ lockerId = 1; code = 'WARMUP-NOPE' }
  if ($r.ok -and $r.data.accepted -ne $true) { $warm = $true } else { Start-Sleep -Seconds 5 }
}
Check 'kiosk unlock endpoint reachable without JWT' $warm ''

$sfx = Get-Date -Format 'mmss'; $PW = 'Passw0rd!123'
# AuthResponse KHONG tra phoneNumber -> giu phone trong bien cuc bo.
$P1 = "0931$sfx"+"1"; $P2 = "0931$sfx"+"2"
$a = Api POST '/api/auth/login' $null @{ identifier = 'admin@lockerly.local'; password = 'Admin@123456' }
$ADM = $a.data.accessToken
$c1 = Api POST '/api/auth/register' $null @{ email = "k1.$sfx@t.local"; phoneNumber = $P1; firstName='k1'; lastName='T'; password = $PW }
$c2 = Api POST '/api/auth/register' $null @{ email = "k2.$sfx@t.local"; phoneNumber = $P2; firstName='k2'; lastName='T'; password = $PW }
Check 'seed users registered' ($c1.ok -and $c2.ok) "(u1=$($c1.data.userId) u2=$($c2.data.userId))"
Check 'admin login' ($a.ok -and $ADM) "($($a.code) $($a.msg))"
$TOK1 = $c1.data.accessToken
$w = Api POST "/api/admin/wallet/$($c1.data.userId)/adjust" $ADM @{ amount = 300000 }
Check 'wallet top-up' $w.ok "($($w.status) $($w.code) $($w.msg))"
$LID = 1

function NewPaidStoringOrder {
  $r = Api POST '/api/orders/send' $TOK1 @{ lockerId = $LID; receiverPhone = $P2; receiverName='K2'; note='kiosk-test' }
  $id = $r.data.id
  $null = Api POST '/api/payments/checkout' $TOK1 @{ orderId = $id; method = 'WALLET' }
  $null = Api PUT "/api/orders/$id/confirm" $TOK1 $null
  $d = Api GET "/api/orders/$id" $TOK1 $null
  return @{ id = $id; box = $d.data.sendBoxId; pin = $d.data.pinCode; qr = $d.data.qrToken; status = $d.data.status }
}

Write-Host "`n== A) Kiosk: nhap so o + PIN (PinScreen flow, no JWT) =="
$o1 = NewPaidStoringOrder
Check 'order ready (STORING, has PIN)' ($o1.status -eq 'STORING' -and $o1.pin) "(id=$($o1.id) box=$($o1.box) pin=$($o1.pin))"
$lay = Api GET "/api/lockers/$LID/layout" $null $null
$cell = $lay.data.cells | Where-Object { $_.id -eq $o1.box } | Select-Object -First 1
Check 'kiosk layout maps box number -> boxId' ($null -ne $cell) "(o so #$($cell.boxNumber))"
$r = Api POST '/api/iot/verify-pin' $null @{ boxId = $o1.box; pinCode = $o1.pin }
Check 'verify-pin valid (no JWT)' ($r.ok -and $r.data.valid -eq $true) "(order=$($r.data.orderId))"
$r = Api POST '/api/iot/unlock' $null @{ lockerId = $LID; boxId = $o1.box; pinCode = $o1.pin; actionType = 'PICKUP' }
Check 'unlock accepted -> simulator opens door' ($r.ok -and $r.data.accepted -eq $true) "(msg=$($r.data.message))"
$r = Api POST '/api/iot/verify-pin' $null @{ boxId = $o1.box; pinCode = '111111' }
Check 'wrong PIN denied' ($r.ok -and $r.data.valid -eq $false) "(msg=$($r.data.message))"
$null = Api PUT "/api/orders/$($o1.id)/complete" $TOK1 $null

Write-Host "`n== B) Kiosk: unlock-with-code bang PIN (StaffScreen flow, no JWT) =="
$o2 = NewPaidStoringOrder
$r = Api POST '/api/iot/unlock-with-code' $null @{ lockerId = $LID; code = $o2.pin }
Check 'unlock-with-code by PIN' ($r.ok -and $r.data.accepted -eq $true -and $r.data.boxId -eq $o2.box) "(box=$($r.data.boxId) order=$($r.data.orderId) msg=$($r.data.message) http=$($r.status))"

Write-Host "`n== C) Kiosk: unlock-with-code bang QR token =="
Check 'order has QR token' (-not [string]::IsNullOrEmpty($o2.qr)) ''
$r = Api POST '/api/iot/unlock-with-code' $null @{ lockerId = $LID; code = $o2.qr }
Check 'unlock-with-code by QR token' ($r.ok -and $r.data.accepted -eq $true) "(box=$($r.data.boxId))"
$r = Api POST '/api/iot/unlock-with-code' $null @{ lockerId = $LID; code = 'LLQR.fake.token' }
Check 'garbage code rejected' ($r.ok -and $r.data.accepted -ne $true) "(msg=$($r.data.message))"
$null = Api PUT "/api/orders/$($o2.id)/complete" $TOK1 $null

Write-Host "`n== D) Cac endpoint IoT khac van can JWT =="
$r = Api POST '/api/iot/pickup' $null @{ orderId = 1 }
Check '/api/iot/pickup without JWT -> 401' ($r.status -eq 401) "(status=$($r.status))"

Write-Host "`n=== SUMMARY ==="
$pass = ($script:results | Where-Object { $_ -eq 'PASS' }).Count
$fail = ($script:results | Where-Object { $_ -eq 'FAIL' }).Count
Write-Host "PASS=$pass FAIL=$fail"
