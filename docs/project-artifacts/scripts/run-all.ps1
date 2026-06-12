# ============================================================
# run-all.ps1 — Chạy toàn bộ hệ thống Smart Laundry Locker
# Backend: Docker Compose (Postgres, RabbitMQ + 14 services)
# Frontend: Vite dev server (background process)
# Cách dùng:
#   .\run-all.ps1              # build (nếu cần) + chạy tất cả
#   .\run-all.ps1 -SkipBuild   # bỏ qua mvn package (đã build sẵn)
#   .\run-all.ps1 -BackendOnly # chỉ chạy backend
# ============================================================
param(
    [switch]$SkipBuild,
    [switch]$BackendOnly
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$ms = Join-Path $root "laundry-locker-microservices"
$fe = Join-Path $root "laundry-locker-frontend\fe"

Write-Host "=== [1/4] Kiem tra Docker ===" -ForegroundColor Cyan
docker info --format '{{.ServerVersion}}' | Out-Null
if (-not $?) { Write-Error "Docker chua chay. Mo Docker Desktop truoc."; exit 1 }

Write-Host "=== [2/4] Build backend (mvn package) ===" -ForegroundColor Cyan
if (-not $SkipBuild) {
    Push-Location $ms
    mvn clean package -DskipTests -T 1C
    if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Error "Maven build that bai."; exit 1 }
    Pop-Location
} else {
    Write-Host "Bo qua build (da co JAR trong target/)."
}

Write-Host "=== [3/4] Chay backend qua Docker Compose ===" -ForegroundColor Cyan
Push-Location $ms
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Error "docker compose up that bai."; exit 1 }
Pop-Location

Write-Host "Cho cac service dang ky Eureka (60s)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 10
    Write-Host "API Gateway: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "API Gateway chua san sang — kiem tra: docker compose -f `"$ms\docker-compose.yml`" logs api-gateway" -ForegroundColor Yellow
}

if ($BackendOnly) {
    Write-Host "Hoan tat (backend-only)." -ForegroundColor Green
    exit 0
}

Write-Host "=== [4/4] Chay frontend (Vite) ===" -ForegroundColor Cyan
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "npm khong co tren PATH. Cai Node.js LTS roi chay lai, hoac mo terminal moi." -ForegroundColor Red
} else {
    Push-Location $fe
    if (-not (Test-Path "node_modules")) { npm install }
    # Chạy Vite dev server nền, log ra fe-dev.log
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev > `"$root\fe-dev.log`" 2>&1" -WorkingDirectory $fe -WindowStyle Hidden
    Pop-Location
    Write-Host "Frontend dang chay nen — log: $root\fe-dev.log" -ForegroundColor Green
}

Write-Host ""
Write-Host "================= HE THONG DA KHOI DONG =================" -ForegroundColor Green
Write-Host " API Gateway : http://localhost:8080"
Write-Host " Eureka      : http://localhost:8761"
Write-Host " RabbitMQ UI : http://localhost:15672 (guest/guest)"
Write-Host " PostgreSQL  : localhost:15432 (postgres/postgres)"
Write-Host " Frontend    : http://localhost:3000"
Write-Host " Dung lai    : .\stop-all.ps1"
Write-Host "=========================================================="
