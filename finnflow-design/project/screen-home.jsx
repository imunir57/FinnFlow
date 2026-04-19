// FinnFlow — Home screen
// Two variations:
//  A) 'hero' — big serif balance, income/expense as small chips, comfortable list
//  B) 'ledger' — paper ledger style, two-column in/out header, tighter list

function CurrencyAmount({ value, currency, size = 14, color, weight = 500, dec = 0 }) {
  return (
    <span style={{
      fontFamily: 'Geist Mono, ui-monospace, monospace',
      fontVariantNumeric: 'tabular-nums',
      fontSize: size, color, fontWeight: weight,
      whiteSpace: 'nowrap',
    }}>
      <span style={{ opacity: 0.55, marginRight: 2 }}>{currency}</span>
      {fmt(value, { dec })}
    </span>
  );
}

function Avatar({ initials = 'SA', size = 36 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: 'oklch(0.55 0.09 155)',
      color: 'var(--paper)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: 'Geist, system-ui', fontSize: size * 0.38,
      fontWeight: 600, letterSpacing: 0.3,
      flexShrink: 0,
    }}>{initials}</div>
  );
}

function IconButton({ children, onClick, size = 40 }) {
  return (
    <button onClick={onClick} style={{
      width: size, height: size, borderRadius: '50%',
      background: 'transparent', border: 0, cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: 'var(--ink)',
    }}>{children}</button>
  );
}

function MonthHeader({ label, onPrev, onNext }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      gap: 4, padding: '6px 0 14px',
    }}>
      <IconButton onClick={onPrev} size={32}><Ic.chevL size={18} /></IconButton>
      <div style={{
        fontFamily: 'Geist, system-ui', fontSize: 14, fontWeight: 600,
        color: 'var(--ink)', letterSpacing: 0.2, minWidth: 120, textAlign: 'center',
      }}>{label}</div>
      <IconButton onClick={onNext} size={32}><Ic.chevR size={18} /></IconButton>
    </div>
  );
}

function TopBar({ onMenu }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 18px 4px', flexShrink: 0,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Avatar initials="SA" />
        <div>
          <div style={{ fontFamily: 'Geist, system-ui', fontSize: 11,
            color: 'var(--ink-3)', letterSpacing: 0.3, textTransform: 'uppercase' }}>
            Good morning
          </div>
          <div style={{ fontFamily: 'Geist, system-ui', fontSize: 15,
            color: 'var(--ink)', fontWeight: 600 }}>
            Sadia Ahmed
          </div>
        </div>
      </div>
      <IconButton onClick={onMenu}><Ic.dots size={20} /></IconButton>
    </div>
  );
}

// ── Variation A: Hero card ──────────────────────────────
function HomeHero({ currency, onOpenTx, txs }) {
  const income  = sumBy(txs, 'INCOME');
  const expense = sumBy(txs, 'EXPENSE');
  const balance = income - expense;
  const groups  = groupByDate(txs);

  return (
    <div style={{ flex: 1, overflowY: 'auto' }}>
      <TopBar />
      <MonthHeader label="April 2026" />

      {/* Hero balance card — punchier dark ink background with subtle texture */}
      <div style={{
        position: 'relative',
        margin: '0 18px 14px', padding: '22px 22px 18px',
        borderRadius: 24,
        color: 'var(--paper)',
        background: `
          radial-gradient(120% 80% at 0% 0%, oklch(0.42 0.09 155 / 0.55) 0%, transparent 55%),
          radial-gradient(110% 90% at 100% 100%, oklch(0.52 0.13 35 / 0.45) 0%, transparent 55%),
          linear-gradient(160deg, oklch(0.26 0.018 60) 0%, oklch(0.20 0.014 60) 100%)
        `,
        boxShadow: '0 16px 40px oklch(0.22 0.012 60 / 0.28), inset 0 1px 0 oklch(0.98 0.006 85 / 0.08)',
        overflow: 'hidden',
      }}>
        {/* decorative large numeric watermark */}
        <div style={{
          position: 'absolute', right: -14, top: -20,
          fontFamily: 'Newsreader, serif',
          fontSize: 180, lineHeight: 1,
          color: 'oklch(0.98 0.006 85 / 0.05)',
          pointerEvents: 'none', letterSpacing: -6,
        }}>৳</div>

        <div style={{ fontFamily: 'Geist, system-ui', fontSize: 11,
          color: 'oklch(0.98 0.006 85 / 0.65)', letterSpacing: 1, textTransform: 'uppercase',
          marginBottom: 6 }}>
          Net balance
        </div>
        <div style={{
          fontFamily: 'Newsreader, serif',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 54, lineHeight: 1.0,
          fontWeight: 400, letterSpacing: -0.8,
          position: 'relative',
        }}>
          <span style={{ fontSize: 28, opacity: 0.6, marginRight: 4,
            verticalAlign: 'top', position: 'relative', top: 8 }}>{currency}</span>
          {fmt(balance)}
        </div>

        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10,
          marginTop: 18, paddingTop: 14,
          borderTop: '1px solid oklch(0.98 0.006 85 / 0.14)',
          position: 'relative',
        }}>
          <HeroStat label="Income"  value={income}  currency={currency} color="oklch(0.78 0.12 155)" />
          <HeroStat label="Expense" value={expense} currency={currency} color="oklch(0.78 0.14 45)" />
        </div>
      </div>

      <TxList groups={groups} currency={currency} onOpen={onOpenTx} />
      <div style={{ height: 100 }} />
    </div>
  );
}

