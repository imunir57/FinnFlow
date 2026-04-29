// FinnFlow — extra Stats insight blocks
// Renders below the donut, above the category list

// ── 1. Income vs Expense ratio bar with savings rate ─────────
function IncomeExpenseBar({ currency }) {
  const income  = sumBy(TX, 'INCOME');
  const expense = sumBy(TX, 'EXPENSE');
  const net     = income - expense;
  const savingsRate = income > 0 ? (net / income) * 100 : 0;
  const max = Math.max(income, expense, 1);

  return (
    <InsightCard
      title="Income vs Expense"
      caption={
        savingsRate > 0
          ? `Saved ${savingsRate.toFixed(0)}% of income this month`
          : `Overspent — net ${currency}${fmt(Math.abs(net))}`
      }
    >
      <div style={{ marginTop: 10 }}>
        <BarLine
          label="Income"  value={income}  max={max}
          color="var(--pos)" currency={currency}
        />
        <div style={{ height: 8 }} />
        <BarLine
          label="Expense" value={expense} max={max}
          color="var(--neg)" currency={currency}
        />
      </div>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
        marginTop: 12, paddingTop: 10,
        borderTop: '1px solid var(--rule)',
      }}>
        <span style={{
          fontFamily: 'Geist, system-ui', fontSize: 11,
          color: 'var(--ink-3)', letterSpacing: 0.6, textTransform: 'uppercase',
        }}>Net balance</span>
        <span style={{
          fontFamily: 'Newsreader, serif',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 22, color: net >= 0 ? 'var(--pos)' : 'var(--neg)',
          letterSpacing: -0.3,
        }}>
          {net >= 0 ? '+' : '−'}
          <span style={{ opacity: 0.55, fontSize: 13, marginLeft: 1, marginRight: 1 }}>{currency}</span>
          {fmt(Math.abs(net))}
        </span>
      </div>
    </InsightCard>
  );
}

function BarLine({ label, value, max, color, currency }) {
  return (
    <div>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
        marginBottom: 4,
      }}>
        <span style={{
          fontFamily: 'Geist, system-ui', fontSize: 12,
          color: 'var(--ink-2)', fontWeight: 500,
        }}>{label}</span>
        <span style={{
          fontFamily: 'Geist Mono, ui-monospace, monospace',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 13, color: 'var(--ink)', fontWeight: 500,
        }}>
          <span style={{ opacity: 0.5, fontSize: 11, marginRight: 1 }}>{currency}</span>
          {fmt(value)}
        </span>
      </div>
      <div style={{
        height: 8, background: 'var(--rule)', borderRadius: 4, overflow: 'hidden',
      }}>
        <div style={{
          height: '100%', width: `${(value / max) * 100}%`, background: color,
          borderRadius: 4, transition: 'width 240ms ease',
        }} />
      </div>
    </div>
  );
}

