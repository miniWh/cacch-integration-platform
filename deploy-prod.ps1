# ============================================
#  cacch-integration-platform Deployment
#  PowerShell 版本 — 部署到生产环境
# ============================================

$ProjectDir = "D:\Software\IdeaProject\cacch-integration-platform"
$JavaHome   = "D:\Software\Java\jdk21.0.10_7"
$MavenHome  = "D:\Software\apache-maven-3.9.9"
$Python     = "C:\Users\Administrator\.workbuddy\binaries\python\versions\3.13.12\python.exe"

# 设置环境变量
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  cacch-integration-platform Deployment" -ForegroundColor Cyan
Write-Host "  Target: PRODUCTION (10.80.68.11)" -ForegroundColor Red
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ========== [1/3] Maven 打包 ==========
Write-Host "[1/3] Maven packaging (mvn clean package -DskipTests) ..." -ForegroundColor Yellow
Write-Host ""

Set-Location $ProjectDir

# 执行 Maven 打包，并捕获退出码
& "$MavenHome\bin\mvn.cmd" clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "" 
    Write-Host "[X] Maven build failed! Please check errors above." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "    [OK] Build successful" -ForegroundColor Green
Write-Host ""

# ========== [2/3] 部署到生产服务器 ==========
Write-Host "[2/3] Deploying to production server (10.80.68.11) ..." -ForegroundColor Yellow
Write-Host ""

& $Python "$ProjectDir\deploy.py" prod

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[X] Deploy failed! Please check errors above." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========== [3/3] 完成 ==========
Write-Host ""
Write-Host "[3/3] Production deployment completed!" -ForegroundColor Green
Write-Host "  Address: http://10.80.68.11:8081" -ForegroundColor Green
Write-Host ""

Read-Host "Press Enter to exit"
