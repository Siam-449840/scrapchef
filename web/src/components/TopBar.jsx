import React from 'react';

export default function TopBar({ user, onSignIn, onSignOut, globalRecipes, authError, isSigningIn }) {
  return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '14px 24px',
      background: 'rgba(20, 24, 32, 0.85)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid var(--border-subtle)',
      position: 'sticky',
      top: 0,
      zIndex: 50
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '18px',
            boxShadow: 'var(--shadow-glow)'
          }}>
            🍳
          </div>
          <div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#fff', lineHeight: 1 }}>
              ScrapChef
            </h1>
            <span style={{ fontSize: '0.7rem', color: 'var(--accent-primary)', fontWeight: 600 }}>
              Zero-Waste AI Engine (Puter.js)
            </span>
          </div>
        </div>

        {globalRecipes !== null && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: '4px 10px',
            borderRadius: '20px',
            background: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            fontSize: '0.75rem',
            color: 'var(--accent-primary)'
          }}>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--accent-primary)' }}></span>
            <span>Global Recipes: <strong>{globalRecipes}</strong></span>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        {authError && (
          <span style={{
            fontSize: '0.8rem',
            color: 'var(--accent-coral)',
            background: 'rgba(244, 63, 94, 0.1)',
            padding: '4px 8px',
            borderRadius: '4px',
            border: '1px solid rgba(244, 63, 94, 0.2)'
          }}>
            {authError}
          </span>
        )}

        {user ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '6px 12px',
              borderRadius: '20px',
              background: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
              fontSize: '0.85rem'
            }}>
              <span style={{ color: 'var(--text-secondary)' }}>User:</span>
              <strong style={{ color: 'var(--text-primary)' }}>{user.username}</strong>
            </div>
            <button
              onClick={onSignOut}
              style={{
                padding: '6px 14px',
                background: 'transparent',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-secondary)'
              }}
              onMouseEnter={(e) => { e.target.style.borderColor = 'var(--accent-coral)'; e.target.style.color = 'var(--accent-coral)'; }}
              onMouseLeave={(e) => { e.target.style.borderColor = 'var(--border-subtle)'; e.target.style.color = 'var(--text-secondary)'; }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          <button
            onClick={onSignIn}
            disabled={isSigningIn}
            style={{
              padding: '8px 18px',
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              color: '#041e15',
              boxShadow: 'var(--shadow-glow)'
            }}
          >
            {isSigningIn ? 'Connecting...' : 'Sign in with Puter'}
          </button>
        )}
      </div>
    </header>
  );
}