function HeroStat({ label, value, currency, color }) {
  return (
    <div>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        fontFamily: 'Geist, system-ui', fontSize: 10,
        color: 'oklch(0.98 0.006 85 / 0.65)', letterSpacing: 1, textTransform: 'uppercase',
        marginBottom: 4,
      }}>
        <span style={{ width: 6, height: 6, borderRadius: '50%', background: color }} />
        {label}
      </div>
      <div style={{
        fontFamily: 'Geist Mono, ui-monospace, monospace',
        fontVariantNumeric: 'tabular-nums',
        fontSize: 18, fontWeight: 500, color: 'oklch(0.98 0.006 85)',
      }}>
        <span style={{ opacity: 0.55, marginRight: 2, fontSize: 13 }}>{currency}</span>
        {fmt(value)}
      </div>
    </div>
  );
}

function StatSnippet({ label, value, currency, tone }) {
  const color = tone === 'pos' ? 'var(--pos)' : 'var(--neg)';
  return (
    <div>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        fontFamily: 'Geist, system-ui', fontSize: 11,
        color: 'var(--ink-3)', letterSpacing: 0.5, textTransform: 'uppercase',
        marginBottom: 4,
      }}>
        <span style={{
          width: 6, height: 6, borderRadius: '50%', background: color,
        }} />
        {label}
      </div>
      <div style={{
        fontFamily: 'Geist Mono, ui-monospace, monospace',
        fontVariantNumeric: 'tabular-nums',
        fontSize: 18, color: 'var(--ink)', fontWeight: 500,
      }}>
        <span style={{ opacity: 0.5, marginRight: 2, fontSize: 13 }}>{currency}</span>
        {fmt(value)}
      </div>
    </div>
  );
}

// Day-grouped list
function TxList({ groups, currency, onOpen }) {
  return (
    <div style={{ padding: '4px 0' }}>
      {groups.map(([date, list]) => {
        const d = new Date(date + 'T00:00:00');
        const day = d.getDate();
        const dow = d.toLocaleDateString('en-US', { weekday: 'short' });
        const mon = d.toLocaleDateString('en-US', { month: 'short' });
        const total = list.reduce((s,t) => s + (t.type === 'INCOME' ? t.amount : -t.amount), 0);
        return (
          <div key={date}>
            <div style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '14px 18px 6px',
            }}>
              <div style={{
                fontFamily: 'Newsreader, ui-serif, Georgia, serif',
                fontSize: 26, lineHeight: 1, color: 'var(--ink)',
                fontVariantNumeric: 'tabular-nums',
              }}>{day}</div>
              <div style={{
                fontFamily: 'Geist, system-ui', fontSize: 11,
                color: 'var(--ink-3)', letterSpacing: 0.5, textTransform: 'uppercase',
                lineHeight: 1.2,
              }}>
                <div>{dow}</div><div>{mon}</div>
              </div>
              <div style={{ flex: 1, height: 1, background: 'var(--rule)', marginLeft: 4 }} />
              <CurrencyAmount value={total} currency={currency} size={12}
                color={total >= 0 ? 'var(--pos)' : 'var(--ink-2)'}
                dec={0} />
            </div>
            {list.map(t => <TxRow key={t.id} tx={t} currency={currency} onClick={() => onOpen?.(t)} />)}
          </div>
        );
      })}
    </div>
  );
}

