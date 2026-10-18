@echo off
cd /d C:\KCMART\storefront
call npx.cmd tsc --noEmit > step52_53_typescript.log 2>&1
exit /b %ERRORLEVEL%