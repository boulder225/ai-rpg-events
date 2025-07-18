#!/bin/bash

echo "🔧 Setting up script permissions..."

# Make all scripts executable
chmod +x start-backend.sh
chmod +x start-frontend.sh  
chmod +x test-enhanced.sh
chmod +x test-location-context.sh
chmod +x location-context-summary.sh
chmod +x debug-movement.sh
chmod +x debug-movement-enhanced.sh
chmod +x make-executable.sh

# Also make gradlew executable (needed for the backend)
chmod +x gradlew

echo "✅ All scripts are now executable:"
echo "  ./start-backend.sh - ✓"
echo "  ./start-frontend.sh - ✓"  
echo "  ./test-enhanced.sh - ✓"
echo "  ./test-location-context.sh - ✓ (NEW!)"
echo "  ./location-context-summary.sh - ✓ (NEW!)"
echo "  ./debug-movement.sh - ✓ (BUG FIX!)"
echo "  ./gradlew - ✓"
echo
echo "🚀 You can now run: ./start-backend.sh"
echo "🧪 To test location context: ./test-location-context.sh"
echo "📋 For implementation summary: ./location-context-summary.sh"
echo "🔧 To debug movement issues: ./debug-movement.sh"
