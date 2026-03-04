@echo off
REM ============================================================
REM  start-backend.bat  -  Start all backend services in order
REM  Run from the repo root:  start-backend.bat
REM ============================================================

setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "BACK=%ROOT%backEnd"
set "LOG_DIR=%ROOT%logs"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo.
echo ============================================================
echo   Smart Freelance - Backend Startup
echo ============================================================
echo.

REM ── 1. Eureka (port 8420) ─────────────────────────────────
echo [1/4] Starting Eureka (port 8420)...
start "Eureka" /D "%BACK%\Eureka" cmd /c "mvn spring-boot:run > "%LOG_DIR%\eureka.log" 2>&1"
call :wait_for_port 8420 "Eureka" 120

REM ── 2. Config Server (port 8888) ──────────────────────────
echo [2/4] Starting Config Server (port 8888)...
start "ConfigServer" /D "%BACK%\ConfigServer" cmd /c "mvn spring-boot:run > "%LOG_DIR%\config-server.log" 2>&1"
call :wait_for_port 8888 "Config Server" 90

REM ── 3. Keycloak Auth service (port 8079) ──────────────────
echo [3/4] Starting Keycloak Auth service (port 8079)...
echo       NOTE: Keycloak itself (port 8080) must already be running!
start "KeycloakAuth" /D "%BACK%\KeyCloak" cmd /c "mvn spring-boot:run > "%LOG_DIR%\keycloak-auth.log" 2>&1"
call :wait_for_port 8079 "Keycloak Auth" 90

REM ── 4. API Gateway (port 8078) ────────────────────────────
echo [4/4] Starting API Gateway (port 8078)...
start "ApiGateway" /D "%BACK%\apiGateway" cmd /c "mvn spring-boot:run > "%LOG_DIR%\api-gateway.log" 2>&1"
call :wait_for_port 8078 "API Gateway" 90

REM ── 5. Microservices ──────────────────────────────────────
echo.
echo Starting all microservices...
echo.

for %%S in (user planning Offer Contract Project review Portfolio Notification task) do (
  if exist "%BACK%\Microservices\%%S" (
    echo   Starting %%S...
    start "%%S" /D "%BACK%\Microservices\%%S" cmd /c "mvn spring-boot:run > "%LOG_DIR%\%%S.log" 2>&1"
  ) else (
    echo   [SKIP] Directory not found: Microservices\%%S
  )
)

echo.
echo ============================================================
echo   All services launched in separate windows.
echo.
echo   Eureka dashboard : http://localhost:8420
echo   API Gateway      : http://localhost:8078
echo   Keycloak Auth    : http://localhost:8079
echo   Config Server    : http://localhost:8888
echo.
echo   Logs saved in:     %LOG_DIR%
echo.
echo   Close the individual windows or run stop-backend.bat to stop.
echo ============================================================
echo.
goto :eof

REM ── Helper: wait until a port responds ────────────────────
:wait_for_port
set "port=%~1"
set "label=%~2"
set "timeout=%~3"
set /a elapsed=0
:wait_loop
  curl -s "http://localhost:%port%/actuator/health" 2>nul | findstr /i "UP" >nul 2>&1
  if !errorlevel! equ 0 (
    echo   [OK] %label% is UP
    goto :eof
  )
  curl -s "http://localhost:%port%/" 2>nul | findstr /i "eureka html OK" >nul 2>&1
  if !errorlevel! equ 0 (
    echo   [OK] %label% is UP
    goto :eof
  )
  timeout /t 3 /nobreak >nul
  set /a elapsed+=3
  if !elapsed! geq %timeout% (
    echo   [WARN] %label% not ready after %timeout%s - continuing anyway
    goto :eof
  )
goto wait_loop
