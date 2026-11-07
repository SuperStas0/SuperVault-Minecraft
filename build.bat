@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "HERE=%~dp0"
set "MOD_FILE=supervault"
set "MIN_JAVA=21"
set "MODS_DIR=%APPDATA%\.minecraft\mods"
set "OPT_C="
set "OPT_I="
set "OPT_R="
set "OPT_H="
set "OPT_BAD="

:nextarg
if "%~1"=="" goto :argsdone
set "FLAGS=%~1"
shift
if "%FLAGS:~0,1%"=="-" (set "FLAGS=%FLAGS:~1%") else set "OPT_BAD=1"
if not defined FLAGS set "OPT_BAD=1"
:nextchar
if not defined FLAGS goto :nextarg
set "CH=%FLAGS:~0,1%"
set "FLAGS=%FLAGS:~1%"
if "%CH%"=="c" (set "OPT_C=1") else if "%CH%"=="i" (set "OPT_I=1") else if "%CH%"=="r" (set "OPT_R=1") else if "%CH%"=="h" (set "OPT_H=1") else set "OPT_BAD=1"
goto :nextchar

:argsdone
if defined OPT_H goto :usage
if defined OPT_BAD goto :unknown

if not "%HERE:!=%"=="%HERE%" goto :badpath
if not "%HERE: =%"=="%HERE%" goto :badpath
where powershell >nul 2>&1 || goto :pathok
powershell -NoProfile -Command "if ($env:HERE -notmatch '^[A-Za-z]:[A-Za-z0-9_.\\-]*$') { exit 1 }"
if errorlevel 1 goto :badpath
:pathok

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "JAVA_VER="
for /f "tokens=1-3" %%a in ('"%JAVA_EXE%" -version 2^>^&1') do if /i "%%b"=="version" if not defined JAVA_VER set "JAVA_VER=%%~c"
if not defined JAVA_VER goto :nojava
for /f "delims=.-" %%m in ("%JAVA_VER%") do set "JAVA_MAJOR=%%m"
if %JAVA_MAJOR% LSS %MIN_JAVA% goto :oldjava

set "VERSION="
for /f "usebackq tokens=1,* delims==" %%a in ("gradle.properties") do if /i "%%a"=="version" set "VERSION=%%b"
if not defined VERSION goto :noversion

set "GRADLE_ARGS=build"
if defined OPT_C set "GRADLE_ARGS=clean build"
call gradlew.bat %GRADLE_ARGS%
if errorlevel 1 goto :failed

set "JAR=%HERE%build\libs\%MOD_FILE%-%VERSION%.jar"
if not exist "%JAR%" goto :nojar
echo Built: %JAR%

if defined OPT_I call :install || exit /b 1
if defined OPT_R call :release || exit /b 1
exit /b 0

:install
if not exist "%MODS_DIR%" mkdir "%MODS_DIR%"
del /q "%MODS_DIR%\%MOD_FILE%-*.jar" 2>nul
copy /y "%JAR%" "%MODS_DIR%\" >nul || goto :installfail
echo Installed to: %MODS_DIR%
exit /b 0

:installfail
echo Copy to %MODS_DIR% failed.
exit /b 1

:release
if not exist "%HERE%release" mkdir "%HERE%release"
del /q "%HERE%release\%MOD_FILE%-*.jar" 2>nul
copy /y "%JAR%" "%HERE%release\" >nul || goto :releasefail
echo Released to: %HERE%release
exit /b 0

:releasefail
echo Copy to %HERE%release failed.
exit /b 1

:usage
echo Usage: build.bat [-c] [-i] [-r] [-h]
echo   -c  clean: delete previous build output first
echo   -i  install: copy the build to %MODS_DIR%
echo   -r  release: package the build into release\
echo   -h  show this help
echo Build output: %HERE%build\libs
echo Release:      %HERE%release
exit /b 0

:unknown
echo Unknown flag, see build.bat -h
exit /b 1

:nojava
echo Java not found, install Java %MIN_JAVA%+ and set JAVA_HOME or PATH.
exit /b 1

:oldjava
echo Java %JAVA_VER% found, Java %MIN_JAVA%+ required.
exit /b 1

:noversion
echo Version not found in gradle.properties.
exit /b 1

:failed
echo Gradle failed.
exit /b 1

:nojar
echo Jar not found: %JAR%
exit /b 1

:badpath
echo Unsafe project path "%HERE%", use only letters, digits, "_", "-" and "." in folder names.
exit /b 1
