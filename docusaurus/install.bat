@echo off
REM Script to set up and run the DevoxxGenie documentation site on Windows

echo Setting up DevoxxGenie Docusaurus documentation site...

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Node.js is required but not installed. Please install Node.js version 18 or higher.
    exit /b 1
)

REM Check Node.js version
for /f "tokens=1,2,3 delims=." %%a in ('node -v') do (
    set NODE_VERSION=%%a
)
set NODE_VERSION=%NODE_VERSION:~1%
if %NODE_VERSION% LSS 18 (
    echo Node.js version 18 or higher is required. Current version: %NODE_VERSION%
    exit /b 1
)

REM Check if npm is installed
where npm >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo npm is required but not installed. Please install npm (usually comes with Node.js).
    exit /b 1
)

REM Install dependencies
echo Installing dependencies...
call npm install

REM Verify installation
if %ERRORLEVEL% neq 0 (
    echo Dependency installation failed. Please check the errors above.
    exit /b 1
)

REM Start the development server
echo Starting Docusaurus development server...
echo The documentation site will be available at http://localhost:3000/
call npm start
