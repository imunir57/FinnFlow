// FinnFlow — paper-ledger Android frame
// Custom frame: warm off-white, top bar with avatar + 3-dot, gesture nav
// Accepts a topBar node (so screens can customize it) and optional bottomNav

function StatusBar({ dark = false }) {
  const c = dark ? '#f5f1ea' : '#2a2621';
  return (
    <div style={{
      height: 34, display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', padding: '0 22px',
      position: 'relative', flexShrink: 0,
      fontFamily: 'Geist, system-ui, sans-serif',
      fontVariantNumeric: 'tabular-nums',
    }}>
      <span style={{ fontSize: 13, fontWeight: 500, color: c, letterSpacing: 0.2 }}>9:30</span>
      <div style={{
        position: 'absolute', left: '50%', top: 6, transform: 'translateX(-50%)',
        width: 18, height: 18, borderRadius: '50%', background: '#1a1814',
      }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
        {/* signal */}
        <svg width="14" height="14" viewBox="0 0 14 14">
          <rect x="1" y="9" width="2" height="4" rx="0.5" fill={c}/>
          <rect x="5" y="6" width="2" height="7" rx="0.5" fill={c}/>
          <rect x="9" y="2" width="2" height="11" rx="0.5" fill={c}/>
        </svg>
        {/* wifi */}
        <svg width="14" height="14" viewBox="0 0 14 14">
          <path d="M7 11 L7 11" stroke={c} strokeWidth="2" strokeLinecap="round"/>
          <path d="M4 8 Q7 5 10 8" stroke={c} strokeWidth="1.3" fill="none" strokeLinecap="round"/>
          <path d="M2 6 Q7 1 12 6" stroke={c} strokeWidth="1.3" fill="none" strokeLinecap="round"/>
        </svg>
        {/* battery */}
        <svg width="22" height="12" viewBox="0 0 22 12">
          <rect x="0.5" y="1.5" width="18" height="9" rx="2" fill="none" stroke={c} strokeWidth="1"/>
          <rect x="20" y="4" width="1.5" height="4" rx="0.5" fill={c}/>
          <rect x="2" y="3" width="12" height="6" rx="1" fill={c}/>
        </svg>
      </div>
    </div>
  );
}

function GestureBar({ dark = false }) {
  return (
    <div style={{
      height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center',
      flexShrink: 0, background: 'transparent',
    }}>
      <div style={{
        width: 128, height: 4, borderRadius: 2,
        background: dark ? 'rgba(245,241,234,0.7)' : 'rgba(42,38,33,0.55)',
      }} />
    </div>
  );
}

// Main bottom nav — 4 tabs
function BottomNav({ current, onChange }) {
  const items = [
    { id: 'home',  label: 'Home',  icon: Ic.home },
    { id: 'stats', label: 'Stats', icon: Ic.chart },
    { id: 'yearly',label: 'Yearly',icon: Ic.calendar },
    { id: 'settings',label: 'Settings',icon: Ic.gear },
  ];
  return (
    <div style={{
      height: 68, flexShrink: 0,
      background: 'var(--paper)',
      borderTop: '1px solid var(--rule)',
      display: 'flex', alignItems: 'stretch',
      paddingBottom: 4,
    }}>
      {items.map(it => {
        const active = it.id === current;
        const I = it.icon;
        return (
          <button key={it.id} onClick={() => onChange?.(it.id)}
            style={{
              flex: 1, background: 'none', border: 0, cursor: 'pointer',
              display: 'flex', flexDirection: 'column', alignItems: 'center',
              justifyContent: 'center', gap: 3,
              color: active ? 'var(--ink)' : 'var(--ink-3)',
              fontFamily: 'Geist, system-ui, sans-serif',
              padding: 0,
            }}>
            <div style={{
              width: 52, height: 28, borderRadius: 14,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: active ? 'var(--ink-wash)' : 'transparent',
              transition: 'background 120ms ease',
            }}>
              <I size={20} />
            </div>
            <span style={{
              fontSize: 11, fontWeight: active ? 600 : 500,
              letterSpacing: 0.1,
            }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// FAB
function FAB({ onClick }) {
  return (
    <button onClick={onClick} style={{
      position: 'absolute', right: 20, bottom: 92,
      width: 60, height: 60, borderRadius: 20,
      background: 'var(--ink)', color: 'var(--paper)',
      border: 0, cursor: 'pointer',
      boxShadow: '0 10px 28px rgba(42,38,33,0.28), 0 2px 6px rgba(42,38,33,0.15)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 10,
    }}>
      <Ic.plus size={24} />
    </button>
  );
}

// Device shell
function Device({ children, width = 390, height = 844, showNav = true, fab, onFab, currentTab, onTabChange, customBottom }) {
  return (
    <div style={{
      width, height, borderRadius: 44,
      background: '#1a1814',
      padding: 8,
      boxSizing: 'border-box',
      boxShadow: '0 40px 90px rgba(42,38,33,0.22), 0 8px 24px rgba(42,38,33,0.14)',
      position: 'relative',
    }}>
      <div style={{
        width: '100%', height: '100%', borderRadius: 36, overflow: 'hidden',
        background: 'var(--paper)',
        display: 'flex', flexDirection: 'column',
        position: 'relative',
      }}>
        <StatusBar />
        <div style={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          {children}
          {fab && <FAB onClick={onFab} />}
        </div>
        {customBottom ? customBottom :
          showNav && <BottomNav current={currentTab} onChange={onTabChange} />}
        <GestureBar />
      </div>
    </div>
  );
}

Object.assign(window, { Device, StatusBar, GestureBar, BottomNav, FAB });
