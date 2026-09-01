# ============================================
#  cacch-integration-platform Deployment
#  PowerShell 版本 (推荐在 PowerShell 中使用)
#  目标: 测试环境 10.80.68.10 —— 必须使用 test 分支
#
#  运行方式（系统禁止运行脚本时）:
#    powershell -ExecutionPolicy Bypass -File deploy-test.ps1
# ============================================

$ProjectDir   = "D:\Software\IdeaProject\cacch-integration-platform"
$JavaHome     = "D:\Software\Java\jdk21.0.10_7"
$MavenHome    = "D:\Software\apache-maven-3.9.9"
$Python       = "C:\Users\Administrator\.workbuddy\binaries\python\envs\deploy\Scripts\python.exe"  # 含 paramiko 的 venv
$DeployBranch = "test"
$TargetHost   = "10.80.68.10"

# 设置环境变量
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  cacch-integration-platform Deployment" -ForegroundColor Cyan
Write-Host "  Target: TEST (10.80.68.10) @ test 分支" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ========== [1/4] Git 分支校验与拉取 ==========
. "$PSScriptRoot\deploy-git.ps1"

$git = Invoke-DeployGitStage -ProjectDir $ProjectDir -Branch $DeployBranch `
    -EnvName "测试环境" -TargetHost $TargetHost

if ($null -eq $git) {
    Write-Host "[X] Git 校验未通过，部署已中止。" -ForegroundColor Red
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

# ========== [3/4] 部署到测试服务器 ==========
Write-Host "[3/4] Deploying to test server ($TargetHost) ..." -ForegroundColor Yellow
Write-Host ""

& $Python "$ProjectDir\deploy.py" test

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[X] Deploy failed! Please check errors above." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ========== [4/4] 完成 ==========
Write-DeployHistory -ProjectDir $ProjectDir -EnvName "测试环境" -TargetHost $TargetHost -Info $git

Write-Host ""
Write-Host "[4/4] All done!" -ForegroundColor Green
Write-Host "  Branch : $($git.Branch) @ $($git.Commit)" -ForegroundColor Green
Write-Host "  Address: http://$TargetHost:8081" -ForegroundColor Green
Write-Host ""

Read-Host "Press Enter to exit"
