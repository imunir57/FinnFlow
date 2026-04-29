// FinnFlow — Settings screen
// Accessed via the 3-dot menu on Home (since Settings is no longer in bottom nav).
// Sectioned list: Account · Preferences · Data · About · sign-out

function SettingsScreen({ onBack, onOpenCategories, onOpenProfile, currency, onCurrency }) {
  return (
    <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', background: 'var(--paper)' }}>
      {/* Top bar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 4,
        padding: '8px 8px 6px',
      }}>
        <IconButton onClick={onBack}><Ic.back size={20} /></IconButton>
        <div style={{
          fontFamily: 'Newsreader, serif', fontSize: 26,
          color: 'var(--ink)', fontWeight: 400,
        }}>Settings</div>
      </div>

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 24 }}>

        {/* Profile summary card */}
        <button onClick={onOpenProfile} style={{
          display: 'flex', alignItems: 'center', gap: 14,
          width: 'calc(100% - 32px)', margin: '6px 16px 18px',
          padding: '14px 16px', borderRadius: 18,
          background: 'var(--card)', border: '1px solid var(--rule)',
          cursor: 'pointer', textAlign: 'left',
        }}>
          <div style={{
            width: 50, height: 50, borderRadius: '50%',
            background: 'var(--pos)', color: 'var(--paper)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: 'Geist, system-ui', fontSize: 18, fontWeight: 600,
            flexShrink: 0,
          }}>SA</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              fontFamily: 'Geist, system-ui', fontSize: 15, fontWeight: 600,
              color: 'var(--ink)',
            }}>Sadia Ahmed</div>
            <div style={{
              fontFamily: 'Geist, system-ui', fontSize: 12,
              color: 'var(--ink-3)', marginTop: 2,
            }}>Tap to edit profile</div>
          </div>
          <Ic.chevR size={18} color="var(--ink-3)" />
        </button>

        <SectionHead>Manage</SectionHead>
        <SettingsRow icon={Ic.bag} iconBg="#7A5C3E" label="Categories"
          sub="Add, edit, reorder categories" onClick={onOpenCategories} />
        <SettingsRow icon={Ic.coin} iconBg="#3E4A8A" label="Currency"
          right={<span style={{
            fontFamily: 'Newsreader, serif', fontSize: 18,
            color: 'var(--ink-2)',
          }}>{currency}</span>}
          onClick={onCurrency} />
        <SettingsRow icon={Ic.bell} iconBg="#B5456E" label="Notifications"
          sub="Daily reminder · 9:00 PM"
          right={<Toggle on />} />

        <SectionHead>Data</SectionHead>
        <SettingsRow icon={Ic.cloudUp} iconBg="#3A6EA5" label="Backup"
          sub="Last backup — Apr 17, 2026" />
        <SettingsRow icon={Ic.cloudDn} iconBg="#2E8B94" label="Restore"
          sub="From a previous backup file" />
        <SettingsRow icon={Ic.export} iconBg="#7A4FA0" label="Export to CSV"
          sub="Share your transactions as a spreadsheet" />

        <SectionHead>App</SectionHead>
        <SettingsRow icon={Ic.palette} iconBg="#D18842" label="Appearance"
          right={<span style={{ fontFamily: 'Geist, system-ui', fontSize: 12.5, color: 'var(--ink-3)' }}>System</span>} />
        <SettingsRow icon={Ic.lock} iconBg="#556B74" label="App Lock"
          sub="Require fingerprint to open"
          right={<Toggle />} />
        <SettingsRow icon={Ic.info} iconBg="#6E8A4A" label="About FinnFlow"
          sub="Version 1.0.0 · Build 102" />

        {/* Sign out */}
        <button style={{
          margin: '20px 16px 8px', padding: '14px',
          width: 'calc(100% - 32px)',
          background: 'transparent',
          border: '1px solid var(--rule)',
          borderRadius: 14,
          fontFamily: 'Geist, system-ui', fontSize: 13.5, fontWeight: 600,
          color: 'var(--neg)',
          cursor: 'pointer',
        }}>Sign out</button>

        <div style={{
          textAlign: 'center', padding: '14px 0 4px',
          fontFamily: 'Newsreader, serif', fontSize: 14,
          color: 'var(--ink-4)', fontStyle: 'italic',
        }}>FinnFlow · made for keeping count</div>
      </div>
    </div>
  );
}

function SectionHead({ children }) {
  return (
    <div style={{
      padding: '14px 22px 6px',
      fontFamily: 'Geist, system-ui', fontSize: 10.5,
      color: 'var(--ink-3)', letterSpacing: 1.2, textTransform: 'uppercase',
      fontWeight: 600,
    }}>{children}</div>
  );
}

function SettingsRow({ icon: I, iconBg, label, sub, right, onClick }) {
  const Tag = onClick ? 'button' : 'div';
  return (
    <Tag onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14,
      width: '100%', padding: '12px 22px',
      background: 'transparent', border: 0, cursor: onClick ? 'pointer' : 'default',
      textAlign: 'left', boxSizing: 'border-box',
    }}
      onMouseDown={(e) => onClick && (e.currentTarget.style.background = 'var(--ink-wash)')}
      onMouseUp={(e) => e.currentTarget.style.background = 'transparent'}
      onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}>
      <div style={{
        width: 34, height: 34, borderRadius: 10,
        background: iconBg + '18', color: iconBg,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}><I size={18} /></div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 14.5, fontWeight: 500,
          color: 'var(--ink)',
        }}>{label}</div>
        {sub && <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 11.5,
          color: 'var(--ink-3)', marginTop: 1,
        }}>{sub}</div>}
      </div>
      {right ?? (onClick && <Ic.chevR size={16} color="var(--ink-3)" />)}
    </Tag>
  );
}

function Toggle({ on: initial = false }) {
  const [on, setOn] = React.useState(initial);
  return (
    <button onClick={(e) => { e.stopPropagation(); setOn(v => !v); }}
      style={{
        width: 38, height: 22, borderRadius: 999,
        background: on ? 'var(--ink)' : 'var(--rule)',
        border: 0, cursor: 'pointer', padding: 2,
        display: 'flex', alignItems: 'center',
        transition: 'background 160ms ease',
      }}>
      <span style={{
        width: 18, height: 18, borderRadius: '50%',
        background: 'var(--paper)',
        transform: on ? 'translateX(16px)' : 'translateX(0)',
        transition: 'transform 160ms ease',
        boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
      }} />
    </button>
  );
}

Object.assign(window, { SettingsScreen, SettingsRow, SectionHead, Toggle });
