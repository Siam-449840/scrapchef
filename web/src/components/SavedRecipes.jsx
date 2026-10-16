import React from 'react';

export default function SavedRecipes({
  recipes,
  activeId,
  onSelectRecipe,
  onDeleteRecipe,
  isOpen,
  onToggleOpen
}) {
  return (
    <div style={{
      width: isOpen ? '280px' : '0px',
      transition: 'width 0.3s ease',
      borderLeft: isOpen ? '1px solid var(--border-subtle)' : 'none',
      background: 'var(--bg-surface)',
      height: 'calc(100vh - 65px)',
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <div style={{
        padding: '18px',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#fff', textTransform: 'uppercase' }}>
          ☁ Cloud Recipes ({recipes.length})
        </h3>
        <button
          onClick={onToggleOpen}
          style={{
            background: 'transparent',
            color: 'var(--text-muted)',
            fontSize: '1rem',
            padding: '2px 6px'
          }}
        >
          ✕
        </button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {recipes.length === 0 ? (
          <div style={{ padding: '24px 12px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            No saved cloud recipes yet. Generate a recipe and tap "Save to Puter Cloud".
          </div>
        ) : (
          recipes.map((item) => {
            const isSelected = item.id === activeId;
            return (
              <div
                key={item.id}
                onClick={() => onSelectRecipe(item)}
                style={{
                  padding: '12px',
                  borderRadius: 'var(--radius-sm)',
                  background: isSelected ? 'var(--bg-surface-elevated)' : 'var(--bg-main)',
                  border: isSelected ? '1px solid var(--accent-primary)' : '1px solid var(--border-subtle)',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ flex: 1, minWidth: 0, paddingRight: '8px' }}>
                  <div style={{
                    fontWeight: 600,
                    fontSize: '0.85rem',
                    color: isSelected ? 'var(--accent-primary)' : 'var(--text-primary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}>
                    {item.title || item.name || 'Untitled Recipe'}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    {item.updatedAt ? new Date(item.updatedAt).toLocaleDateString() : 'Saved'}
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteRecipe(item.id);
                  }}
                  title="Delete from Puter cloud storage"
                  style={{
                    background: 'transparent',
                    color: 'var(--text-muted)',
                    padding: '4px',
                    borderRadius: '4px',
                    fontSize: '0.8rem'
                  }}
                  onMouseEnter={(e) => { e.target.style.color = 'var(--accent-coral)'; }}
                  onMouseLeave={(e) => { e.target.style.color = 'var(--text-muted)'; }}
                >
                  🗑
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
