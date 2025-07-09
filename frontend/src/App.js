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
const GameActions = ({ sessionId, onActionExecuted }) => {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [chat, setChat] = useState([
    { sender: 'system', text: 'What do you want to do?' }
  ]);

  const sendAction = async () => {
    if (!sessionId || !input.trim()) return;
    const userMsg = input.trim();
    setChat(prev => [...prev, { sender: 'user', text: userMsg }]);
    setInput('');
    setLoading(true);
    try {
      const response = await api.executeAction(sessionId, userMsg);
      if (response.success) {
        setChat(prev => [...prev, { sender: 'system', text: response.message }]);
        onActionExecuted();
      } else {
        setChat(prev => [...prev, { sender: 'system', text: `❌ ERROR: ${response.error}` }]);
      }
    } catch (error) {
      setChat(prev => [...prev, { sender: 'system', text: `🌐 Network error: ${error.message}` }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendAction();
    }
  };

  return (
    <div className="section chatbox-large">
      <h3>Game Actions</h3>
      <div className="chatbox-history">
        {chat.map((msg, idx) => (
          <div key={idx} className={`chat-msg ${msg.sender}`}>{msg.sender === 'user' ? 'You: ' : 'System: '}{msg.text}</div>
        ))}
      </div>
      <div className="chatbox-input-row">
        <textarea
          className="chatbox-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type your action and press Enter..."
          rows={2}
        />
        <button onClick={sendAction} disabled={loading || !input.trim()} className="chatbox-send">
          {loading ? 'Processing...' : 'Send'}
        </button>
      </div>
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

// ChatRPG Component
const ChatRPG = () => {
  const [sessionId, setSessionId] = useState('');
  const [chat, setChat] = useState([
    { sender: 'system', text: 'Welcome to AI-RPG! What is your hero name?' }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [awaitingHeroName, setAwaitingHeroName] = useState(true);

  // Helper to fetch and show adventure context
  const fetchAndShowAdventureContext = async (sid) => {
    try {
      const resp = await fetch(`/api/game/status?session_id=${sid}`);
      const data = await resp.json();
      if (data.success && data.context && data.context.adventure_context) {
        setChat(prev => [...prev, { sender: 'system', text: data.context.adventure_context }]);
      }
    } catch (e) {
      // Ignore context fetch errors
    }
  };

  const sendPrompt = async () => {
    const prompt = input.trim();
    if (!prompt) return;
    setChat(prev => [...prev, { sender: 'user', text: prompt }]);
    setInput('');
    setLoading(true);
    if (awaitingHeroName) {
      // Create session
      try {
        const data = await api.createSession(prompt);
        if (data.success) {
          setSessionId(data.session_id);
          setChat(prev => [...prev, { sender: 'system', text: `Welcome, ${prompt}! Your adventure begins. What do you want to do?` }]);
          setAwaitingHeroName(false);
          // Show initial adventure context
          fetchAndShowAdventureContext(data.session_id);
        } else {
          setChat(prev => [...prev, { sender: 'system', text: `❌ Error: ${data.error || 'Unknown error'}` }]);
        }
      } catch (error) {
        setChat(prev => [...prev, { sender: 'system', text: `🌐 Network error: ${error.message}` }]);
      } finally {
        setLoading(false);
      }
      return;
    }
    // Game action
    if (!sessionId) {
      setChat(prev => [...prev, { sender: 'system', text: 'Please enter your hero name to begin.' }]);
      setLoading(false);
      return;
    }
    try {
      const response = await api.executeAction(sessionId, prompt);
      if (response.success) {
        setChat(prev => [...prev, { sender: 'system', text: response.message }]);
        // Show updated adventure context
        fetchAndShowAdventureContext(sessionId);
      } else {
        setChat(prev => [...prev, { sender: 'system', text: `❌ ERROR: ${response.error}` }]);
      }
    } catch (error) {
      setChat(prev => [...prev, { sender: 'system', text: `🌐 Network error: ${error.message}` }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendPrompt();
    }
  };

  return (
    <div className="section chatbox-large">
      <div className="chatbox-history">
        {chat.map((msg, idx) => (
          <div key={idx} className={`chat-msg ${msg.sender}`}>{msg.sender === 'user' ? 'You: ' : 'System: '}{msg.text}</div>
        ))}
      </div>
      <div className="chatbox-input-row">
        <textarea
          className="chatbox-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={awaitingHeroName ? 'Enter your hero name...' : 'Type your action and press Enter...'}
          rows={2}
          disabled={loading}
        />
        <button onClick={sendPrompt} disabled={loading || !input.trim()} className="chatbox-send">
          {loading ? 'Processing...' : 'Send'}
        </button>
      </div>
    </div>
  );
};

// Main App Component
const App = () => {
  return (
    <div className="App">
      <div className="container">
        <ChatRPG />
      </div>
    </div>
  );
};

export default App;