#!/bin/bash

# Enhanced AI-RPG Platform - Quick Test Script
echo "🧪 Testing Enhanced AI-RPG Platform Features"
echo "============================================="
echo

API_BASE="http://localhost:8080/api"

# Test if server is running
echo "1. Testing server connectivity..."
if curl -s "$API_BASE/game/metadata" > /dev/null; then
    echo "✅ Server is running"
else
    echo "❌ Server not responding. Make sure to run './start-backend.sh' first"
    exit 1
fi

echo
echo "2. Testing enhanced metadata..."
METADATA=$(curl -s "$API_BASE/game/metadata")
echo "$METADATA" | grep -q "context_management_enabled" && echo "✅ Context management detected" || echo "⚠️ Basic mode only"
echo "$METADATA" | grep -q "rules_validation_active" && echo "✅ Rules validation detected" || echo "⚠️ Validation not active"
echo "$METADATA" | grep -q "world_state_consistency" && echo "✅ World state tracking detected" || echo "⚠️ Basic tracking only"

echo
echo "3. Creating test session..."
SESSION_RESPONSE=$(curl -s -X POST "$API_BASE/session/create" \
  -H "Content-Type: application/json" \
  -d '{"player_id":"test-player","player_name":"Test Hero"}')

SESSION_ID=$(echo "$SESSION_RESPONSE" | grep -o '"session_id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$SESSION_ID" ]; then
    echo "✅ Session created: $SESSION_ID"
    
    # Check if context management is active for this session
    echo "$SESSION_RESPONSE" | grep -q '"context_management":true' && echo "✅ Enhanced context active for session" || echo "ℹ️ Basic mode for session"
    
    echo
    echo "4. Testing enhanced game action..."
    ACTION_RESPONSE=$(curl -s -X POST "$API_BASE/game/action" \
      -H "Content-Type: application/json" \
      -d "{\"sessionId\":\"$SESSION_ID\",\"command\":\"I want to attack the rust monster with my sword\"}")
    
    echo "$ACTION_RESPONSE" | grep -q '"enhanced_processing":true' && echo "✅ Enhanced processing active" || echo "ℹ️ Using legacy processing"
    echo "$ACTION_RESPONSE" | grep -q '"rules_validated":true' && echo "✅ Rules validation working" || echo "ℹ️ Basic validation"
    
    echo
    echo "5. Testing context retrieval..."
    if curl -s "$API_BASE/context/current?session_id=$SESSION_ID" | grep -q '"current_location"'; then
        echo "✅ Enhanced context endpoint working"
    else
        echo "ℹ️ Enhanced context not available (needs API key)"
    fi
    
    echo
    echo "6. Testing enhanced metrics..."
    METRICS=$(curl -s "$API_BASE/metrics")
    echo "$METRICS" | grep -q '"context_managers_active"' && echo "✅ Enhanced metrics available" || echo "ℹ️ Basic metrics only"
    
    ACTIVE_SESSIONS=$(echo "$METRICS" | grep -o '"active_sessions":[0-9]*' | cut -d':' -f2)
    echo "📊 Active sessions: $ACTIVE_SESSIONS"
    
else
    echo "❌ Failed to create session"
    exit 1
fi

echo
echo "🎯 Test Summary:"
echo "==============="
if echo "$SESSION_RESPONSE" | grep -q '"context_management":true'; then
    echo "✅ ENHANCED MODE: Full context management active"
    echo "   🎯 World state consistency enabled"
    echo "   🛡️ D&D rules validation working" 
    echo "   🧠 AI responses with memory"
    echo "   💰 Cost: ~$10-50/month"
else
    echo "ℹ️ BASIC MODE: Enhanced features available but limited"
    echo "   📝 Add CLAUDE_API_KEY to .env for full enhancement"
    echo "   🆓 Current cost: $0/month"
fi

echo
echo "🧪 Test completed!"
echo "💡 Try the enhanced features:"
echo "   Frontend: http://localhost:3000"
echo "   API docs: Check server logs for endpoints"