// ── 2. Daily spend trend (sparkline + stats) ─────────────────
function DailyTrendCard({ currency }) {
  // Build daily expense totals across the month
  const days = 30; // April has 30 days
  const totals = Array.from({ length: days }, () => 0);
  for (const t of TX) {
    if (t.type !== 'EXPENSE') continue;
    const d = parseInt(t.date.slice(8, 10), 10) - 1;
    if (d >= 0 && d < days) totals[d] += t.amount;
  }
  const today = 18; // index for Apr 19
  const visible = totals.slice(0, today + 1);
  const avg = visible.reduce((s, v) => s + v, 0) / Math.max(1, visible.filter(v => v > 0).length);
  const max = Math.max(...visible, 1);
  const peakIdx = visible.indexOf(Math.max(...visible));

  // sparkline path
  const w = 280, h = 60;
  const stepX = w / (days - 1);
  const pts = totals.map((v, i) => {
    const x = i * stepX;
    const y = h - (v / max) * (h - 4) - 2;
    return [x, y];
  });
  const linePath = pts.slice(0, today + 1)
    .map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(' ');
  const areaPath = linePath + ` L${pts[today][0]},${h} L0,${h} Z`;

  return (
    <InsightCard
      title="Daily spend trend"
      caption={`Avg ${currency}${fmt(Math.round(avg))} / day · 19 days in`}
    >
      <div style={{ position: 'relative', marginTop: 10 }}>
        <svg width="100%" height={h} viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none">
          <defs>
            <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%"  stopColor="var(--neg)" stopOpacity="0.22" />
              <stop offset="100%" stopColor="var(--neg)" stopOpacity="0" />
            </linearGradient>
          </defs>
          {/* avg dotted line */}
          <line x1="0" y1={h - (avg / max) * (h - 4) - 2}
                x2={w} y2={h - (avg / max) * (h - 4) - 2}
                stroke="var(--ink-4)" strokeWidth="1" strokeDasharray="3 3" />
          <path d={areaPath} fill="url(#trendFill)" />
          <path d={linePath} fill="none" stroke="var(--neg)" strokeWidth="1.6"
            strokeLinecap="round" strokeLinejoin="round" />
          {/* peak dot */}
          <circle cx={pts[peakIdx][0]} cy={pts[peakIdx][1]} r="3.5"
            fill="var(--paper)" stroke="var(--neg)" strokeWidth="1.5" />
          {/* today dot */}
          <circle cx={pts[today][0]} cy={pts[today][1]} r="3"
            fill="var(--ink)" />
        </svg>

        {/* x-axis labels */}
        <div style={{
          display: 'flex', justifyContent: 'space-between',
          marginTop: 4,
          fontFamily: 'Geist Mono, ui-monospace, monospace',
          fontSize: 9.5, color: 'var(--ink-4)',
        }}>
          <span>Apr 1</span>
          <span>Apr 10</span>
          <span>Apr 19</span>
          <span style={{ opacity: 0.5 }}>Apr 30</span>
        </div>
      </div>

      <div style={{
        display: 'flex', gap: 12, marginTop: 12, paddingTop: 10,
        borderTop: '1px solid var(--rule)',
      }}>
        <MiniStat label="Peak day" value={`Apr ${peakIdx + 1}`}
          sub={`${currency}${fmt(Math.round(visible[peakIdx]))}`} />
        <Sep />
        <MiniStat label="Quietest" value="Apr 11"
          sub={`${currency}0`} />
        <Sep />
        <MiniStat label="Today" value={`${currency}${fmt(Math.round(totals[today]))}`}
          sub={totals[today] > avg ? 'above avg' : 'below avg'}
          color={totals[today] > avg ? 'var(--neg)' : 'var(--pos)'} />
      </div>
    </InsightCard>
  );
}

// ── 3. Highlights — biggest, frequent, jump ──────────────────
function HighlightsCard({ currency }) {
  // Biggest single expense
  const expenses = TX.filter(t => t.type === 'EXPENSE');
  const biggest = expenses.reduce((a, b) => b.amount > a.amount ? b : a, expenses[0]);
  const bigCat = catById(biggest.catId);
  const BigI = Ic[bigCat.icon] || Ic.dots;

  // Most frequent category
  const freq = {};
  for (const t of expenses) freq[t.catId] = (freq[t.catId] || 0) + 1;
  const topId = Object.entries(freq).sort((a, b) => b[1] - a[1])[0];
  const freqCat = catById(parseInt(topId[0], 10));
  const FreqI = Ic[freqCat.icon] || Ic.dots;

  // Top sub within that category
  const subs = {};
  for (const t of expenses.filter(t => t.catId === freqCat.id)) {
    subs[t.subId] = (subs[t.subId] || 0) + 1;
  }
  const topSub = Object.entries(subs).sort((a, b) => b[1] - a[1])[0]?.[0];

  return (
    <InsightCard title="Highlights" caption="What stood out this month">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 8 }}>
        <HighlightRow
          icon={BigI} color={bigCat.color}
          label="Biggest single expense"
          primary={`${currency}${fmt(biggest.amount)}`}
          sub={`${bigCat.name} · ${biggest.subId} · Apr ${parseInt(biggest.date.slice(8,10),10)}`}
        />
        <HighlightRow
          icon={FreqI} color={freqCat.color}
          label="Most frequent"
          primary={freqCat.name}
          sub={`${topId[1]} transactions${topSub ? ` · top: ${topSub}` : ''}`}
        />
        <HighlightRow
          icon={Ic.trending} color="oklch(0.52 0.13 35)"
          label="Biggest jump vs March"
          primary="Housing"
          sub={`+${currency}3,200 · electricity & internet up`}
          arrowUp
        />
      </div>
    </InsightCard>
  );
}

