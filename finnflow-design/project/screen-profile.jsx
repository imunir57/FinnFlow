// FinnFlow — Profile screen
// Editable display name + avatar, account info, sync placeholder, life-time stats

function ProfileScreen({ onBack, currency, txs }) {
  const [name, setName]   = React.useState('Sadia Ahmed');
  const [email]           = React.useState('sadia.ahmed@gmail.com');
  const [editing, setEdit] = React.useState(false);
  const [draft, setDraft]  = React.useState(name);

  // Life-time stats from mock txs
  const totalIn  = txs ? sumBy(txs, 'INCOME')  : 86500;
  const totalOut = txs ? sumBy(txs, 'EXPENSE') : 40570;
  const txCount  = txs ? txs.length : 22;
  const initials = name.split(' ').map(w => w[0]).slice(0,2).join('').toUpperCase();

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
        }}>Profile</div>
      </div>

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 24 }}>

        {/* Avatar block */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '14px 18px 22px' }}>
          <div style={{ position: 'relative' }}>
            <div style={{
              width: 90, height: 90, borderRadius: '50%',
              background: 'var(--pos)', color: 'var(--paper)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: 'Newsreader, serif', fontSize: 36, fontWeight: 500,
              boxShadow: '0 8px 22px oklch(0.42 0.09 155 / 0.25)',
            }}>{initials}</div>
            <button style={{
              position: 'absolute', right: -4, bottom: -4,
              width: 32, height: 32, borderRadius: '50%',
              background: 'var(--ink)', color: 'var(--paper)',
              border: '3px solid var(--paper)',
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><Ic.pencil size={14} /></button>
          </div>

          {/* Name — inline editable */}
          {editing ? (
            <div style={{
              display: 'flex', alignItems: 'center', gap: 6,
              marginTop: 14, width: '100%', maxWidth: 280,
            }}>
              <input value={draft} autoFocus
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') { setName(draft); setEdit(false); }
                  if (e.key === 'Escape'){ setDraft(name); setEdit(false); }
                }}
                style={{
                  flex: 1,
                  fontFamily: 'Newsreader, serif', fontSize: 22,
                  color: 'var(--ink)',
                  textAlign: 'center',
                  border: 0,
                  borderBottom: '1.5px solid var(--ink)',
                  background: 'transparent',
                  padding: '6px 4px',
                  outline: 'none',
                }} />
              <button onClick={() => { setName(draft); setEdit(false); }}
                style={{
                  border: 0, background: 'var(--ink)', color: 'var(--paper)',
                  width: 30, height: 30, borderRadius: '50%',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  cursor: 'pointer', flexShrink: 0,
                }}><Ic.check size={16} /></button>
            </div>
          ) : (
            <button onClick={() => { setDraft(name); setEdit(true); }}
              style={{
                marginTop: 14, padding: '4px 10px',
                background: 'transparent', border: 0, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
                fontFamily: 'Newsreader, serif', fontSize: 24,
                color: 'var(--ink)', fontWeight: 400,
              }}>
              {name}
              <Ic.pencil size={13} color="var(--ink-3)" />
            </button>
          )}
          <div style={{
            marginTop: 4,
            fontFamily: 'Geist, system-ui', fontSize: 12.5,
            color: 'var(--ink-3)',
          }}>{email}</div>
        </div>

        {/* Stats triptych */}
        <div style={{
          margin: '0 16px 18px',
          padding: '16px 12px',
          background: 'var(--card)',
          border: '1px solid var(--rule)',
          borderRadius: 18,
          display: 'flex', alignItems: 'stretch',
        }}>
          <StatCell label="Income" value={totalIn}  currency={currency} color="var(--pos)" />
          <Divider />
          <StatCell label="Expense" value={totalOut} currency={currency} color="var(--neg)" />
          <Divider />
          <StatCell label="Entries" value={txCount} plain />
        </div>

        <SectionHead>Account</SectionHead>
        <SettingsRow icon={Ic.mail} iconBg="#3A6EA5" label="Email"
          sub={email} />
        <SettingsRow icon={Ic.cloud} iconBg="#2E8B94" label="Cloud sync"
          sub="Sign in with Google to enable"
          right={<span style={{
            padding: '3px 8px', borderRadius: 999,
            background: 'var(--ink-wash)',
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'var(--ink-3)', letterSpacing: 0.5,
          }}>SOON</span>} />
        <SettingsRow icon={Ic.shield} iconBg="#7A5C3E" label="Privacy"
          sub="Your data is stored on this device only" />

        <SectionHead>Preferences</SectionHead>
        <SettingsRow icon={Ic.calendar} iconBg="#7A4FA0" label="Start of month"
          right={<span style={{
            fontFamily: 'Geist, system-ui', fontSize: 12.5,
            color: 'var(--ink-2)',
          }}>1st</span>} />
        <SettingsRow icon={Ic.coin} iconBg="#3E4A8A" label="Default currency"
          right={<span style={{
            fontFamily: 'Newsreader, serif', fontSize: 18,
            color: 'var(--ink-2)',
          }}>{currency}</span>} />

        {/* Joined since */}
        <div style={{
          textAlign: 'center', padding: '20px 0 4px',
          fontFamily: 'Geist, system-ui', fontSize: 11.5,
          color: 'var(--ink-3)',
        }}>Member since January 2025</div>
      </div>
    </div>
  );
}

function StatCell({ label, value, currency, color = 'var(--ink)', plain }) {
  return (
    <div style={{ flex: 1, textAlign: 'center', padding: '0 4px' }}>
      <div style={{
        fontFamily: 'Newsreader, serif', fontSize: 10,
        color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
        marginBottom: 4,
      }}>{label}</div>
      <div style={{
        fontFamily: 'Newsreader, serif',
        fontVariantNumeric: 'tabular-nums',
        fontSize: 18, color, letterSpacing: -0.2, lineHeight: 1.1,
      }}>
        {!plain && <span style={{ opacity: 0.55, fontSize: 11, marginRight: 1 }}>{currency}</span>}
        {fmt(value)}
      </div>
    </div>
  );
}

function Divider() {
  return <div style={{ width: 1, alignSelf: 'stretch', background: 'var(--rule)' }} />;
}

Object.assign(window, { ProfileScreen });
