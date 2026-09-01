@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ============================================
echo   cacch-integration-platform Deployment
echo   Target: TEST (10.80.68.10) @ test branch
echo ============================================
echo.

set JAVA_HOME=D:\Software\Java\jdk21.0.10_7
set PATH=%JAVA_HOME%\bin;%PATH%

set MAVEN_HOME=D:\Software\apache-maven-3.9.9
set PATH=%MAVEN_HOME%\bin;%PATH%

set PYTHON=C:\Users\Administrator\.workbuddy\binaries\python\envs\deploy\Scripts\python.exe

set PROJECT_DIR=D:\Software\IdeaProject\cacch-integration-platform
set DEPLOY_BRANCH=test

cd /d "%PROJECT_DIR%"

echo [1/4] Git branch check (must be %DEPLOY_BRANCH%) ...
echo.

set CUR_BRANCH=
for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set CUR_BRANCH=%%i

if not defined CUR_BRANCH (
    echo.
    echo [X] Not a git repository: %PROJECT_DIR%
    echo.
    pause
    exit /b 1
)

if not "%CUR_BRANCH%"=="%DEPLOY_BRANCH%" (
    echo.
    echo [X] Branch mismatch: current [%CUR_BRANCH%], test env requires [%DEPLOY_BRANCH%]
    echo     Please run: git checkout %DEPLOY_BRANCH%
    echo.
    pause
    exit /b 1
)
echo    [OK] Current branch: %CUR_BRANCH%

git fetch --prune origin %DEPLOY_BRANCH% 2>nul
if !errorlevel! neq 0 (
    echo    [!] Cannot reach origin, building with local HEAD
) else (
    git pull --ff-only origin %DEPLOY_BRANCH% 2>nul
    if !errorlevel! neq 0 (
        echo    [!] Fast-forward failed, building with local HEAD
    ) else (
        echo    [OK] Updated from origin/%DEPLOY_BRANCH%
    )
)

set CUR_COMMIT=
for /f "delims=" %%i in ('git rev-parse --short HEAD') do set CUR_COMMIT=%%i
echo    [OK] Commit: %CUR_COMMIT%
echo.

echo [2/4] Maven packaging (mvn clean package -DskipTests) ...
echo.

call "%MAVEN_HOME%\bin\mvn.cmd" clean package -DskipTests 2>&1

if %errorlevel% neq 0 (
    echo.
    echo [X] Maven build failed! Please check errors above.
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo    [OK] Build successful
echo.

echo [3/4] Deploying to test server (10.80.68.10) ...
echo.

"%PYTHON%" "%PROJECT_DIR%\deploy.py" test

if %errorlevel% neq 0 (
    echo.
    echo [X] Deploy failed! Please check errors above.
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo [4/4] All done! branch=%CUR_BRANCH% commit=%CUR_COMMIT%
echo.
pause
