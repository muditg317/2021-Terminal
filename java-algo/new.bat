@echo off
SET target=%1>nul
SET target>nul
SET target=%target:"=%>nul
SET target>nul
SET comment=%2>nul
SET comment>nul
SET comment=%comment:"=%>nul
SET comment>nul
SET message="code for %target%">nul
IF "%comment%" NEQ "" SET message=%message%: %comment%>nul
SET message>nul
SET message=%message:"=%>nul
SET message>nul

echo commit code: %message%
git add -A >nul 2>nul
git commit -am "%message%" >nul 2>nul

echo create algo folder: %target%
Xcopy /E /I /Y algo-target "algos\%target%" >nul

echo commit built %target%
git add "algos\%target%" >nul 2>nul
git commit -m "build %target%" >nul 2>nul
@echo on