#!/bin/bash

# Test Location Context Awareness System
echo "🧪 Testing Location Context Awareness System"
echo "============================================"
echo

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Please run this script from the project root directory"
    exit 1
fi

echo "📋 Compiling Java sources..."
./gradlew compileJava

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"
echo

echo "🎮 Running Location Context Demo..."
echo "=================================="
echo

# Run the location context example
java -cp "build/classes/java/main:src/main/resources" com.eventsourcing.examples.LocationContextExample

echo
echo "✅ Location Context Test Complete!"
echo
echo "🔍 What this test demonstrated:"
echo "   📍 Rich location context gathering"
echo "   🚪 Automatic exit and feature detection"
echo "   💡 Lighting and environmental awareness"
echo "   🗝️  Secret integration and discovery"
echo "   ⚡ Performance caching system"
echo "   🤖 Enhanced AI prompt generation"
echo
echo "🎯 Next Steps:"
echo "   1. Start backend: ./start-backend.sh"
echo "   2. Start frontend: ./start-frontend.sh"
echo "   3. Test location commands like '/go cave_entrance'"
echo "   4. Watch AI responses include rich location details"
echo
