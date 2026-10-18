@echo off
cd /d C:\KCMART\storefront
call npm.cmd run build > step52_54_build_output.log 2>&1
exit /b %ERRORLEVEL%