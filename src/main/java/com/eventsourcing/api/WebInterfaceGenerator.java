package com.eventsourcing.api;

/**
 * Simple web interface generator for the AI-RPG platform.
 */
public class WebInterfaceGenerator {
    
    public static String generateHTML() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>🎮 AI-RPG Event Sourcing Platform</title>
                <style>
                    body { 
                        font-family: 'Segoe UI', sans-serif; 
                        margin: 0; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                        color: white; 
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container { max-width: 1200px; margin: 0 auto; }
                    .header {
                        text-align: center; 
                        margin-bottom: 40px; 
                        padding: 20px;
                        background: rgba(255,255,255,0.1); 
                        border-radius: 15px;
                        backdrop-filter: blur(10px);
                    }
                    .section { 
                        margin: 20px 0; 
                        padding: 25px; 
                        background: rgba(255,255,255,0.15); 
                        border-radius: 15px;
                        backdrop-filter: blur(10px);
                    }
                    button { 
                        padding: 12px 24px; 
                        margin: 8px; 
                        cursor: pointer; 
                        background: linear-gradient(45deg, #ff6b6b, #ee5a24);
                        color: white; 
                        border: none; 
                        border-radius: 8px; 
                        font-weight: bold;
                        transition: all 0.3s ease;
                    }
                    button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 15px rgba(0,0,0,0.3);
                    }
                    input { 
                        width: 300px; 
                        padding: 12px; 
                        margin: 8px; 
                        border: none; 
                        border-radius: 8px;
                        background: rgba(255,255,255,0.9);
                        color: #333;
                    }
                    .output { 
                        background: rgba(0,0,0,0.3); 
                        padding: 20px; 
                        border-radius: 8px; 
                        white-space: pre-wrap; 
                        font-family: 'Courier New', monospace;
                        border-left: 4px solid #00d4aa;
                        margin-top: 15px;
                        max-height: 400px;
                        overflow-y: auto;
                    }
                    .hidden { display: none; }
                    h1 { font-size: 2.5em; margin-bottom: 10px; }
                    h3 { color: #00d4aa; margin-top: 0; }
                    .grid { 
                        display: grid; 
                        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); 
                        gap: 20px; 
                    }
                    .map-container {
                        background: rgba(0,0,0,0.4);
                        border-radius: 12px;
                        padding: 15px;
                        border: 2px solid #00d4aa;
                        position: relative;
                        min-height: 200px;
                    }
                    .map-location {
                        display: inline-block;
                        background: rgba(255,255,255,0.1);
                        border: 1px solid #555;
                        border-radius: 8px;
                        padding: 8px 12px;
                        margin: 4px;
                        font-size: 0.9em;
                        transition: all 0.3s ease;
                    }
                    .map-location.current {
                        background: linear-gradient(45deg, #00d4aa, #00ff88);
                        color: #000;
                        font-weight: bold;
                        border-color: #00ff88;
                        transform: scale(1.1);
                        box-shadow: 0 0 15px rgba(0, 212, 170, 0.5);
                    }
                    .map-location.connected {
                        background: rgba(0, 212, 170, 0.2);
                        border-color: #00d4aa;
                    }
                    .current-location {
                        color: #00ff88;
                        font-weight: bold;
                        font-size: 1.1em;
                        margin-bottom: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎮 AI-RPG Event Sourcing Platform</h1>
                        <p>🤖 Claude AI Integration • 🌍 Persistent Worlds • ⚡ Real-time Event Sourcing</p>
                        <div id="aiStatus" style="margin-top: 10px; padding: 10px; border-radius: 8px; background: rgba(0,0,0,0.2);">
                            🔄 Checking AI status...
                        </div>
                    </div>
                    
                    <div class="grid">
                        <div class="section">
                            <h3>🌟 Create New Adventure</h3>
                            <input type="text" id="playerName" placeholder="Hero Name" value="Lyra the Mystic">
                            <br>
                            <button onclick="createSession()">🚀 Begin Journey</button>
                            <div id="sessionInfo" class="output hidden"></div>
                        </div>
                        
                        <div class="section">
                            <h3>⚔️ Game Actions</h3>
                            <input type="text" id="sessionId" placeholder="Session ID">
                            <br>
                            <input type="text" id="command" placeholder="Command" value="/look around">
                            <br>
                            <button onclick="setCommand('/look around')">👁️ Look</button>
                            <button onclick="setCommand('/talk tavern_keeper')">💬 Talk</button>
                            <button onclick="setCommand('/attack goblin')">⚔️ Fight</button>
                            <br>
                            <button onclick="setCommand('/go cave')">🕳️ Go Cave</button>
                            <button onclick="setCommand('/go village')">🏘️ Go Village</button>
                            <button onclick="setCommand('/go corridor')">🌑 Go Corridor</button>
                            <br>
                            <button onclick="executeAction()">🎯 Execute Action</button>
                            <div id="actionResult" class="output hidden"></div>
                        </div>
                        
                        <div class="section">
                            <h3>🗺️ Adventure Map</h3>
                            <div class="current-location" id="currentLocationText">📍 Current Location: Village</div>
                            <div class="map-container" id="adventureMap">
                                <div class="map-location current" data-location="village">🏘️ Village</div>
                                <div class="map-location" data-location="cave_entrance">🕳️ Cave Entrance</div>
                                <div class="map-location" data-location="first_corridor">🌑 Dark Corridor</div>
                                <div class="map-location" data-location="snake_chamber">🐍 Snake Chamber</div>
                                <div class="map-location" data-location="aleena_chamber">⛪ Aleenas Chamber</div>
                                <div class="map-location" data-location="ghoul_corridor">💀 Ghoul Corridor</div>
                                <div class="map-location" data-location="locked_door_area">🚪 Locked Door</div>
                                <div class="map-location" data-location="bargle_chamber">🧙‍♂️ Bargle's Lair</div>
                                <div class="map-location" data-location="exit_passage">🌅 Hidden Exit</div>
                            </div>
                            <button onclick="updateMap()">🔄 Update Map</button>
                        </div>
                        
                        <div class="section">
                            <h3>📊 World State & AI</h3>
                            <button onclick="getStatus()">🌍 World Status</button>
                            <button onclick="getAIPrompt()">🧠 AI Context</button>
                            <button onclick="getMetrics()">📈 Metrics</button>
                            <div id="contextResult" class="output hidden"></div>
                        </div>
                        
                        <div class="section">
                            <h3>🎭 Epic Adventure Demo</h3>
                            <button onclick="runEpicScenario()">🌟 Experience Full Adventure</button>
                            <div id="testResult" class="output hidden"></div>
                        </div>
                    </div>
                </div>

                <script>
                    let currentSessionId = '';

                    // Check AI status on page load
                    window.addEventListener('load', checkAIStatus);

                    async function checkAIStatus() {
                        try {
                            const response = await fetch('/api/metrics');
                            const data = await response.json();
                            const aiConfigured = data.context.ai.configured;
                            const aiProvider = data.context.ai.provider;
                            
                            const statusDiv = document.getElementById('aiStatus');
                            if (aiConfigured) {
                                statusDiv.innerHTML = '🤖 Claude AI: <span style="color: #00ff88;">ACTIVE</span> - Intelligent responses enabled';
                                statusDiv.style.background = 'rgba(0,255,136,0.2)';
                            } else {
                                statusDiv.innerHTML = '⚠️ Claude AI: <span style="color: #ffaa00;">SIMULATION MODE</span> - Add CLAUDE_API_KEY to .env for real AI';
                                statusDiv.style.background = 'rgba(255,170,0,0.2)';
                            }
                        } catch (error) {
                            document.getElementById('aiStatus').innerHTML = '❌ AI Status: Unknown';
                        }
                    }

                    function setCommand(cmd) {
                        document.getElementById('command').value = cmd;
                    }

                    async function createSession() {
                        const playerName = document.getElementById('playerName').value;
                        if (!playerName) {
                            alert('🚨 Please enter a hero name!');
                            return;
                        }

                        try {
                            const response = await fetch('/api/session/create', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    player_id: 'player_' + Date.now(),
                                    player_name: playerName
                                })
                            });

                            const data = await response.json();
                            if (data.success) {
                                currentSessionId = data.session_id;
                                document.getElementById('sessionId').value = currentSessionId;
                                document.getElementById('sessionInfo').innerText = 
                                    '🆔 Session: ' + data.session_id + '\\n\\n' +
                                    '🎬 ' + data.message;
                                show('sessionInfo');
                            } else {
                                alert('❌ Error: ' + (data.error || 'Unknown error'));
                            }
                        } catch (error) {
                            alert('🌐 Network error: ' + error.message);
                        }
                    }

