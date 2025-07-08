#!/bin/bash

# AI-RPG Platform - Decoupled Frontend/Backend Startup
echo "🎮 AI-RPG Event Sourcing Platform - React Frontend"
echo "================================================="
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
echo "📡 Backend API should be running at: http://localhost:8080"
echo
echo "💡 Make sure the backend server is running first!"
echo "   Run './start-backend.sh' in another terminal"
echo

# Start React development server
npm start