@echo off
REM Script to set up and run the DevoxxGenie documentation site on Windows

echo Setting up DevoxxGenie Docusaurus documentation site...

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Node.js is required but not installed. Please install Node.js version 18 or higher.
    exit /b 1
)

REM Check if node_modules exists
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    
    if %ERRORLEVEL% neq 0 (
        echo Dependency installation failed. Please check the errors above.
        exit /b 1
    )
)

REM Note: npm start will automatically generate placeholder images
REM No need to run placeholder-setup.js separately

REM Start the development server
echo Starting Docusaurus development server...
echo The documentation site will be available at http://localhost:3000/
call npm start
