@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ============================================
echo   cacch-integration-platform Deployment
echo ============================================
echo.

set JAVA_HOME=D:\Software\Java\jdk21.0.10_7
set PATH=%JAVA_HOME%\bin;%PATH%

set MAVEN_HOME=D:\Software\apache-maven-3.9.9
set PATH=%MAVEN_HOME%\bin;%PATH%

set PYTHON=C:\Users\Administrator\.workbuddy\binaries\python\versions\3.13.12\python.exe

echo [1/3] Maven packaging (mvn clean package -DskipTests) ...
echo.
cd /d "D:\Software\IdeaProject\cacch-integration-platform"

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

echo [2/3] Deploying to test server (10.80.68.10) ...
echo.

"%PYTHON%" "D:\Software\IdeaProject\cacch-integration-platform\deploy.py" test

if %errorlevel% neq 0 (
    echo.
    echo [X] Deploy failed! Please check errors above.
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] All done!
echo.
pause
