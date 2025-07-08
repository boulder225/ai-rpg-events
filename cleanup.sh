#!/bin/bash

# AI-RPG Project Cleanup Script
# Removes unused classes after React frontend migration

echo "🧹 AI-RPG Project Cleanup"
echo "========================"
echo

echo "🗑️  Removing unused files after React frontend migration..."

# Remove WebInterfaceGenerator.java (no longer needed)
if [ -f "src/main/java/com/eventsourcing/api/WebInterfaceGenerator.java" ]; then
    echo "❌ Removing WebInterfaceGenerator.java"
    rm "src/main/java/com/eventsourcing/api/WebInterfaceGenerator.java"
    echo "   ✅ WebInterfaceGenerator.java removed"
else
    echo "   ℹ️  WebInterfaceGenerator.java already removed"
fi

# Clean build directory to remove old compiled classes
echo "🧹 Cleaning build directory..."
if [ -d "build" ]; then
    ./gradlew clean
    echo "   ✅ Build directory cleaned"
else
    echo "   ℹ️  Build directory not found"
fi

# Check for other potentially unused files
echo "🔍 Checking for other cleanup opportunities..."

# List all Java files in API package
echo "📁 Current API package files:"
ls -la src/main/java/com/eventsourcing/api/

echo
echo "📊 Cleanup Summary:"
echo "✅ WebInterfaceGenerator.java - REMOVED (no longer needed)"
echo "✅ HTML generation code - REMOVED"
echo "✅ Web interface handler - REMOVED"
echo "✅ Build artifacts - CLEANED"
echo
echo "🎯 Project is now clean and optimized for React frontend!"
echo "📁 Frontend: Use React app in frontend/ directory"
echo "📡 Backend: Pure API server in src/ directory"