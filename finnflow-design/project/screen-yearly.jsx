// FinnFlow — Yearly screen
// Year navigator + hero summary + monthly bars (income above, expense below)
// Mirrors the Compose YearlyScreen's structure.

function YearlyScreen({ currency, onMenu, txs }) {
  const [year, setYear] = React.useState(2026);

  // Build per-month aggregates from mock txs (April only in seed) + sprinkle other months
  const monthly = React.useMemo(() => {
    const arr = Array.from({ length: 12 }, (_, i) => ({ income: 0, expense: 0 }));
    // Real data from txs (assume April 2026)
    for (const t of (txs || TX)) {
      const m = parseInt(t.date.slice(5,7), 10) - 1;
      if (t.type === 'INCOME') arr[m].income += t.amount;
      else arr[m].expense += t.amount;
    }
    // Add some plausible historic values for prior months so the year tells a story
    const seed = [
      { i: 62000, e: 38500 }, { i: 64000, e: 41200 }, { i: 70000, e: 39800 },
      null,                   // April — use real
      null, null, null, null, null, null, null, null,
    ];
    seed.forEach((s, idx) => {
      if (s) { arr[idx].income = s.i; arr[idx].expense = s.e; }
    });
    return arr;
  }, [txs, year]);

  const totalIncome  = monthly.reduce((s, m) => s + m.income, 0);
  const totalExpense = monthly.reduce((s, m) => s + m.expense, 0);
  const netBalance   = totalIncome - totalExpense;
  const maxVal = Math.max(1, ...monthly.flatMap(m => [m.income, m.expense]));
  const avgIn  = totalIncome  / 4;   // April so far → 4 months avg
  const avgOut = totalExpense / 4;

  const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];

  return (
    <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      {/* Title bar */}
      <div style={{
        display: 'flex', alignItems: 'center',
        padding: '8px 12px 6px',
      }}>
        <div style={{
          fontFamily: 'Newsreader, serif', fontSize: 26,
          color: 'var(--ink)', fontWeight: 400, flex: 1, paddingLeft: 6,
        }}>Yearly</div>
        <IconButton onClick={onMenu}><Ic.dots size={20} /></IconButton>
      </div>

      {/* Year navigator */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4,
        padding: '0 0 6px',
      }}>
        <IconButton onClick={() => setYear(y => y - 1)}><Ic.chevL size={18} /></IconButton>
        <span style={{
          fontFamily: 'Geist Mono, ui-monospace, monospace',
          fontSize: 14, fontWeight: 600, color: 'var(--ink)',
          padding: '0 8px',
        }}>{year}</span>
        <IconButton onClick={() => setYear(y => y + 1)}><Ic.chevR size={18} /></IconButton>
      </div>

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 28 }}>

        {/* Hero summary card — same treatment as Home hero */}
        <div style={{
          position: 'relative',
          margin: '4px 16px 14px', padding: '20px 22px 16px',
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
          <div style={{
            position: 'absolute', right: -18, top: -28,
            fontFamily: 'Newsreader, serif',
            fontSize: 200, lineHeight: 1,
            color: 'oklch(0.98 0.006 85 / 0.05)',
            pointerEvents: 'none', letterSpacing: -8,
          }}>{year}</div>

          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 11,
            color: 'oklch(0.98 0.006 85 / 0.65)', letterSpacing: 1, textTransform: 'uppercase',
            marginBottom: 4,
          }}>Net balance · {year}</div>
          <div style={{
            fontFamily: 'Newsreader, serif',
            fontVariantNumeric: 'tabular-nums',
            fontSize: 46, lineHeight: 1.0, letterSpacing: -0.6,
            position: 'relative',
          }}>
            <span style={{ fontSize: 24, opacity: 0.6, marginRight: 4,
              verticalAlign: 'top', position: 'relative', top: 6 }}>{currency}</span>
            {fmt(netBalance)}
          </div>

          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10,
            marginTop: 14, paddingTop: 12,
            borderTop: '1px solid oklch(0.98 0.006 85 / 0.14)',
          }}>
            <HeroStat label="Income"  value={totalIncome}  currency={currency} color="oklch(0.78 0.12 155)" />
            <HeroStat label="Expense" value={totalExpense} currency={currency} color="oklch(0.78 0.14 45)" />
          </div>
        </div>

        {/* Monthly avg strip */}
        <div style={{
          margin: '0 16px 12px',
          display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10,
          padding: '12px 14px',
          background: 'var(--card)', border: '1px solid var(--rule)',
          borderRadius: 16,
        }}>
          <AvgCell label="Avg / month in"  value={avgIn}  color="var(--pos)" currency={currency} />
          <div style={{ width: 1, position: 'absolute' }} />
          <AvgCell label="Avg / month out" value={avgOut} color="var(--neg)" currency={currency} />
        </div>

        {/* Column header */}
        <div style={{
          display: 'flex', justifyContent: 'space-between',
          padding: '10px 22px 8px',
        }}>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>Month</span>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>In · Out · Net</span>
        </div>
        <div style={{ borderTop: '1px solid var(--rule)' }} />

        {months.map((name, i) => (
          <YearMonthRow key={i}
            name={name}
            income={monthly[i].income}
            expense={monthly[i].expense}
            maxVal={maxVal}
            currency={currency}
            current={year === 2026 && i === 3}
          />
        ))}
      </div>
    </div>
  );
}

