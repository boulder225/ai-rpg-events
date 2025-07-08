import React, { useState, useEffect } from 'react';
import './App.css';

// API service
const api = {
  async createSession(playerName) {
    const response = await fetch('/api/session/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        player_id: 'player_' + Date.now(),
        player_name: playerName
      })
    });
    return response.json();
  },

  async executeAction(sessionId, command) {
    const response = await fetch('/api/game/action', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ session_id: sessionId, command })
    });
    return response.json();
  },

  async getStatus(sessionId) {
    const response = await fetch(`/api/game/status?session_id=${sessionId}`);
    return response.json();
  },

  async getMetrics() {
    const response = await fetch('/api/metrics');
    return response.json();
  }
};

// Adventure Map Component
const AdventureMap = ({ currentLocation, sessionId, onLocationUpdate }) => {
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

  const updateMap = async () => {
    if (!sessionId) return;
    try {
      const data = await api.getStatus(sessionId);
      if (data.success && data.context.current_location) {
        onLocationUpdate(data.context.current_location);
      }
    } catch (error) {
      console.error('Map update error:', error);
    }
  };

  const getLocationClass = (locationId) => {
    let className = 'map-location';
    if (locationId === currentLocation) {
      className += ' current';
    } else if (locationConnections[currentLocation]?.includes(locationId)) {
      className += ' connected';
    }
    return className;
  };

  return (
    <div className="section">
      <h3>🗺️ Adventure Map</h3>
      <div className="current-location">
        📍 Current Location: {locationMap[currentLocation] || locationMap['village']}
      </div>
      <div className="map-container">
        {Object.entries(locationMap).map(([locationId, name]) => (
          <div
            key={locationId}
            className={getLocationClass(locationId)}
            data-location={locationId}
          >
            {name}
          </div>
        ))}
      </div>
      <button onClick={updateMap}>🔄 Update Map</button>
    </div>
  );
};

// Game Actions Component
const GameActions = ({ sessionId, onActionExecuted }) => {
  const [command, setCommand] = useState('/look around');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  const executeAction = async () => {
    if (!sessionId || !command) {
      alert('🚨 Please enter session ID and command');
      return;
    }

    setLoading(true);
    try {
      const data = await api.executeAction(sessionId, command);
      if (data.success) {
        setResult(`🎭 AI RESPONSE:\n${data.message}\n\n📊 CONTEXT:\n${JSON.stringify(data.context, null, 2)}`);
        onActionExecuted();
      } else {
        setResult(`❌ ERROR: ${data.error}`);
      }
    } catch (error) {
      alert('🌐 Network error: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const quickCommands = [
    { label: '👁️ Look', cmd: '/look around' },
    { label: '💬 Talk', cmd: '/talk tavern_keeper' },
    { label: '⚔️ Fight', cmd: '/attack goblin' },
    { label: '🕳️ Go Cave', cmd: '/go cave' },
    { label: '🏘️ Go Village', cmd: '/go village' },
    { label: '🌑 Go Corridor', cmd: '/go corridor' }
  ];

  return (
    <div className="section">
      <h3>⚔️ Game Actions</h3>
      <input
        type="text"
        value={command}
        onChange={(e) => setCommand(e.target.value)}
        placeholder="Command"
      />
      <br />
      {quickCommands.map((cmd, index) => (
        <button key={index} onClick={() => setCommand(cmd.cmd)}>
          {cmd.label}
        </button>
      ))}
      <br />
      <button onClick={executeAction} disabled={loading}>
        {loading ? '⏳ Processing...' : '🎯 Execute Action'}
      </button>
      {result && <div className="output">{result}</div>}
    </div>
  );
};

// Session Management Component
const SessionManager = ({ onSessionCreated }) => {
  const [playerName, setPlayerName] = useState('Lyra the Mystic');
  const [sessionInfo, setSessionInfo] = useState('');
  const [loading, setLoading] = useState(false);

  const createSession = async () => {
    if (!playerName) {
      alert('🚨 Please enter a hero name!');
      return;
    }

    setLoading(true);
    try {
      const data = await api.createSession(playerName);
      if (data.success) {
        setSessionInfo(`🆔 Session: ${data.session_id}\n\n🎬 ${data.message}`);
        onSessionCreated(data.session_id);
      } else {
        alert('❌ Error: ' + (data.error || 'Unknown error'));
      }
    } catch (error) {
      alert('🌐 Network error: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="section">
      <h3>🌟 Create New Adventure</h3>
      <input
        type="text"
        value={playerName}
        onChange={(e) => setPlayerName(e.target.value)}
        placeholder="Hero Name"
      />
      <br />
      <button onClick={createSession} disabled={loading}>
        {loading ? '⏳ Creating...' : '🚀 Begin Journey'}
      </button>
      {sessionInfo && <div className="output">{sessionInfo}</div>}
    </div>
  );
};

// AI Status Component
const AIStatus = () => {
  const [status, setStatus] = useState('🔄 Checking AI status...');
  const [statusClass, setStatusClass] = useState('');

  useEffect(() => {
    const checkAIStatus = async () => {
      try {
        const data = await api.getMetrics();
        const aiConfigured = data.context.ai.configured;
        
        if (aiConfigured) {
          setStatus('🤖 Claude AI: ACTIVE - Intelligent responses enabled');
          setStatusClass('ai-active');
        } else {
          setStatus('⚠️ Claude AI: SIMULATION MODE - Add CLAUDE_API_KEY to .env for real AI');
          setStatusClass('ai-simulation');
        }
      } catch (error) {
        setStatus('❌ AI Status: Unknown');
        setStatusClass('ai-error');
      }
    };

    checkAIStatus();
  }, []);

  return (
    <div className={`ai-status ${statusClass}`}>
      {status}
    </div>
  );
};

// Main App Component
const App = () => {
  const [sessionId, setSessionId] = useState('');
  const [currentLocation, setCurrentLocation] = useState('village');

  const handleSessionCreated = (newSessionId) => {
    setSessionId(newSessionId);
    setCurrentLocation('village');
  };

  const handleActionExecuted = () => {
    // Auto-update map after actions
    setTimeout(async () => {
      if (sessionId) {
        try {
          const data = await api.getStatus(sessionId);
          if (data.success && data.context.current_location) {
            setCurrentLocation(data.context.current_location);
          }
        } catch (error) {
          console.error('Auto-update error:', error);
        }
      }
    }, 500);
  };

  return (
    <div className="App">
      <div className="container">
        <div className="header">
          <h1>🎮 AI-RPG Event Sourcing Platform</h1>
          <p>🤖 Claude AI Integration • 🌍 Persistent Worlds • ⚡ Real-time Event Sourcing</p>
          <AIStatus />
        </div>
        
        <div className="grid">
          <SessionManager onSessionCreated={handleSessionCreated} />
          
          <GameActions 
            sessionId={sessionId}
            onActionExecuted={handleActionExecuted}
          />
          
          <AdventureMap 
            currentLocation={currentLocation}
            sessionId={sessionId}
            onLocationUpdate={setCurrentLocation}
          />
        </div>
      </div>
    </div>
  );
};

export default App;