@echo off
REM slimefun-slim build pipeline (Windows)
REM
REM Direction 4: git submodule + patch mechanism.
REM The upstream Slimefun source is compiled UNCHANGED from the "upstream"
REM submodule. All modifications are applied at build time:
REM   1. checkout the pinned (or latest) upstream commit
REM   2. apply the patches from patches\ (currently: MC 26 support)
REM   3. compile upstream source + overlay\ stubs (maven-shade-plugin weaves
REM      PaperLib & dough replacements in)
REM
REM Usage:
REM   build.bat               reproducible build of the pinned upstream commit
REM   build.bat --update      track the latest upstream "experimental" HEAD
REM   build.bat -Pslim        also pass any maven arguments (e.g. -Pslim, -Dmaven.test.skip=true)
REM
REM Output jars are copied to .\dist\
setlocal enabledelayedexpansion
cd /d "%~dp0"

set UPDATE=0
set SKIP_CHECKOUT=0
set MVN_ARGS=

:parse
if "%~1"=="" goto parse_done
if "%~1"=="--update" ( set UPDATE=1 & shift & goto parse )
if "%~1"=="--skip-checkout" ( set SKIP_CHECKOUT=1 & shift & goto parse )
set MVN_ARGS=!MVN_ARGS! %~1
shift
goto parse
:parse_done

REM 1. Read the pinned upstream version from pom.xml
for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -Command "(Select-String -Path 'pom.xml' -Pattern '<upstream.version>([0-9a-f]+)</upstream.version>').Matches[0].Groups[1].Value"`) do set UPSTREAM_VERSION=%%i
if "%UPSTREAM_VERSION%"=="" (
    echo [build] ERROR: could not read ^<upstream.version^> from pom.xml
    exit /b 1
)

REM 2. Make sure the upstream submodule is present
git submodule update --init upstream
if errorlevel 1 exit /b 1

REM 3. Checkout the source we build against
if "%SKIP_CHECKOUT%"=="1" (
    echo [build] using the upstream working tree as-is
) else if "%UPDATE%"=="1" (
    echo [build] tracking latest upstream 'experimental'...
    git -C upstream fetch origin experimental
    if errorlevel 1 exit /b 1
    git -C upstream checkout --force origin/experimental
    if errorlevel 1 exit /b 1
) else (
    echo [build] building pinned upstream %UPSTREAM_VERSION%...
    git -C upstream checkout --force %UPSTREAM_VERSION%
    if errorlevel 1 exit /b 1
)

REM 4. Apply our patches (idempotent: reverse first, then apply)
for %%p in (patches\*.patch) do (
    echo [build] applying %%~nxp
    git -C upstream apply -R "..\%%p" 2>nul
    git -C upstream apply --check "..\%%p"
    if errorlevel 1 exit /b 1
    git -C upstream apply "..\%%p"
    if errorlevel 1 exit /b 1
)

REM 5. Build
REM Upstream unit tests cannot run against paper-api 26 (stale mocks /
REM MockBukkit 1.21 vs MC 26 API), so they are skipped by default.
REM Pass -Dmaven.test.skip=false in MVN_ARGS to override.
call mvn -B clean package -Dmaven.test.skip=true %MVN_ARGS%
if errorlevel 1 exit /b 1

REM 6. Collect the jars
if not exist dist mkdir dist
copy /y "target\Slimefun v*.jar" dist\ >nul 2>&1
echo.
echo [build] done. Artifacts:
dir /b dist\
