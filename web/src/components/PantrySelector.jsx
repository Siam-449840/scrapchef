import React, { useState } from 'react';
import { MODELS } from '../lib/puter';

const COMMON_PANTRY = [
  'Egg', 'Rice', 'Onion', 'Garlic', 'Tomato', 'Potato',
  'Cheese', 'Pasta', 'Olive Oil', 'Carrot', 'Lentil', 'Spinach'
];

const COOKING_TOOLS = ['Stovetop', 'Oven', 'Microwave', 'Blender', 'Air Fryer', 'No-Cook'];

export default function PantrySelector({
  selectedIngredients,
  onToggleIngredient,
  onAddCustomIngredient,
  selectedTools,
  onToggleTool,
  maxTime,
  onChangeMaxTime,
  selectedModel,
  onChangeModel,
  onGenerateRecipe,
  isGenerating,
  generateError
}) {
  const [customInput, setCustomInput] = useState('');

  const handleAdd = (e) => {
    e.preventDefault();
    if (customInput.trim()) {
      onAddCustomIngredient(customInput.trim());
      setCustomInput('');
    }
  };

  return (
    <aside style={{
      width: '340px',
      borderRight: '1px solid var(--border-subtle)',
      padding: '24px',
      background: 'var(--bg-surface)',
      height: 'calc(100vh - 65px)',
      overflowY: 'auto',
      display: 'flex',
      flexDirection: 'column',
      gap: '24px'
    }}>
      {/* 1. Ingredients Section */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
          <h2 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Zero-Waste Pantry
          </h2>
          <span style={{
            fontSize: '0.75rem',
            padding: '2px 8px',
            borderRadius: '12px',
            background: selectedIngredients.length >= 3 ? 'rgba(16, 185, 129, 0.2)' : 'rgba(245, 158, 11, 0.2)',
            color: selectedIngredients.length >= 3 ? 'var(--accent-primary)' : 'var(--accent-amber)',
            fontWeight: 700
          }}>
            {selectedIngredients.length} selected (min 3)
          </span>
        </div>

        {/* Custom Ingredient Input */}
        <form onSubmit={handleAdd} style={{ display: 'flex', gap: '8px', marginBottom: '14px' }}>
          <input
            type="text"
            placeholder="Type ingredient..."
            value={customInput}
            onChange={(e) => setCustomInput(e.target.value)}
            style={{ flex: 1, fontSize: '0.85rem' }}
          />
          <button
            type="submit"
            style={{
              padding: '6px 14px',
              background: 'var(--bg-surface-elevated)',
              color: 'var(--text-primary)',
              border: '1px solid var(--border-subtle)'
            }}
          >
            Add
          </button>
        </form>

        {/* Quick select tags */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
          {COMMON_PANTRY.map((item) => {
            const isSelected = selectedIngredients.includes(item);
            return (
              <button
                key={item}
                type="button"
                onClick={() => onToggleIngredient(item)}
                style={{
                  padding: '5px 10px',
                  borderRadius: '20px',
                  fontSize: '0.8rem',
                  fontWeight: 500,
                  background: isSelected ? 'var(--accent-primary)' : 'var(--bg-surface-elevated)',
                  color: isSelected ? '#041e15' : 'var(--text-secondary)',
                  border: isSelected ? '1px solid var(--accent-primary)' : '1px solid var(--border-subtle)'
                }}
              >
                {isSelected ? '✓ ' : '+ '}{item}
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Cooking Tools Section */}
      <div>
        <h3 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '10px' }}>
          Cooking Equipment
        </h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
          {COOKING_TOOLS.map((tool) => {
            const isSelected = selectedTools.includes(tool);
            return (
              <button
                key={tool}
                type="button"
                onClick={() => onToggleTool(tool)}
                style={{
                  padding: '5px 10px',
                  borderRadius: '6px',
                  fontSize: '0.8rem',
                  background: isSelected ? 'rgba(99, 102, 241, 0.2)' : 'var(--bg-surface-elevated)',
                  color: isSelected ? '#a5b4fc' : 'var(--text-secondary)',
                  border: isSelected ? '1px solid var(--accent-indigo)' : '1px solid var(--border-subtle)'
                }}
              >
                {tool}
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. Max Time */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
          <label style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
            Max Prep Time
          </label>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)', fontWeight: 600 }}>
            {maxTime} mins
          </span>
        </div>
        <input
          type="range"
          min="10"
          max="90"
          step="5"
          value={maxTime}
          onChange={(e) => onChangeMaxTime(Number(e.target.value))}
          style={{ width: '100%', accentColor: 'var(--accent-primary)' }}
        />
      </div>

      {/* 4. AI Model Preference (Puter.kv) */}
      <div>
        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '8px' }}>
          Puter AI Engine (puter.kv)
        </label>
        <select
          value={selectedModel}
          onChange={(e) => onChangeModel(e.target.value)}
          style={{ width: '100%', fontSize: '0.85rem' }}
        >
          {MODELS.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
          Preference automatically saved to your Puter KV store.
        </span>
      </div>

      {/* 5. Error Banner */}
      {generateError && (
        <div style={{
          padding: '10px 12px',
          borderRadius: '6px',
          background: 'rgba(244, 63, 94, 0.1)',
          border: '1px solid rgba(244, 63, 94, 0.3)',
          color: 'var(--accent-coral)',
          fontSize: '0.8rem'
        }}>
          <strong>Generation Notice:</strong> {generateError}
        </div>
      )}

      {/* 6. Action Button */}
      <button
        onClick={onGenerateRecipe}
        disabled={isGenerating || selectedIngredients.length < 3}
        style={{
          marginTop: 'auto',
          padding: '12px 18px',
          background: selectedIngredients.length >= 3 && !isGenerating
            ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
            : 'var(--bg-surface-elevated)',
          color: selectedIngredients.length >= 3 && !isGenerating ? '#041e15' : 'var(--text-muted)',
          fontSize: '0.95rem',
          boxShadow: selectedIngredients.length >= 3 && !isGenerating ? 'var(--shadow-glow)' : 'none'
        }}
      >
        {isGenerating ? 'Streaming Recipe...' : 'Generate Recipe (User-Pays AI)'}
      </button>
    </aside>
  );
}
