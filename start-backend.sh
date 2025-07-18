#!/bin/bash

# AI-RPG Platform - Enhanced Backend API Server Startup with Location Context
echo "🎮 AI-RPG Event Sourcing Platform - Enhanced Backend API with Location Context"
echo "==========================================================================="
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
    echo "2. Run this script again to start the backend"
    echo
    echo "💡 The platform works in simulation mode without an API key!"
    exit 0
fi

# Check if Claude API key is configured
if grep -q "CLAUDE_API_KEY=your_claude_api_key_here" .env || ! grep -q "CLAUDE_API_KEY=" .env; then
    echo "⚠️  Claude AI: SIMULATION MODE"
    echo "   Add your API key to .env for real AI responses"
    echo "🔧 Enhanced Features: LIMITED (basic context only)"
else
    echo "🤖 Claude AI: CONFIGURED"
    echo "   Real intelligent responses enabled"
    echo "✨ Enhanced Features: FULLY ACTIVE"
    echo "   🎯 World state consistency tracking"
    echo "   🛡️ D&D rules validation" 
    echo "   🧠 Context management with memory"
    echo "   📍 Location Context Awareness (NEW!)"
    echo "   🎲 Equipment vulnerability (rust monsters!)"
fi

echo
echo "🚀 Starting enhanced backend API server..."
echo "📡 API will be available at: http://localhost:8080"
echo "🌐 Start frontend with: ./start-frontend.sh"
echo
echo "📚 Enhanced API Endpoints:"
echo "   📌 Classic endpoints + enhanced context management"
echo "   GET  /api/game/status - Complete world state with location context"
echo "   POST /api/game/action - AI responses with rich location awareness"
echo "   GET  /api/ai/prompt - View enhanced AI context including location details"
echo "   GET  /api/metrics - System metrics + location context cache stats"
echo
echo "🆕 Location Context Features:"
echo "   🏠 Rich environmental descriptions"
echo "   🚪 Automatic exit and feature detection"
echo "   💡 Lighting and exploration awareness"
echo "   🗝️  Secret discovery integration"
echo "   ⚡ Performance caching for smooth gameplay"
echo
echo "🧪 Test the location system:"
echo "   java -cp src/main/java com.eventsourcing.examples.LocationContextExample"
echo

# Start the backend server
./gradlew run