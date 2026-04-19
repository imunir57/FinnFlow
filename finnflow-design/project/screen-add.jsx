// FinnFlow — Add Transaction screen (variation A)
// Updates:
//  - 4 date chips (Today, Yesterday, one "random day" — the day before yesterday, Pick)
//  - Calculator icon replaces the . key in the keypad; tapping swaps the content
//    area to a full calculator view for expressions, then returns the result

function AddTransactionScreen({ currency, onCancel, onSave }) {
  const [type, setType] = React.useState('EXPENSE');
  const [amount, setAmount] = React.useState('');
  const [dateIdx, setDateIdx] = React.useState(0);
  const [catId, setCatId] = React.useState(1);
  const [subName, setSubName] = React.useState(null);
  const [note, setNote] = React.useState('');
  const [showCalc, setShowCalc] = React.useState(false);

  const cats = type === 'EXPENSE' ? EXPENSE_CATS : INCOME_CATS;
  const cat  = cats.find(c => c.id === catId) || cats[0];
  React.useEffect(() => {
    if (!cats.find(c => c.id === catId)) { setCatId(cats[0].id); setSubName(null); }
  }, [type]);

  const today = new Date('2026-04-19T00:00:00');
  const dateChips = [
    { id: 0, label: 'Today',     sub: fmtDay(today, 0) },
    { id: 1, label: 'Yesterday', sub: fmtDay(today, -1) },
    { id: 2, label: fmtWday(today, -2), sub: fmtDay(today, -2) },
  ];

  const canSave = amount && parseFloat(amount) > 0 && catId && subName;

  return (
    <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', background: 'var(--paper)' }}>
      {/* Top bar */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 14px 4px',
      }}>
        <IconButton onClick={showCalc ? () => setShowCalc(false) : onCancel}>
          {showCalc ? <Ic.back size={22} /> : <Ic.x size={22} />}
        </IconButton>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 15, fontWeight: 600,
          color: 'var(--ink)',
        }}>{showCalc ? 'Calculator' : 'New transaction'}</div>
        <button onClick={canSave ? onSave : undefined}
          disabled={!canSave || showCalc}
          style={{
            border: 0,
            background: canSave && !showCalc ? 'var(--ink)' : 'transparent',
            color:      canSave && !showCalc ? 'var(--paper)' : 'var(--ink-3)',
            padding: '8px 14px', borderRadius: 999,
            fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
            cursor: canSave && !showCalc ? 'pointer' : 'default',
            visibility: showCalc ? 'hidden' : 'visible',
          }}>Save</button>
      </div>

      {showCalc ? (
        <CalculatorView
          initial={amount}
          currency={currency}
          onCancel={() => setShowCalc(false)}
          onUse={(v) => { setAmount(String(v)); setShowCalc(false); }}
        />
      ) : (
        <>
          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: '6px 18px 20px' }}>

            <div style={{ display: 'flex', justifyContent: 'center', padding: '6px 0 16px' }}>
              <Segmented
                value={type} onChange={(v) => { setType(v); setSubName(null); }}
                items={[
                  { id: 'EXPENSE', label: 'Expense' },
                  { id: 'INCOME',  label: 'Income' },
                ]}
              />
            </div>

            {/* Amount — hero */}
            <div style={{
              display: 'flex', justifyContent: 'center', alignItems: 'baseline',
              gap: 6, padding: '2px 0 4px',
            }}>
              <span style={{ fontFamily: 'Newsreader, serif', fontSize: 34, color: 'var(--ink-3)' }}>{currency}</span>
              <span style={{
                fontFamily: 'Newsreader, serif', fontVariantNumeric: 'tabular-nums',
                fontSize: 64, lineHeight: 1, color: type === 'INCOME' ? 'var(--pos)' : 'var(--ink)',
                letterSpacing: -1.2, fontWeight: 400,
              }}>
                {amount ? formatAmount(amount) : <span style={{ color: 'var(--ink-4)' }}>0</span>}
              </span>
            </div>
            <div style={{ textAlign: 'center', paddingBottom: 18 }}>
              <span style={{
                fontFamily: 'Geist, system-ui', fontSize: 11,
                color: 'var(--ink-3)', letterSpacing: 0.6, textTransform: 'uppercase',
              }}>Tap keypad — or use calculator</span>
            </div>

            {/* Date — 4 fixed chips */}
            <FieldLabel>Date</FieldLabel>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
              {dateChips.map(d => (
                <Chip key={d.id} active={dateIdx === d.id} onClick={() => setDateIdx(d.id)} column>
                  <span style={{ fontSize: 12.5, fontWeight: 600 }}>{d.label}</span>
                  <span style={{ fontSize: 10.5, opacity: 0.7 }}>{d.sub}</span>
                </Chip>
              ))}
              <Chip active={dateIdx === 3} onClick={() => setDateIdx(3)} column>
                <Ic.calendar size={16} />
                <span style={{ fontSize: 10.5, opacity: 0.7 }}>Pick</span>
              </Chip>
            </div>

            {/* Category */}
            <FieldLabel style={{ marginTop: 18 }}>Category</FieldLabel>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {cats.map(c => {
                const I = Ic[c.icon] || Ic.dots;
                const active = c.id === catId;
                return (
                  <button key={c.id} onClick={() => { setCatId(c.id); setSubName(null); }}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 8,
                      padding: '8px 12px 8px 10px',
                      borderRadius: 999,
                      border: active ? '1.5px solid ' + c.color : '1px solid var(--rule)',
                      background: active ? c.color + '14' : 'var(--paper)',
                      color: active ? c.color : 'var(--ink-2)',
                      cursor: 'pointer',
                      fontFamily: 'Geist, system-ui', fontSize: 13,
                      fontWeight: active ? 600 : 500,
                    }}>
                    <div style={{
                      width: 20, height: 20, borderRadius: 6,
                      background: c.color + (active ? '22' : '14'),
                      color: c.color,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}><I size={13} /></div>
                    {c.name}
                  </button>
                );
              })}
            </div>

            {/* Sub-category */}
            <FieldLabel style={{ marginTop: 18 }}>Sub-category</FieldLabel>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7 }}>
              {cat.subs.map(s => {
                const active = s === subName;
                return (
                  <button key={s} onClick={() => setSubName(s)} style={{
                    padding: '7px 12px', borderRadius: 999,
                    border: active ? '1.5px solid var(--ink)' : '1px solid var(--rule)',
                    background: active ? 'var(--ink)' : 'var(--paper)',
                    color: active ? 'var(--paper)' : 'var(--ink-2)',
                    cursor: 'pointer',
                    fontFamily: 'Geist, system-ui', fontSize: 12.5,
                    fontWeight: active ? 600 : 500,
                  }}>{s}</button>
                );
              })}
            </div>

            {/* Note */}
            <FieldLabel style={{ marginTop: 18 }}>Note <span style={{ opacity: 0.6, textTransform: 'none', letterSpacing: 0 }}>— optional</span></FieldLabel>
            <input
              value={note} onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. Dinner with Rafi"
              style={{
                width: '100%', boxSizing: 'border-box',
                padding: '12px 14px',
                border: '1px solid var(--rule)',
                borderRadius: 12, background: 'var(--card)',
                fontFamily: 'Geist, system-ui', fontSize: 14, color: 'var(--ink)',
                outline: 'none',
              }}
            />
          </div>

          {/* Keypad */}
          <Keypad
            onDigit={(d) => setAmount(a => appendDigit(a, d))}
            onDot={() => setAmount(a => a.includes('.') ? a : (a || '0') + '.')}
            onBack={() => setAmount(a => a.slice(0, -1))}
            onCalc={() => setShowCalc(true)}
          />
        </>
      )}
    </div>
  );
}

