@echo off
REM AI-RPG Event Sourcing Platform Startup Script for Windows
REM Sets up Claude AI integration and starts the server

echo 🎮 AI-RPG Event Sourcing Platform
echo =================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo 📝 Setting up environment configuration...
    copy .env.example .env
    echo ✅ Created .env file from template
    echo.
    echo 🔧 Next steps:
    echo 1. Edit .env file and add your Claude API key:
    echo    CLAUDE_API_KEY=your_actual_api_key_here
    echo 2. Run this script again to start the server
    echo.
    echo 💡 The platform works in simulation mode without an API key!
    pause
    exit /b 0
)

REM Check if Claude API key is configured
findstr /C:"CLAUDE_API_KEY=your_claude_api_key_here" .env >nul
if %errorlevel%==0 (
    echo ⚠️  Claude AI: SIMULATION MODE
    echo    Add your API key to .env for real AI responses
) else (
    echo 🤖 Claude AI: CONFIGURED
    echo    Real intelligent responses enabled
)

echo.
echo 🚀 Starting AI-RPG server...
echo.

REM Start the server
gradlew.bat run