function AvgCell({ label, value, color, currency }) {
  return (
    <div>
      <div style={{
        fontFamily: 'Geist, system-ui', fontSize: 10,
        color: 'var(--ink-3)', letterSpacing: 0.8, textTransform: 'uppercase',
      }}>{label}</div>
      <div style={{
        fontFamily: 'Newsreader, serif',
        fontVariantNumeric: 'tabular-nums',
        fontSize: 22, color, marginTop: 2, letterSpacing: -0.2,
      }}>
        <span style={{ opacity: 0.55, fontSize: 13, marginRight: 1 }}>{currency}</span>
        {fmt(Math.round(value))}
      </div>
    </div>
  );
}

function YearMonthRow({ name, income, expense, maxVal, currency, current }) {
  const balance = income - expense;
  const hasData = income > 0 || expense > 0;

  return (
    <div style={{
      padding: '12px 22px',
      borderBottom: '1px solid var(--rule)',
      background: current ? 'var(--ink-wash)' : 'transparent',
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10, marginBottom: hasData ? 8 : 0,
      }}>
        <span style={{
          fontFamily: 'Geist, system-ui', fontSize: 14, fontWeight: 500,
          color: hasData ? 'var(--ink)' : 'var(--ink-4)', flex: 1,
        }}>
          {name}
          {current && <span style={{
            marginLeft: 8, padding: '1px 6px', borderRadius: 4,
            background: 'var(--ink)', color: 'var(--paper)',
            fontSize: 9, letterSpacing: 0.6, textTransform: 'uppercase',
            fontWeight: 600, verticalAlign: 1,
          }}>Now</span>}
        </span>

        {hasData ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{
              fontFamily: 'Geist Mono, ui-monospace, monospace',
              fontSize: 11, color: 'var(--pos)',
              fontVariantNumeric: 'tabular-nums',
            }}>+{fmt(income)}</span>
            <span style={{
              fontFamily: 'Geist Mono, ui-monospace, monospace',
              fontSize: 11, color: 'var(--neg)',
              fontVariantNumeric: 'tabular-nums',
            }}>−{fmt(expense)}</span>
            <span style={{
              fontFamily: 'Newsreader, serif',
              fontVariantNumeric: 'tabular-nums',
              fontSize: 14, fontWeight: 500,
              color: balance >= 0 ? 'var(--pos)' : 'var(--neg)',
              minWidth: 56, textAlign: 'right',
            }}>{balance >= 0 ? '+' : '−'}{fmt(Math.abs(balance))}</span>
          </div>
        ) : (
          <span style={{ color: 'var(--ink-4)', fontFamily: 'Newsreader, serif', fontSize: 16 }}>—</span>
        )}
      </div>

      {hasData && (
        <div>
          <BarTrack value={income / maxVal} color="var(--pos)" />
          <div style={{ height: 3 }} />
          <BarTrack value={expense / maxVal} color="var(--neg)" />
        </div>
      )}
    </div>
  );
}

function BarTrack({ value, color }) {
  return (
    <div style={{
      height: 3, borderRadius: 2, background: 'var(--rule)',
      overflow: 'hidden',
    }}>
      <div style={{
        width: `${Math.min(100, Math.max(0, value * 100))}%`,
        height: '100%', background: color, opacity: 0.75,
      }} />
    </div>
  );
}

Object.assign(window, { YearlyScreen });