                    async function executeAction() {
                        const sessionId = document.getElementById('sessionId').value;
                        const command = document.getElementById('command').value;
                        
                        if (!sessionId || !command) {
                            alert('🚨 Please enter session ID and command');
                            return;
                        }

                        try {
                            const response = await fetch('/api/game/action', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    session_id: sessionId,
                                    command: command
                                })
                            });

                            const data = await response.json();
                            if (data.success) {
                                document.getElementById('actionResult').innerText = 
                                    '🎭 AI RESPONSE:\\n' + data.message + '\\n\\n' +
                                    '📊 CONTEXT:\\n' + JSON.stringify(data.context, null, 2);
                            } else {
                                document.getElementById('actionResult').innerText = 
                                    '❌ ERROR: ' + data.error;
                            }
                            show('actionResult');
                        } catch (error) {
                            alert('🌐 Network error: ' + error.message);
                        }
                    }

                    async function getStatus() {
                        const sessionId = document.getElementById('sessionId').value || currentSessionId;
                        if (!sessionId) {
                            alert('🚨 Please enter session ID');
                            return;
                        }

                        try {
                            const response = await fetch('/api/game/status?session_id=' + sessionId);
                            const data = await response.json();
                            document.getElementById('contextResult').innerText = 
                                '🌍 WORLD STATE:\\n' + JSON.stringify(data.context, null, 2);
                            show('contextResult');
                        } catch (error) {
                            alert('🌐 Network error: ' + error.message);
                        }
                    }

                    async function getAIPrompt() {
                        const sessionId = document.getElementById('sessionId').value || currentSessionId;
                        if (!sessionId) {
                            alert('🚨 Please enter session ID');
                            return;
                        }

                        try {
                            const response = await fetch('/api/ai/prompt?session_id=' + sessionId);
                            const data = await response.json();
                            document.getElementById('contextResult').innerText = 
                                '🧠 AI PROMPT CONTEXT:\\n' + data.message;
                            show('contextResult');
                        } catch (error) {
                            alert('🌐 Network error: ' + error.message);
                        }
                    }

                    async function getMetrics() {
                        try {
                            const response = await fetch('/api/metrics');
                            const data = await response.json();
                            document.getElementById('contextResult').innerText = 
                                '📈 SYSTEM METRICS:\\n' + JSON.stringify(data.context, null, 2);
                            show('contextResult');
                        } catch (error) {
                            alert('🌐 Network error: ' + error.message);
                        }
                    }

                    async function runEpicScenario() {
                        const output = document.getElementById('testResult');
                        output.innerText = '🌟 Starting epic adventure scenario...\\n\\n';
                        show('testResult');

                        try {
                            // Create hero session
                            const sessionResp = await fetch('/api/session/create', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    player_id: 'epic_hero',
                                    player_name: 'Aria the Stormcaller'
                                })
                            });
                            const sessionData = await sessionResp.json();
                            const testSessionId = sessionData.session_id;
                            
                            output.innerText += '🦸‍♀️ Created hero: Aria the Stormcaller\\n';
                            output.innerText += '🆔 Session: ' + testSessionId + '\\n\\n';

                            // Epic adventure sequence
                            const epicActions = [
                                { cmd: '/look around', desc: '👁️ Surveying the mystical realm' },
                                { cmd: '/talk tavern_keeper', desc: '💬 Meeting the wise tavern keeper' },
                                { cmd: '/attack goblin', desc: '⚔️ Vanquishing a menacing goblin' }
                            ];

                            for (const action of epicActions) {
                                output.innerText += action.desc + '...\\n';
                                
                                const actionResp = await fetch('/api/game/action', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        session_id: testSessionId,
                                        command: action.cmd
                                    })
                                });
                                const actionData = await actionResp.json();
                                
                                if (actionData.success) {
                                    output.innerText += '🎭 ' + actionData.message.substring(0, 100) + '...\\n\\n';
                                }
                                
                                // Small delay for dramatic effect
                                await new Promise(resolve => setTimeout(resolve, 800));
                            }

                            // Get final AI prompt
                            const promptResp = await fetch('/api/ai/prompt?session_id=' + testSessionId);
                            const promptData = await promptResp.json();
                            
                            output.innerText += '\\n🧠 === FINAL AI CONTEXT ===\\n';
                            output.innerText += promptData.message;
                            
                            output.innerText += '\\n\\n🎉 Epic adventure completed! The autonomous AI agents have full context of your journey.';

                        } catch (error) {
                            output.innerText += '❌ Adventure interrupted: ' + error.message;
                        }
                    }

                    // Map functionality
                    const locationMap = {
                        'village': '🏘️ Your Home Village',
                        'cave_entrance': '🕳️ Cave Entrance', 
                        'first_corridor': '🌑 Dark Corridor',
                        'snake_chamber': '🐍 Snake Chamber',
                        'aleena_chamber': '⛪ Aleena\'s Chamber',
                        'ghoul_corridor': '💀 Ghoul Corridor', 
                        'locked_door_area': '🚪 Locked Door',
                        'bargle_chamber': '🧙‍♂️ Bargle\'s Lair',
                        'exit_passage': '🌅 Hidden Exit'
                    };
                    
                    const locationConnections = {
                        'village': ['cave_entrance'],
                        'cave_entrance': ['village', 'first_corridor'],
                        'first_corridor': ['cave_entrance', 'snake_chamber'],
                        'snake_chamber': ['first_corridor', 'aleena_chamber'],
                        'aleena_chamber': ['snake_chamber', 'ghoul_corridor'],
                        'ghoul_corridor': ['aleena_chamber', 'locked_door_area'],
                        'locked_door_area': ['ghoul_corridor', 'bargle_chamber'],
                        'bargle_chamber': ['locked_door_area', 'exit_passage'],
                        'exit_passage': ['bargle_chamber', 'village']
                    };
                    
                    function updateMapDisplay(currentLocation) {
                        const mapContainer = document.getElementById('adventureMap');
                        const currentLocationText = document.getElementById('currentLocationText');
                        
                        // Update current location text
                        const locationName = locationMap[currentLocation] || locationMap['village'];
                        currentLocationText.textContent = `📍 Current Location: ${locationName}`;
                        
                        // Update map locations
                        const locations = mapContainer.querySelectorAll('.map-location');
                        locations.forEach(location => {
                            const locationId = location.getAttribute('data-location');
                            location.classList.remove('current', 'connected');
                            
                            if (locationId === currentLocation) {
                                location.classList.add('current');
                            } else if (locationConnections[currentLocation] && 
                                      locationConnections[currentLocation].includes(locationId)) {
                                location.classList.add('connected');
                            }
                        });
                    }
                    
                    async function updateMap() {
                        const sessionId = document.getElementById('sessionId').value || currentSessionId;
                        if (!sessionId) {
                            alert('🚨 Please enter session ID or create a session first');
                            return;
                        }
                        
                        try {
                            const response = await fetch('/api/game/status?session_id=' + sessionId);
                            const data = await response.json();
                            
                            if (data.success && data.context.current_location) {
                                updateMapDisplay(data.context.current_location);
                            } else {
                                // Default to village if no location found
                                updateMapDisplay('village');
                            }
                        } catch (error) {
                            console.error('Map update error:', error);
                            updateMapDisplay('village'); // Fallback to village
                        }
                    }
                    
                    // Auto-update map after actions
                    const originalExecuteAction = executeAction;
                    executeAction = async function() {
                        await originalExecuteAction();
                        setTimeout(updateMap, 500); // Update map after action
                    };
                    
                    const originalCreateSession = createSession;
                    createSession = async function() {
                        await originalCreateSession();
                        setTimeout(() => updateMapDisplay('village'), 500); // Start at village
                    };
                    
                    function show(elementId) {
                        document.getElementById(elementId).classList.remove('hidden');
                    }
                </script>
            </body>
            </html>
            """;
    }
}
