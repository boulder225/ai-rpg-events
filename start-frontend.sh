#!/bin/bash

# AI-RPG Platform - Enhanced Frontend Startup with Location Context
echo "🎮 AI-RPG Event Sourcing Platform - Enhanced React Frontend with Location Context"
echo "=================================================================================="
echo

# Check if we're in the frontend directory
if [ ! -f "package.json" ]; then
    if [ -d "frontend" ]; then
        echo "📁 Switching to frontend directory..."
        cd frontend
    else
        echo "❌ Frontend directory not found!"
        echo "Run this script from the project root or frontend directory"
        exit 1
    fi
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing React dependencies..."
    npm install
    echo "✅ Dependencies installed"
    echo
fi

echo "🚀 Starting React development server..."
echo "📍 Frontend will be available at: http://localhost:3000"
echo "📡 Enhanced Backend API should be running at: http://localhost:8080"
echo
echo "✨ Enhanced Features Available:"
echo "   🎯 Real-time world state consistency"
echo "   🛡️ D&D rules validation in real-time"
echo "   🧠 AI responses with full context memory"
echo "   📍 Location Context Awareness (NEW!)"
echo "   📊 Enhanced metrics and debugging"
echo
echo "🆕 Location Context in Frontend:"
echo "   🏠 Rich location descriptions in responses"
echo "   🚪 Available exits shown contextually"
echo "   💡 Lighting conditions affect gameplay"
echo "   🗺️ Auto-updating world state display"
echo "   🎮 Immersive environmental storytelling"
echo
echo "💡 Development Tips:"
echo "   • Use commands like '/go cave_entrance' to test location changes"
echo "   • Check browser console for location context updates"
echo "   • Watch the status panel for real-time location info"
echo "   • AI responses now include rich environmental details"
echo
echo "💡 Make sure the enhanced backend server is running first!"
echo "   Run './start-backend.sh' in another terminal"
echo

# Start React development server
npm start