function FieldLabel({ children, style }) {
  return (
    <div style={{
      fontFamily: 'Geist, system-ui', fontSize: 10.5,
      color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
      marginBottom: 10, ...style,
    }}>{children}</div>
  );
}

function Chip({ active, onClick, children, column }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', flexDirection: column ? 'column' : 'row',
      alignItems: 'center', justifyContent: 'center',
      gap: column ? 2 : 6,
      padding: '10px 12px',
      borderRadius: 14,
      border: active ? '1.5px solid var(--ink)' : '1px solid var(--rule)',
      background: active ? 'var(--ink)' : 'var(--paper)',
      color: active ? 'var(--paper)' : 'var(--ink-2)',
      cursor: 'pointer',
      fontFamily: 'Geist, system-ui',
      flexShrink: 0,
    }}>{children}</button>
  );
}

// ── Keypad — calculator icon replaces the dot key ──
function Keypad({ onDigit, onDot, onBack, onCalc }) {
  // layout is 4 rows of 3 keys: 1 2 3 / 4 5 6 / 7 8 9 / calc 0 ⌫
  const rows = [
    ['1','2','3'],
    ['4','5','6'],
    ['7','8','9'],
    ['calc','0','back'],
  ];
  return (
    <div style={{
      borderTop: '1px solid var(--rule)',
      padding: '8px 6px 6px',
      background: 'var(--paper-2)',
      flexShrink: 0,
      display: 'grid',
      gridTemplateColumns: 'repeat(3, 1fr)',
      gap: 2,
    }}>
      {rows.flat().map((k, i) => {
        const isDigit = /^[0-9]$/.test(k);
        const isCalc = k === 'calc';
        const isBack = k === 'back';
        return (
          <button key={i}
            onClick={() => {
              if (isCalc) onCalc();
              else if (isBack) onBack();
              else if (isDigit) onDigit(k);
            }}
            onMouseDown={(e) => e.currentTarget.style.background = 'var(--ink-wash)'}
            onMouseUp={(e) => e.currentTarget.style.background = 'transparent'}
            onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
            style={{
              height: 50, border: 0, background: 'transparent',
              borderRadius: 12,
              fontFamily: 'Newsreader, serif',
              fontSize: 26,
              color: 'var(--ink)', cursor: 'pointer',
              fontVariantNumeric: 'tabular-nums',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            {isCalc ? <CalcGlyph /> :
             isBack ? <Ic.back size={22} /> :
             k}
          </button>
        );
      })}
    </div>
  );
}

function CalcGlyph({ size = 22 }) {
  // custom inline svg: small 4-button calculator icon
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="1.6"
         strokeLinecap="round" strokeLinejoin="round">
      <rect x="4" y="3" width="16" height="18" rx="2.5" />
      <rect x="7" y="6" width="10" height="3" rx="0.5" fill="currentColor" fillOpacity="0.15" stroke="none"/>
      <circle cx="9"  cy="13" r="0.6" fill="currentColor"/>
      <circle cx="12" cy="13" r="0.6" fill="currentColor"/>
      <circle cx="15" cy="13" r="0.6" fill="currentColor"/>
      <circle cx="9"  cy="17" r="0.6" fill="currentColor"/>
      <circle cx="12" cy="17" r="0.6" fill="currentColor"/>
      <circle cx="15" cy="17" r="0.6" fill="currentColor"/>
    </svg>
  );
}

// ── Calculator view ─────────────────────────────────────
// Shown when the keypad's calc icon is tapped. Simple 4-function with expression history.
function CalculatorView({ initial, currency, onCancel, onUse }) {
  const [expr, setExpr] = React.useState(initial || '');
  const result = React.useMemo(() => safeEval(expr), [expr]);

  const press = (t) => {
    if (t === 'C') return setExpr('');
    if (t === '⌫') return setExpr(e => e.slice(0, -1));
    if (t === '=') { if (result !== null) setExpr(String(result)); return; }
    setExpr(e => appendExpr(e, t));
  };

  const buttons = [
    ['C','÷','×','⌫'],
    ['7','8','9','−'],
    ['4','5','6','+'],
    ['1','2','3','='],
    ['0','.','=','='],   // placeholder row; we override below
  ];

  return (
    <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      {/* Display */}
      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column',
        justifyContent: 'flex-end', padding: '20px 22px', gap: 8 }}>
        <div style={{
          fontFamily: 'Geist Mono, ui-monospace, monospace',
          fontSize: 22, color: 'var(--ink-3)',
          textAlign: 'right', wordBreak: 'break-all',
          minHeight: 30,
        }}>{expr || ' '}</div>
        <div style={{
          fontFamily: 'Newsreader, serif',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 52, color: 'var(--ink)',
          textAlign: 'right', letterSpacing: -1, lineHeight: 1,
        }}>
          <span style={{ opacity: 0.5, fontSize: 26, marginRight: 4 }}>{currency}</span>
          {result === null ? '—' : formatAmount(String(result))}
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 10 }}>
          <button onClick={onCancel} style={{
            background: 'transparent', border: '1px solid var(--rule)',
            padding: '10px 18px', borderRadius: 999,
            fontFamily: 'Geist, system-ui', fontSize: 13, color: 'var(--ink-2)',
            cursor: 'pointer',
          }}>Cancel</button>
          <button onClick={() => result !== null && onUse(result)}
            disabled={result === null}
            style={{
              background: result === null ? 'var(--ink-wash)' : 'var(--ink)',
              color: result === null ? 'var(--ink-3)' : 'var(--paper)',
              border: 0, padding: '10px 20px', borderRadius: 999,
              fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
              cursor: result === null ? 'default' : 'pointer',
            }}>Use amount</button>
        </div>
      </div>

      {/* Calculator grid */}
      <div style={{
        borderTop: '1px solid var(--rule)',
        padding: '10px 8px 10px',
        background: 'var(--paper-2)',
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
        gap: 4,
        flexShrink: 0,
      }}>
        {[
          'C','(',')','⌫',
          '7','8','9','÷',
          '4','5','6','×',
          '1','2','3','−',
          '0','.','=','+',
        ].map((k, i) => {
          const isOp = '÷×−+'.includes(k) || k === '=';
          const isFn = k === 'C' || k === '⌫' || k === '(' || k === ')';
          return (
            <button key={i}
              onClick={() => press(k)}
              onMouseDown={(e) => e.currentTarget.style.background =
                (isOp ? 'oklch(0.30 0.012 60)' : 'var(--ink-wash)')}
              onMouseUp={(e) => e.currentTarget.style.background =
                (isOp ? 'var(--ink)' : isFn ? 'var(--card)' : 'transparent')}
              onMouseLeave={(e) => e.currentTarget.style.background =
                (isOp ? 'var(--ink)' : isFn ? 'var(--card)' : 'transparent')}
              style={{
                height: 52,
                border: 0,
                borderRadius: 14,
                background: isOp ? 'var(--ink)' : isFn ? 'var(--card)' : 'transparent',
                color: isOp ? 'var(--paper)' : 'var(--ink)',
                fontFamily: isOp || isFn ? 'Geist, system-ui' : 'Newsreader, serif',
                fontSize: isOp || isFn ? 18 : 24,
                fontWeight: isOp ? 600 : 400,
                cursor: 'pointer',
                fontVariantNumeric: 'tabular-nums',
              }}>{k}</button>
          );
        })}
      </div>
    </div>
  );
}

