#!/bin/bash

echo "🔍 API Response Format Checker"
echo "=============================="
echo

# Test session creation and inspect the exact response format
echo "📡 Testing session creation API response format..."

if ! curl -s http://localhost:8080/api/metrics > /dev/null; then
    echo "❌ Backend server is not running!"
    echo "   Please start it with: ./start-backend.sh"
    exit 1
fi

echo "✅ Backend server is running"
echo

# Create session and show the exact response
echo "🎮 Creating test session..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/session/create \
  -H "Content-Type: application/json" \
  -d '{"player_id":"format-test","player_name":"Format Test"}')

echo "📄 Raw API Response:"
echo "$RESPONSE"
echo

# Show formatted JSON if possible
if command -v jq &> /dev/null; then
    echo "📋 Formatted JSON:"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "   Failed to parse as JSON"
    echo
    
    echo "🔍 Available fields:"
    echo "$RESPONSE" | jq -r 'keys[]' 2>/dev/null || echo "   Failed to extract keys"
    echo
    
    echo "📋 Session ID extraction test:"
    SESSION_ID=$(echo "$RESPONSE" | jq -r '.session_id // empty' 2>/dev/null)
    echo "   Extracted session_id: '$SESSION_ID'"
else
    echo "💡 Install jq for better JSON analysis: brew install jq"
fi

echo
echo "🧪 Testing field extraction methods:"

# Test grep extraction
SESSION_ID_GREP=$(echo "$RESPONSE" | grep -o '"session_id":"[^"]*"' | cut -d'"' -f4)
echo "   grep method: '$SESSION_ID_GREP'"

# Test sed extraction
SESSION_ID_SED=$(echo "$RESPONSE" | sed -n 's/.*"session_id":"\([^"]*\)".*/\1/p')
echo "   sed method: '$SESSION_ID_SED'"

echo
if [ -n "$SESSION_ID_GREP" ] || [ -n "$SESSION_ID_SED" ]; then
    echo "✅ Session ID extraction working correctly!"
else
    echo "❌ Session ID extraction failed - check API response format"
fi
