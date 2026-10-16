import React from 'react';

export default function RecipeView({
  activeRecipe,
  streamingText,
  isGenerating,
  onSaveToCloud,
  isSaving,
  user
}) {
  if (isGenerating) {
    return (
      <main style={{
        flex: 1,
        padding: '32px',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          borderRadius: 'var(--radius-md)',
          background: 'rgba(16, 185, 129, 0.08)',
          border: '1px solid rgba(16, 185, 129, 0.2)'
        }}>
          <div style={{
            width: '12px',
            height: '12px',
            borderRadius: '50%',
            background: 'var(--accent-primary)',
            boxShadow: '0 0 10px var(--accent-primary)',
            animation: 'pulse 1.5s infinite'
          }}></div>
          <div>
            <h3 style={{ fontSize: '1rem', color: 'var(--accent-primary)', fontWeight: 700 }}>
              Puter.ai Streaming in Progress...
            </h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Synthesizing zero-waste recipe using real-time LLM token stream.
            </p>
          </div>
        </div>

        {/* Live Streaming Terminal / Preview */}
        <div style={{
          flex: 1,
          padding: '20px',
          borderRadius: 'var(--radius-md)',
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-subtle)',
          fontFamily: 'var(--font-mono)',
          fontSize: '0.85rem',
          color: '#34d399',
          whiteSpace: 'pre-wrap',
          overflowY: 'auto',
          lineHeight: 1.6
        }}>
          {streamingText || 'Waiting for first token from Puter AI...'}
        </div>
      </main>
    );
  }

  if (!activeRecipe) {
    return (
      <main style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        color: 'var(--text-muted)',
        padding: '32px',
        textAlign: 'center',
        gap: '12px'
      }}>
        <div style={{ fontSize: '48px' }}>🥦</div>
        <h2 style={{ fontSize: '1.4rem', color: 'var(--text-secondary)', fontWeight: 700 }}>
          Your Zero-Waste Culinary Canvas
        </h2>
        <p style={{ maxWidth: '440px', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
          Select 3 or more ingredients from your pantry on the left and tap "Generate Recipe".
          Puter.ai will craft a customized recipe with zero food waste.
        </p>
      </main>
    );
  }

  return (
    <main style={{
      flex: 1,
      padding: '32px 40px',
      overflowY: 'auto',
      display: 'flex',
      flexDirection: 'column',
      gap: '28px'
    }}>
      {/* Recipe Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        borderBottom: '1px solid var(--border-subtle)',
        paddingBottom: '20px'
      }}>
        <div>
          <span style={{
            fontSize: '0.75rem',
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'var(--accent-primary)',
            fontWeight: 700
          }}>
            Zero-Waste Recipe
          </span>
          <h2 style={{ fontSize: '1.9rem', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
            {activeRecipe.name || 'Custom Recipe'}
          </h2>
          <div style={{ display: 'flex', gap: '16px', marginTop: '8px', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            <span>⏱ Prep: <strong>{activeRecipe.prep_time_minutes || 15}m</strong></span>
            <span>🔥 Cook: <strong>{activeRecipe.cook_time_minutes || 20}m</strong></span>
            <span>🍽 Servings: <strong>{activeRecipe.servings || 2}</strong></span>
          </div>
        </div>

        {user && onSaveToCloud && (
          <button
            onClick={onSaveToCloud}
            disabled={isSaving}
            style={{
              padding: '8px 16px',
              background: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--accent-primary)',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            {isSaving ? 'Saving to Puter FS...' : '☁ Save to Puter Cloud'}
          </button>
        )}
      </div>

      {/* Description / Zero Waste Tip */}
      {activeRecipe.description && (
        <div style={{
          padding: '16px 20px',
          borderRadius: 'var(--radius-md)',
          background: 'var(--bg-surface)',
          borderLeft: '4px solid var(--accent-primary)',
          fontSize: '0.95rem',
          color: 'var(--text-primary)'
        }}>
          {activeRecipe.description}
        </div>
      )}

      {/* Ingredients List */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#fff', marginBottom: '14px' }}>
          Required Ingredients
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '10px' }}>
          {(activeRecipe.ingredients || []).map((ing, idx) => (
            <div
              key={idx}
              style={{
                padding: '10px 14px',
                borderRadius: 'var(--radius-sm)',
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)',
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.85rem'
              }}
            >
              <strong style={{ color: 'var(--text-primary)' }}>{typeof ing === 'string' ? ing : ing.name}</strong>
              <span style={{ color: 'var(--accent-primary)' }}>{typeof ing === 'object' ? `${ing.amount || ''} ${ing.unit || ''}` : ''}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Instructions */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#fff', marginBottom: '14px' }}>
          Preparation Steps
        </h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {(activeRecipe.instructions || []).map((step, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                gap: '16px',
                padding: '14px 18px',
                borderRadius: 'var(--radius-sm)',
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)'
              }}
            >
              <div style={{
                width: '26px',
                height: '26px',
                borderRadius: '50%',
                background: 'rgba(16, 185, 129, 0.15)',
                color: 'var(--accent-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '0.8rem',
                flexShrink: 0
              }}>
                {idx + 1}
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)', lineHeight: 1.6 }}>
                {typeof step === 'string' ? step : step.instruction || step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}
