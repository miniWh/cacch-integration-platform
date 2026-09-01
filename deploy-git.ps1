# ============================================
#  部署 Git 前置校验（公共模块）
#  被 deploy-test.ps1 / deploy-prod.ps1 以 dot-source 方式引用
#
#  策略：只校验与快进拉取，不自动切换分支
#    - 分支不匹配  → 立即中止，提示人工切换
#    - 本地落后    → git pull --ff-only 快进
#    - 本地领先/分叉 → 测试环境警告继续，正式环境中止
#    - 工作区脏    → 按约定忽略，仅提示
# ============================================

function Invoke-DeployGitStage {
    param(
        [Parameter(Mandatory = $true)] [string] $ProjectDir,
        [Parameter(Mandatory = $true)] [string] $Branch,
        [Parameter(Mandatory = $true)] [string] $EnvName,
        [Parameter(Mandatory = $true)] [string] $TargetHost,
        [switch] $RequireConfirm
    )

    Write-Host "[Git] 校验分支与代码状态 ($EnvName -> $Branch)" -ForegroundColor Yellow
    Write-Host ""

    # 保证中文 commit 信息不乱码
    try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
    $env:LC_ALL = "C.UTF-8"

    Push-Location $ProjectDir
    try {
        # ---------- 1. 分支校验 ----------
        $current = (& git rev-parse --abbrev-ref HEAD 2>$null)
        if ($LASTEXITCODE -ne 0 -or -not $current) {
            Write-Host "    [X] 当前目录不是 Git 仓库: $ProjectDir" -ForegroundColor Red
            return $null
        }
        $current = $current.Trim()

        if ($current -ne $Branch) {
            Write-Host "    [X] 分支不匹配：当前 [$current]，$EnvName 必须使用 [$Branch]" -ForegroundColor Red
            Write-Host ""
            Write-Host "    请手动切换分支后重新运行：" -ForegroundColor Yellow
            Write-Host "        git checkout $Branch" -ForegroundColor Cyan
            Write-Host "        git pull --ff-only origin $Branch" -ForegroundColor Cyan
            Write-Host ""
            return $null
        }
        Write-Host "    [OK] 当前分支: $current" -ForegroundColor Green

        # ---------- 2. 抓取远端（失败降级为离线模式，不阻断） ----------
        & git fetch --prune origin $Branch 2>$null | Out-Null
        $fetchOk = ($LASTEXITCODE -eq 0)

        $local = (& git rev-parse HEAD).Trim()
        $remote = (& git rev-parse "origin/$Branch" 2>$null)
        if ($LASTEXITCODE -eq 0 -and $remote) { $remote = $remote.Trim() } else { $remote = $null }

        if (-not $fetchOk) {
            Write-Host "    [!] 无法连接 origin，使用本地已有代码（离线模式）" -ForegroundColor Yellow
        }

        # ---------- 3. 与远端比对并快进 ----------
        $syncState = "unknown"
        if ($remote) {
            if ($local -eq $remote) {
                $syncState = "synced"
                Write-Host "    [OK] 已与 origin/$Branch 同步" -ForegroundColor Green
            }
            else {
                $base = (& git merge-base HEAD "origin/$Branch").Trim()
                if ($base -eq $local) {
                    Write-Host "    [..] 本地落后于 origin/$Branch，正在快进拉取 ..." -ForegroundColor Yellow
                    & git pull --ff-only origin $Branch 2>$null | Out-Null
                    if ($LASTEXITCODE -eq 0) {
                        $local = (& git rev-parse HEAD).Trim()
                        $syncState = "synced"
                        Write-Host "    [OK] 已更新到最新提交" -ForegroundColor Green
                    }
                    else {
                        $syncState = "behind"
                        Write-Host "    [!] 快进失败（工作区有未提交改动），沿用本地 HEAD 打包" -ForegroundColor Yellow
                    }
                }
                elseif ($base -eq $remote) {
                    $syncState = "ahead"
                    Write-Host "    [!] 本地领先 origin/$Branch，存在未推送的提交" -ForegroundColor Yellow
                }
                else {
                    $syncState = "diverged"
                    Write-Host "    [!] 本地与 origin/$Branch 已分叉" -ForegroundColor Yellow
                }
            }
        }

        # ---------- 4. 工作区状态（按约定忽略，仅提示） ----------
        $dirtyFiles = @(& git status --porcelain)
        $dirtyCount = $dirtyFiles.Count
        if ($dirtyCount -gt 0) {
            Write-Host "    [!] 工作区有 $dirtyCount 处未提交改动（按配置忽略，直接打包）" -ForegroundColor Yellow
        }

        # ---------- 5. 提交信息 ----------
        $commitShort = (& git rev-parse --short HEAD).Trim()
        $commitInfo  = (& git log -1 --pretty=format:"%an|%ad|%s" --date=format:"%Y-%m-%d %H:%M")
        $parts    = $commitInfo -split "\|", 3
        $author   = $parts[0]
        $date     = $parts[1]
        $subject  = $parts[2]

        # ---------- 6. 正式环境护栏 ----------
        if ($RequireConfirm) {
            if ($syncState -eq "ahead" -or $syncState -eq "diverged") {
                Write-Host ""
                Write-Host "    [X] 正式环境要求 main 与 origin/main 完全一致，当前状态: $syncState" -ForegroundColor Red
                Write-Host "        请先处理本地未推送或分叉的提交后重新部署。" -ForegroundColor Red
                Write-Host ""
                return $null
            }

            Write-Host ""
            Write-Host "    ------------------------------------------" -ForegroundColor Red
            Write-Host "     即将部署到【正式环境】 $TargetHost" -ForegroundColor Red
            Write-Host "     分支   : $Branch" -ForegroundColor White
            Write-Host ("     提交   : " + $commitShort + "  " + $subject) -ForegroundColor White
            Write-Host ("     作者   : " + $author + " (" + $date + ")") -ForegroundColor White
            Write-Host ("     同步   : " + $syncState) -ForegroundColor White
            if ($dirtyCount -gt 0) {
                Write-Host ("     工作区 : 有 " + $dirtyCount + " 处未提交改动") -ForegroundColor Yellow
            }
            Write-Host "    ------------------------------------------" -ForegroundColor Red
            $answer = Read-Host "    确认继续请输入 yes"
            if ($answer -ne "yes") {
                Write-Host "    已取消部署。" -ForegroundColor Yellow
                return $null
            }
        }

        Write-Host ""
        $info = @{
            Branch  = $Branch
            Commit  = $commitShort
            Subject = $subject
            Author  = $author
            Date    = $date
            Sync    = $syncState
            Dirty   = $dirtyCount
        }
        return $info
    }
    finally {
        Pop-Location
    }
}

function Write-DeployHistory {
    param(
        [Parameter(Mandatory = $true)] [string] $ProjectDir,
        [Parameter(Mandatory = $true)] [string] $EnvName,
        [Parameter(Mandatory = $true)] [string] $TargetHost,
        [Parameter(Mandatory = $true)] $Info
    )

    $logDir  = Join-Path $ProjectDir "logs"
    $logFile = Join-Path $logDir "deploy-history.log"
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "$stamp | $EnvName | $TargetHost | $($Info.Branch) | $($Info.Commit) | $($Info.Subject) | sync=$($Info.Sync) dirty=$($Info.Dirty)"

    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host ("    部署记录已写入: " + $logFile) -ForegroundColor Gray
}
