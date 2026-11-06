@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "HERE=%~dp0"
if not "%HERE:!=%"=="%HERE%" goto :badpath
if not "%HERE: =%"=="%HERE%" goto :badpath
where powershell >nul 2>&1 || goto :pathok
powershell -NoProfile -Command "if ($env:HERE -notmatch '^[A-Za-z]:[A-Za-z0-9_.\\-]*$') { exit 1 }"
if errorlevel 1 goto :badpath

:pathok

rem ---- config ----
set "MOD_FILE=supervault"
set "MIN_JAVA=21"
set "MODS_DIR=%APPDATA%\.minecraft\mods"

set "TASK=%~1"
if "%TASK%"=="" set "TASK=build"

set "GRADLE_ARGS="
if /i "%TASK%"=="build"  set "GRADLE_ARGS=build"
if /i "%TASK%"=="clean"  set "GRADLE_ARGS=clean build"
if /i "%TASK%"=="deploy" set "GRADLE_ARGS=build"
if /i "%TASK%"=="client" set "GRADLE_ARGS=runClient"
if /i "%TASK%"=="server" set "GRADLE_ARGS=runServer"
if /i "%TASK%"=="help" goto :usage
if not defined GRADLE_ARGS goto :unknown

rem ---- java check ----
set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "JAVA_VER="
for /f "tokens=1-3" %%a in ('"%JAVA_EXE%" -version 2^>^&1') do if /i "%%b"=="version" if not defined JAVA_VER set "JAVA_VER=%%~c"
if not defined JAVA_VER goto :nojava
for /f "delims=.-" %%m in ("%JAVA_VER%") do set "JAVA_MAJOR=%%m"
if %JAVA_MAJOR% LSS %MIN_JAVA% goto :oldjava

rem ---- version ----
set "VERSION="
for /f "usebackq tokens=1,* delims==" %%a in ("gradle.properties") do if /i "%%a"=="version" set "VERSION=%%b"
if not defined VERSION goto :noversion

rem ---- run gradle ----
call gradlew.bat %GRADLE_ARGS%
if errorlevel 1 goto :failed

if /i "%TASK%"=="client" exit /b 0
if /i "%TASK%"=="server" exit /b 0

set "JAR=%~dp0build\libs\%MOD_FILE%-%VERSION%.jar"
if not exist "%JAR%" goto :nojar
echo.
echo Built: %JAR%

if /i not "%TASK%"=="deploy" exit /b 0
if not "%~2"=="" set "MODS_DIR=%~2"
if not exist "%MODS_DIR%" mkdir "%MODS_DIR%"
del /q "%MODS_DIR%\%MOD_FILE%-*.jar" 2>nul
copy /y "%JAR%" "%MODS_DIR%\" >nul
if errorlevel 1 goto :copyfail
echo Deployed to: %MODS_DIR%
exit /b 0

:usage
echo Usage: build.bat [build^|clean^|client^|server^|deploy [dir]^|help]
exit /b 0

:unknown
echo Unknown task: %TASK%
echo Usage: build.bat [build^|clean^|client^|server^|deploy [dir]^|help]
exit /b 1

:nojava
echo Java not found. Install Java %MIN_JAVA%+ and set JAVA_HOME or PATH.
exit /b 1

:oldjava
echo Java %JAVA_VER% found, Java %MIN_JAVA%+ required.
exit /b 1

:noversion
echo Version not found in gradle.properties.
exit /b 1

:failed
echo.
echo Gradle failed.
exit /b 1

:nojar
echo Jar not found: %JAR%
exit /b 1

:copyfail
echo Copy to %MODS_DIR% failed.
exit /b 1

:badpath
echo Unsafe project path: "%HERE%"
echo Gradle and Loom can fail on "!", spaces and non-ASCII characters in the path.
echo Use only letters, digits, "_", "-" and "." in folder names, e.g. C:\dev\SuperVault-Minecraft
exit /b 1
