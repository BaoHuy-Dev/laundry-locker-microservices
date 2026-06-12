# ============================================================
# seed-test-data.ps1 — Tạo data test cho tài khoản nqbhuy2004nt@gmail.com
# Chạy lại bao nhiêu lần cũng được (mỗi lần thêm 1 bộ đơn hàng mới).
# Yêu cầu: backend đang chạy (gateway http://localhost:8080)
# ============================================================
$ErrorActionPreference = "Stop"
$GW = "http://localhost:8080"
$EMAIL = "nqbhuy2004nt@gmail.com"
$PASSWORD = "Test@123456"

function Post($uri, $body, $headers = @{}) {
    Invoke-RestMethod -Method Post -Uri "$GW$uri" -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 5) -Headers $headers
}
function Patch($uri, $body, $headers = @{}) {
    Invoke-RestMethod -Method Patch -Uri "$GW$uri" -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 5) -Headers $headers
}

# ---- 1. Tài khoản ----
Write-Host "[1/6] Tai khoan $EMAIL ..." -ForegroundColor Cyan
try {
    $reg = Post "/api/auth/register" @{ email = $EMAIL; phoneNumber = "0905222333"; firstName = "Huy"; lastName = "Nguyen"; password = $PASSWORD; roles = @("CUSTOMER") }
    Write-Host "  Da dang ky moi (userId=$($reg.data.userId))"
} catch { Write-Host "  Tai khoan da ton tai - dang nhap" }

$login = Post "/api/auth/login" @{ identifier = $EMAIL; password = $PASSWORD }
$token = $login.data.accessToken
$userId = $login.data.userId
$auth = @{ Authorization = "Bearer $token" }
Write-Host "  Login OK: userId=$userId" -ForegroundColor Green

# ---- 2. Cửa hàng ----
Write-Host "[2/6] Cua hang ..." -ForegroundColor Cyan
$stores = (Invoke-RestMethod -Uri "$GW/api/stores").data
if ($stores.Count -ge 2) {
    $store1 = $stores[0]; $store2 = $stores[1]
    Write-Host "  Dung lai store co san: $($store1.id), $($store2.id)"
} else {
    $store1 = (Post "/api/stores" @{ name = "Lockerly Quan 1"; address = "12 Nguyen Hue, Q1, TP.HCM"; phoneNumber = "02838221234"; status = "ACTIVE" }).data
    $store2 = (Post "/api/stores" @{ name = "Lockerly Thu Duc"; address = "100 Vo Van Ngan, Thu Duc, TP.HCM"; phoneNumber = "02838995566"; status = "ACTIVE" }).data
    Write-Host "  Tao store: $($store1.id) + $($store2.id)"
}

# ---- 3. Tủ + ô tủ ----
Write-Host "[3/6] Tu locker + o tu ..." -ForegroundColor Cyan
$lockers = (Invoke-RestMethod -Uri "$GW/api/lockers").data
if ($lockers.Count -ge 1) {
    $locker = $lockers[0]
    Write-Host "  Dung lai locker co san: $($locker.id)"
} else {
    $locker = (Post "/api/lockers" @{ storeId = $store1.id; code = "LCK-Q1-01"; name = "Tu Quan 1 - So 1"; location = "Sanh chinh"; status = "ACTIVE" }).data
    Write-Host "  Tao locker: $($locker.id)"
}
try { $boxes = (Invoke-RestMethod -Uri "$GW/api/boxes?lockerId=$($locker.id)" -Headers $auth).data } catch { $boxes = @() }
if (-not $boxes -or $boxes.Count -lt 6) {
    $boxes = @()
    foreach ($i in 1..6) {
        $size = @("SMALL","MEDIUM","LARGE")[($i - 1) % 3]
        try { $boxes += (Post "/api/boxes" @{ lockerId = $locker.id; boxNumber = $i; size = $size } $auth).data } catch {}
    }
    Write-Host "  Tao $($boxes.Count) box"
} else { Write-Host "  Dung lai $($boxes.Count) box co san" }
$boxIds = @($boxes | ForEach-Object { $_.id })

# ---- 4. Đơn hàng đủ trạng thái ----
Write-Host "[4/6] Don hang (4 trang thai) ..." -ForegroundColor Cyan
function NewOrder($boxId, $note) {
    (Post "/api/orders" @{
        userId = $userId; storeId = $store1.id; lockerId = $locker.id; sendBoxId = $boxId
        type = "LAUNDRY"; serviceCategory = "LAUNDRY"; customerNote = $note
        totalPrice = 75000
        items = @(@{ serviceId = 1; quantity = 3; description = "Giat say 3kg" })
    } $auth).data
}

# Đơn 1: mới tạo (chờ bỏ đồ)
$o1 = NewOrder $boxIds[0] "Don moi tao - cho bo do vao tu"
Write-Host "  Don $($o1.id) [$($o1.orderCode)] INITIALIZED, PIN=$($o1.pinCode)"

# Đơn 2: đang giặt
$o2 = NewOrder $boxIds[1] "Don dang giat"
Patch "/api/orders/$($o2.id)/status" @{ status = "PROCESSING"; staffId = 1 } $auth | Out-Null
Write-Host "  Don $($o2.id) [$($o2.orderCode)] PROCESSING"

# Đơn 3: giặt xong, đã trả vào tủ chờ lấy
$o3 = NewOrder $boxIds[2] "Don giat xong cho lay"
Patch "/api/orders/$($o3.id)/status" @{ status = "RETURNED"; staffId = 1; receiveBoxId = $boxIds[3] } $auth | Out-Null
Write-Host "  Don $($o3.id) [$($o3.orderCode)] RETURNED (cho khach lay)"

# Đơn 4: hoàn tất + thanh toán
$o4 = NewOrder $boxIds[4] "Don da hoan tat"
Patch "/api/orders/$($o4.id)/status" @{ status = "COMPLETED"; staffId = 1 } $auth | Out-Null
Write-Host "  Don $($o4.id) [$($o4.orderCode)] COMPLETED"

# ---- 5. Thanh toán ----
Write-Host "[5/6] Thanh toan ..." -ForegroundColor Cyan
$pay = (Post "/api/payments" @{ orderId = $o4.id; userId = $userId; amount = 75000; method = "CASH"; referenceId = "SEED-$(Get-Random)" } $auth).data
Patch "/api/payments/$($pay.id)/status" @{ status = "COMPLETED" } $auth | Out-Null
Write-Host "  Payment $($pay.id) COMPLETED cho don $($o4.id)"

# ---- 6. Kiểm tra ----
Write-Host "[6/6] Kiem tra du lieu cua user ..." -ForegroundColor Cyan
$myOrders = (Invoke-RestMethod -Uri "$GW/api/orders/my-orders" -Headers $auth).data
$notis = (Invoke-RestMethod -Uri "$GW/api/notifications/user/$userId" -Headers $auth -ErrorAction SilentlyContinue).data
Write-Host ""
Write-Host "================== SEED XONG ==================" -ForegroundColor Green
Write-Host " Email     : $EMAIL"
Write-Host " Password  : $PASSWORD"
Write-Host " UserId    : $userId"
Write-Host " Don hang  : $($myOrders.Count) don (moi nhat: PIN $($o1.pinCode) de mo tu)"
Write-Host " Thong bao : $($notis.Count)"
Write-Host " Data nam trong Postgres volume - ton tai qua cac lan restart."
Write-Host " (Chi mat khi chay stop-all.ps1 -Purge)"
Write-Host "==============================================="