function appendExpr(cur, t) {
  const last = cur.slice(-1);
  if ('÷×−+'.includes(t)) {
    if (!cur) return '';             // don't start with op
    if ('÷×−+'.includes(last)) return cur.slice(0, -1) + t;
  }
  return cur + t;
}

function safeEval(expr) {
  if (!expr) return null;
  if ('÷×−+.('.includes(expr.slice(-1))) return null;
  const js = expr.replace(/÷/g,'/').replace(/×/g,'*').replace(/−/g,'-');
  if (!/^[\d+\-*/().\s]+$/.test(js)) return null;
  try {
    // eslint-disable-next-line no-new-func
    const v = Function('"use strict"; return (' + js + ')')();
    if (!isFinite(v)) return null;
    return Math.round(v * 100) / 100;
  } catch { return null; }
}

function appendDigit(cur, d) {
  if (!cur && d === '0') return '0';
  if (cur === '0') return d;
  return cur + d;
}

function formatAmount(s) {
  if (s === '' || s === null || s === undefined) return '0';
  const [intp, dec] = String(s).split('.');
  const n = parseInt(intp || '0', 10);
  const intFmt = n.toLocaleString('en-IN');
  return dec !== undefined ? intFmt + '.' + dec : intFmt;
}

function fmtDay(base, delta) {
  const d = new Date(base.getTime() + delta * 86400000);
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
function fmtWday(base, delta) {
  const d = new Date(base.getTime() + delta * 86400000);
  return d.toLocaleDateString('en-US', { weekday: 'short' });
}

Object.assign(window, { AddTransactionScreen });
