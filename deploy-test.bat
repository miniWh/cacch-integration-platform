@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════╗
echo ║  cacch-integration-platform 一键部署  ║
echo ╚════════════════════════════════════════╝
echo.

REM ============== JDK 21 环境 ==============
set JAVA_HOME=D:\Software\Java\jdk21.0.10_7
set PATH=%JAVA_HOME%\bin;%PATH%

set MAVEN_HOME=D:\Software\apache-maven-3.9.9
set PATH=%MAVEN_HOME%\bin;%PATH%

set PYTHON=C:\Users\Administrator\.workbuddy\binaries\python\versions\3.13.12\python.exe

REM ============== 步骤1: Maven 打包 ==============
echo [1/3] Maven 打包 (mvn clean package -DskipTests) ...
echo.
cd /d "D:\Software\IdeaProject\cacch-integration-platform"

call "%MAVEN_HOME%\bin\mvn.cmd" clean package -DskipTests 2>&1

if %errorlevel% neq 0 (
    echo.
    echo ┌──────────────────────────────────────┐
    echo │  ❌ Maven 打包失败，请检查编译错误    │
    echo └──────────────────────────────────────┘
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo    ✅ 打包成功
echo.

REM ============== 步骤2: 调用部署脚本 ==============
echo [2/3] 上传并部署到测试环境 (10.80.68.10) ...
echo.

"%PYTHON%" "D:\Software\IdeaProject\cacch-integration-platform\deploy.py" test

if %errorlevel% neq 0 (
    echo.
    echo ┌──────────────────────────────────────┐
    echo │  ❌ 部署失败，请查看上方错误信息      │
    echo └──────────────────────────────────────┘
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] 全部完成! 🎉
echo.
pause