function TxRow({ tx, currency, onClick }) {
  const cat = catById(tx.catId);
  const I = Ic[cat.icon] || Ic.dots;
  const isIncome = tx.type === 'INCOME';
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14,
      padding: '10px 18px', width: '100%',
      background: 'none', border: 0, cursor: 'pointer', textAlign: 'left',
    }}>
      <div style={{
        width: 38, height: 38, borderRadius: 12,
        background: cat.color + '1a',
        color: cat.color,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <I size={19} />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 14.5,
          color: 'var(--ink)', fontWeight: 500,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{tx.subId || cat.name}</div>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 12,
          color: 'var(--ink-3)',
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{tx.note || cat.name}</div>
      </div>
      <CurrencyAmount
        value={tx.amount} currency={currency}
        size={15}
        color={isIncome ? 'var(--pos)' : 'var(--ink)'}
        weight={500}
      />
    </button>
  );
}

// ── Variation B: Ledger (paper accounting style) ────────
function HomeLedger({ currency, onOpenTx, txs }) {
  const income  = sumBy(txs, 'INCOME');
  const expense = sumBy(txs, 'EXPENSE');
  const balance = income - expense;
  const groups  = groupByDate(txs);

  return (
    <div style={{ flex: 1, overflowY: 'auto' }}>
      <TopBar />
      <MonthHeader label="April 2026" />

      {/* Ledger header — two-column in/out, thin rule, balance underneath */}
      <div style={{ margin: '0 18px 10px' }}>
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr',
          borderTop: '1.5px solid var(--ink)',
          borderBottom: '1px solid var(--rule)',
        }}>
          <div style={{ padding: '12px 0 10px', borderRight: '1px solid var(--rule)' }}>
            <div style={{
              fontFamily: 'Geist, system-ui', fontSize: 10,
              color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
              marginBottom: 4,
            }}>In</div>
            <div style={{
              fontFamily: 'Geist Mono, ui-monospace, monospace',
              fontVariantNumeric: 'tabular-nums',
              fontSize: 22, color: 'var(--pos)', fontWeight: 500,
            }}>
              <span style={{ opacity: 0.6, fontSize: 14, marginRight: 3 }}>{currency}</span>
              {fmt(income)}
            </div>
          </div>
          <div style={{ padding: '12px 0 10px 16px' }}>
            <div style={{
              fontFamily: 'Geist, system-ui', fontSize: 10,
              color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
              marginBottom: 4,
            }}>Out</div>
            <div style={{
              fontFamily: 'Geist Mono, ui-monospace, monospace',
              fontVariantNumeric: 'tabular-nums',
              fontSize: 22, color: 'var(--neg)', fontWeight: 500,
            }}>
              <span style={{ opacity: 0.6, fontSize: 14, marginRight: 3 }}>{currency}</span>
              {fmt(expense)}
            </div>
          </div>
        </div>
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          padding: '12px 0 4px',
        }}>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 11,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>Balance</span>
          <span style={{
            fontFamily: 'Newsreader, ui-serif, Georgia, serif',
            fontVariantNumeric: 'tabular-nums',
            fontSize: 30, color: 'var(--ink)', letterSpacing: -0.5,
          }}>
            <span style={{ opacity: 0.5, fontSize: 18, marginRight: 3 }}>{currency}</span>
            {fmt(balance)}
          </span>
        </div>
      </div>

      <TxList groups={groups} currency={currency} onOpen={onOpenTx} />
      <div style={{ height: 100 }} />
    </div>
  );
}

Object.assign(window, { HomeHero, HomeLedger, TopBar, MonthHeader, Avatar, IconButton, CurrencyAmount, TxList, TxRow });
