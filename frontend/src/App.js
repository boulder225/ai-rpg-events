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
  },

  async getMetadata() {
    const response = await fetch('/api/game/metadata');
    return response.json();
  }
};

// Adventure Map Component
const AdventureMap = ({ currentLocation, sessionId, onLocationUpdate, locations }) => {
  // locations: { id: LocationData }
  const locationMap = locations || {};
  const locationIds = Object.keys(locationMap);

  // Build connections map from locations
  const locationConnections = {};
  locationIds.forEach(id => {
    locationConnections[id] = locationMap[id]?.connections || [];
  });

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
        📍 Current Location: {locationMap[currentLocation]?.name || 'Unknown'}
      </div>
      <div className="map-container">
        {locationIds.map(locationId => (
          <div
            key={locationId}
            className={getLocationClass(locationId)}
            data-location={locationId}
          >
            {locationMap[locationId]?.icon} {locationMap[locationId]?.name}
          </div>
        ))}
      </div>
      <button onClick={updateMap}>🔄 Update Map</button>
    </div>
  );
};

// Game Actions Component
const GameActions = ({ sessionId, onActionExecuted, quickCommands }) => {
  const [command, setCommand] = useState('');
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
      {quickCommands && quickCommands.map((cmd, index) => (
        <button key={index} onClick={() => setCommand(cmd.command)}>
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
const SessionManager = ({ onSessionCreated, defaultPlayerName }) => {
  const [playerName, setPlayerName] = useState(defaultPlayerName || '');
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
  const [currentLocation, setCurrentLocation] = useState('');
  const [metadata, setMetadata] = useState(null);

  useEffect(() => {
    // Fetch game metadata on mount
    api.getMetadata().then(setMetadata);
  }, []);

  useEffect(() => {
    // Set starting location from metadata when session is created
    if (metadata && !sessionId) {
      setCurrentLocation(metadata.startingLocation || '');
    }
  }, [metadata, sessionId]);

  const handleSessionCreated = (newSessionId) => {
    setSessionId(newSessionId);
    setCurrentLocation(metadata?.startingLocation || '');
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

  if (!metadata) {
    return <div className="App"><div className="container"><div>Loading game system...</div></div></div>;
  }

  return (
    <div className="App">
      <div className="container">
        <div className="header">
          <h1>{metadata.systemName || 'AI-RPG Event Sourcing Platform'}</h1>
          <p>{metadata.description || 'AI Integration • Persistent Worlds • Event Sourcing'}</p>
          <AIStatus />
        </div>
        <div className="grid">
          <SessionManager onSessionCreated={handleSessionCreated} defaultPlayerName="Lyra the Mystic" />
          <GameActions 
            sessionId={sessionId}
            onActionExecuted={handleActionExecuted}
            quickCommands={metadata.quickCommands}
          />
          <AdventureMap 
            currentLocation={currentLocation}
            sessionId={sessionId}
            onLocationUpdate={setCurrentLocation}
            locations={metadata.locations}
          />
        </div>
      </div>
    </div>
  );
};

export default App;