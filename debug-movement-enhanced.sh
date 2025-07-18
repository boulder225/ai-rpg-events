#!/bin/bash

echo "🔧 Location Context Bug Fix - Testing Movement Commands (Enhanced Debug)"
echo "========================================================================"
echo

# Check if server is running
echo "📡 Checking if backend server is running..."
if ! curl -s http://localhost:8080/api/metrics > /dev/null; then
    echo "❌ Backend server is not running!"
    echo "   Please start it with: ./start-backend.sh"
    exit 1
fi

echo "✅ Backend server is running"
echo

# Create a test session with verbose debugging
echo "🎮 Creating test session..."
echo "Debug: Sending POST request to /api/session/create"

SESSION_RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/session/create \
  -H "Content-Type: application/json" \
  -d '{"player_id":"test-player","player_name":"Debug Hero"}')

# Extract HTTP code and response body
HTTP_CODE=$(echo "$SESSION_RESPONSE" | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
RESPONSE_BODY=$(echo "$SESSION_RESPONSE" | sed 's/HTTP_CODE:[0-9]*$//')

echo "📊 Session creation response:"
echo "   HTTP Code: $HTTP_CODE"
echo "   Response Body: $RESPONSE_BODY"
echo

# Check if session creation was successful
if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Session creation failed with HTTP code: $HTTP_CODE"
    echo "   Response: $RESPONSE_BODY"
    exit 1
fi

# Try different methods to extract session ID
echo "🔍 Extracting session ID..."

# Method 1: Using grep (fixed: use snake_case field name)
SESSION_ID_1=$(echo "$RESPONSE_BODY" | grep -o '"session_id":"[^"]*"' | cut -d'"' -f4)
echo "   Method 1 (grep): '$SESSION_ID_1'"

# Method 2: Using jq if available (fixed: use snake_case field name)
if command -v jq &> /dev/null; then
    SESSION_ID_2=$(echo "$RESPONSE_BODY" | jq -r '.session_id // empty' 2>/dev/null)
    echo "   Method 2 (jq): '$SESSION_ID_2'"
else
    echo "   Method 2 (jq): not available"
fi

# Method 3: Using sed (fixed: use snake_case field name)
SESSION_ID_3=$(echo "$RESPONSE_BODY" | sed -n 's/.*"session_id":"\([^"]*\)".*/\1/p')
echo "   Method 3 (sed): '$SESSION_ID_3'"

# Use the first non-empty session ID
SESSION_ID=""
if [ -n "$SESSION_ID_1" ]; then
    SESSION_ID="$SESSION_ID_1"
    echo "✅ Using session ID from method 1: $SESSION_ID"
elif [ -n "$SESSION_ID_2" ]; then
    SESSION_ID="$SESSION_ID_2" 
    echo "✅ Using session ID from method 2: $SESSION_ID"
elif [ -n "$SESSION_ID_3" ]; then
    SESSION_ID="$SESSION_ID_3"
    echo "✅ Using session ID from method 3: $SESSION_ID"
fi

if [ -z "$SESSION_ID" ]; then
    echo "❌ Failed to extract session ID from response"
    echo "   Full response: $RESPONSE_BODY"
    echo "   Please check the response format"
    
    # Try to show the JSON structure
    echo "🔍 Attempting to parse JSON structure:"
    if command -v jq &> /dev/null; then
        echo "$RESPONSE_BODY" | jq . 2>/dev/null || echo "   Invalid JSON format"
    else
        echo "   Install jq for better JSON debugging: brew install jq"
    fi
    exit 1
fi

echo

# Test initial status
echo "📊 Checking initial player status..."
STATUS_RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" "http://localhost:8080/api/game/status?session_id=$SESSION_ID")
STATUS_HTTP_CODE=$(echo "$STATUS_RESPONSE" | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
STATUS_BODY=$(echo "$STATUS_RESPONSE" | sed 's/HTTP_CODE:[0-9]*$//')

echo "   Status HTTP Code: $STATUS_HTTP_CODE"
if [ "$STATUS_HTTP_CODE" = "200" ]; then
    CURRENT_LOCATION=$(echo "$STATUS_BODY" | grep -o '"current_location":"[^"]*"' | cut -d'"' -f4)
    echo "   Current location: $CURRENT_LOCATION"
else
    echo "   ❌ Failed to get status: $STATUS_BODY"
fi
echo

# Test movement to cave entrance
echo "🚶 Testing movement: village -> cave_entrance"
MOVE_RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/game/action \
  -H "Content-Type: application/json" \
  -d "{\"session_id\":\"$SESSION_ID\",\"command\":\"/go cave_entrance\"}")

MOVE_HTTP_CODE=$(echo "$MOVE_RESPONSE" | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
MOVE_BODY=$(echo "$MOVE_RESPONSE" | sed 's/HTTP_CODE:[0-9]*$//')

echo "   Move HTTP Code: $MOVE_HTTP_CODE"
echo "   Move Response Preview:"
echo "$MOVE_BODY" | head -c 300
echo "..."
echo

# Check updated status
echo "📊 Checking updated player status..."
STATUS_RESPONSE2=$(curl -s -w "HTTP_CODE:%{http_code}" "http://localhost:8080/api/game/status?session_id=$SESSION_ID")
STATUS_HTTP_CODE2=$(echo "$STATUS_RESPONSE2" | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
STATUS_BODY2=$(echo "$STATUS_RESPONSE2" | sed 's/HTTP_CODE:[0-9]*$//')

if [ "$STATUS_HTTP_CODE2" = "200" ]; then
    NEW_LOCATION=$(echo "$STATUS_BODY2" | grep -o '"current_location":"[^"]*"' | cut -d'"' -f4)
    LOCATION_NAME=$(echo "$STATUS_BODY2" | grep -o '"location_name":"[^"]*"' | cut -d'"' -f4)
    echo "   New location: $NEW_LOCATION"
    echo "   Location name: $LOCATION_NAME"
    
    # Check if movement was successful
    if [ "$NEW_LOCATION" = "cave_entrance" ]; then
        echo "   ✅ Movement successful!"
    else
        echo "   ❌ Movement failed - still at: $NEW_LOCATION"
    fi
else
    echo "   ❌ Failed to get updated status: $STATUS_BODY2"
fi
echo

# Test AI prompt to see if context is correct
echo "🤖 Checking AI context for correct location..."
AI_RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" "http://localhost:8080/api/ai/prompt?session_id=$SESSION_ID")
AI_HTTP_CODE=$(echo "$AI_RESPONSE" | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
AI_BODY=$(echo "$AI_RESPONSE" | sed 's/HTTP_CODE:[0-9]*$//')

if [ "$AI_HTTP_CODE" = "200" ]; then
    echo "Current location in AI context:"
    echo "$AI_BODY" | grep -A 3 -B 3 "Player Location" | head -10
else
    echo "   ❌ Failed to get AI context: $AI_BODY"
fi
echo

echo "🧪 Test complete!"
echo
echo "📋 Summary:"
echo "   Session ID: $SESSION_ID"
echo "   Initial Location: $CURRENT_LOCATION"
echo "   Final Location: $NEW_LOCATION"
echo "   Movement Success: $([ "$NEW_LOCATION" = "cave_entrance" ] && echo "✅ YES" || echo "❌ NO")"
echo
echo "📊 Manual commands for further testing:"
echo "   curl \"http://localhost:8080/api/game/status?session_id=$SESSION_ID\""
echo "   curl \"http://localhost:8080/api/ai/prompt?session_id=$SESSION_ID\""
echo