function HighlightRow({ icon: I, color, label, primary, sub, arrowUp }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: color + '1c', color,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}><I size={17} /></div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 10.5,
          color: 'var(--ink-3)', letterSpacing: 0.6, textTransform: 'uppercase',
        }}>{label}</div>
        <div style={{
          display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 1,
        }}>
          <span style={{
            fontFamily: 'Newsreader, serif', fontSize: 16,
            color: 'var(--ink)', fontVariantNumeric: 'tabular-nums',
          }}>{primary}</span>
          {arrowUp && (
            <span style={{
              fontFamily: 'Geist, system-ui', fontSize: 10, fontWeight: 600,
              color: 'var(--neg)', letterSpacing: 0.4,
            }}>▲</span>
          )}
        </div>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 11.5,
          color: 'var(--ink-3)', marginTop: 1,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{sub}</div>
      </div>
    </div>
  );
}

// ── 4. Day-of-week heatmap ───────────────────────────────────
function DayOfWeekCard({ currency }) {
  const days = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  // Compute total per weekday
  const totals = [0,0,0,0,0,0,0];
  for (const t of TX.filter(t => t.type === 'EXPENSE')) {
    const d = new Date(t.date + 'T00:00:00').getDay();
    totals[d] += t.amount;
  }
  const max = Math.max(...totals, 1);
  const peakIdx = totals.indexOf(max);

  return (
    <InsightCard
      title="Spending by day of week"
      caption={`Heaviest on ${days[peakIdx]}day`}
    >
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)',
        gap: 6, marginTop: 12, alignItems: 'end',
      }}>
        {totals.map((v, i) => {
          const intensity = v / max;
          const isPeak = i === peakIdx;
          return (
            <div key={i} style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
            }}>
              <span style={{
                fontFamily: 'Geist Mono, ui-monospace, monospace',
                fontSize: 9.5, color: isPeak ? 'var(--neg)' : 'var(--ink-4)',
                fontVariantNumeric: 'tabular-nums', fontWeight: isPeak ? 600 : 400,
              }}>{v > 0 ? fmt(Math.round(v / 1000)) + 'k' : '—'}</span>
              <div style={{
                width: '100%', maxWidth: 28, aspectRatio: '1',
                borderRadius: 8,
                background: v === 0
                  ? 'var(--rule)'
                  : `oklch(${(0.92 - intensity * 0.5).toFixed(3)} ${(0.04 + intensity * 0.10).toFixed(3)} 35)`,
                border: isPeak ? '1.5px solid var(--neg)' : '1px solid var(--rule)',
                boxSizing: 'border-box',
              }} />
              <span style={{
                fontFamily: 'Geist, system-ui', fontSize: 10,
                color: isPeak ? 'var(--ink)' : 'var(--ink-3)',
                fontWeight: isPeak ? 600 : 500, letterSpacing: 0.3,
              }}>{days[i]}</span>
            </div>
          );
        })}
      </div>
    </InsightCard>
  );
}

// ── shared chrome ────────────────────────────────────────────
function InsightCard({ title, caption, children }) {
  return (
    <div style={{
      margin: '10px 16px',
      padding: '14px 16px 16px',
      background: 'var(--card)',
      border: '1px solid var(--rule)',
      borderRadius: 18,
    }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <span style={{
          fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
          color: 'var(--ink)', letterSpacing: 0.1,
        }}>{title}</span>
        {caption && (
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 11.5,
            color: 'var(--ink-3)',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            minWidth: 0, textAlign: 'right',
          }}>{caption}</span>
        )}
      </div>
      {children}
    </div>
  );
}

function MiniStat({ label, value, sub, color = 'var(--ink)' }) {
  return (
    <div style={{ flex: 1, minWidth: 0 }}>
      <div style={{
        fontFamily: 'Geist, system-ui', fontSize: 9.5,
        color: 'var(--ink-3)', letterSpacing: 0.8, textTransform: 'uppercase',
      }}>{label}</div>
      <div style={{
        fontFamily: 'Newsreader, serif', fontSize: 15, color,
        fontVariantNumeric: 'tabular-nums', marginTop: 1, lineHeight: 1.1,
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
      }}>{value}</div>
      {sub && (
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 10.5,
          color: 'var(--ink-3)', marginTop: 1,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{sub}</div>
      )}
    </div>
  );
}

function Sep() {
  return <div style={{ width: 1, alignSelf: 'stretch', background: 'var(--rule)' }} />;
}

Object.assign(window, {
  IncomeExpenseBar, DailyTrendCard, HighlightsCard, DayOfWeekCard,
  InsightCard, MiniStat,
});
