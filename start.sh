#!/bin/bash

# AI-RPG Event Sourcing Platform Startup Script
# Sets up Claude AI integration and starts the server

echo "🎮 AI-RPG Event Sourcing Platform"
echo "================================="
echo

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "📝 Setting up environment configuration..."
    cp .env.example .env
    echo "✅ Created .env file from template"
    echo
    echo "🔧 Next steps:"
    echo "1. Edit .env file and add your Claude API key:"
    echo "   CLAUDE_API_KEY=your_actual_api_key_here"
    echo "2. Run this script again to start the server"
    echo
    echo "💡 The platform works in simulation mode without an API key!"
    exit 0
fi

# Check if Claude API key is configured
if grep -q "CLAUDE_API_KEY=your_claude_api_key_here" .env || ! grep -q "CLAUDE_API_KEY=" .env; then
    echo "⚠️  Claude AI: SIMULATION MODE"
    echo "   Add your API key to .env for real AI responses"
else
    echo "🤖 Claude AI: CONFIGURED"
    echo "   Real intelligent responses enabled"
fi

echo
echo "🚀 Starting AI-RPG server..."
echo

# Start the server
./gradlew run
