# ============================================================
# stop-all.ps1 — Dừng toàn bộ hệ thống Smart Laundry Locker
# Cách dùng:
#   .\stop-all.ps1        # dừng container + frontend (giữ data DB)
#   .\stop-all.ps1 -Purge # dừng và XÓA volume Postgres (mất data!)
# ============================================================
param(
    [switch]$Purge
)

$root = $PSScriptRoot
$ms = Join-Path $root "laundry-locker-microservices"

Write-Host "=== Dung backend (Docker Compose) ===" -ForegroundColor Cyan
Push-Location $ms
if ($Purge) {
    docker compose down -v
} else {
    docker compose down
}
Pop-Location

Write-Host "=== Dung frontend (Vite/node tren port 3000) ===" -ForegroundColor Cyan
$conn = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $conn | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
        Write-Host "Dung process PID $_ (port 3000)"
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Host "Khong co process nao dang nghe port 3000."
}

Write-Host "Hoan tat. Da dung toan bo he thong." -ForegroundColor Green
