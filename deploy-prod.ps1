# ============================================
#  cacch-integration-platform Deployment
#  PowerShell 版本 — 部署到正式环境
#  目标: 正式环境 10.80.68.11 —— 必须使用 main 分支
#
#  运行方式（系统禁止运行脚本时）:
#    powershell -ExecutionPolicy Bypass -File deploy-prod.ps1
# ============================================

$ProjectDir   = "D:\Software\IdeaProject\cacch-integration-platform"
$JavaHome     = "D:\Software\Java\jdk21.0.10_7"
$MavenHome    = "D:\Software\apache-maven-3.9.9"
$Python       = "C:\Users\Administrator\.workbuddy\binaries\python\envs\deploy\Scripts\python.exe"  # 含 paramiko 的 venv
$DeployBranch = "main"
$TargetHost   = "10.80.68.11"

# 设置环境变量
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  cacch-integration-platform Deployment" -ForegroundColor Cyan
Write-Host "  Target: PRODUCTION (10.80.68.11) @ main 分支" -ForegroundColor Red
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ========== [1/4] Git 分支校验 + 生产二次确认 ==========
. "$PSScriptRoot\deploy-git.ps1"

$git = Invoke-DeployGitStage -ProjectDir $ProjectDir -Branch $DeployBranch `
    -EnvName "正式环境" -TargetHost $TargetHost -RequireConfirm

if ($null -eq $git) {
    Write-Host "[X] Git 校验未通过或已取消，部署已中止。" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========== [2/4] Maven 打包 ==========
Write-Host "[2/4] Maven packaging (mvn clean package -DskipTests) ..." -ForegroundColor Yellow
Write-Host ""

Set-Location $ProjectDir

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

# ========== [3/4] 部署到正式服务器 ==========
Write-Host "[3/4] Deploying to production server ($TargetHost) ..." -ForegroundColor Yellow
Write-Host ""

& $Python "$ProjectDir\deploy.py" prod

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[X] Deploy failed! Please check errors above." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========== [4/4] 完成 ==========
Write-DeployHistory -ProjectDir $ProjectDir -EnvName "正式环境" -TargetHost $TargetHost -Info $git

Write-Host ""
Write-Host "[4/4] Production deployment completed!" -ForegroundColor Green
Write-Host "  Branch : $($git.Branch) @ $($git.Commit)" -ForegroundColor Green
Write-Host "  Address: http://$TargetHost:8081" -ForegroundColor Green
Write-Host ""

Read-Host "Press Enter to exit"
