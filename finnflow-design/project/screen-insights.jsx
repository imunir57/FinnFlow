// FinnFlow — Insights page
// Dedicated screen of analytical cards. Reached via the "View insights" button on Stats.
// Cards live in screen-stats-insights.jsx; this file is the screen chrome + range tabs.

function InsightsScreen({ currency, onBack }) {
  const [range, setRange] = React.useState('month');

  // Top-level numbers for the hero
  const income  = sumBy(TX, 'INCOME');
  const expense = sumBy(TX, 'EXPENSE');
  const net     = income - expense;
  const savingsRate = income > 0 ? (net / income) * 100 : 0;

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
          color: 'var(--ink)', fontWeight: 400, flex: 1,
        }}>Insights</div>
        <IconButton><Ic.dots size={20} /></IconButton>
      </div>

      {/* Range tabs (re-using Stats' RangeTabs) */}
      <RangeTabs value={range} onChange={setRange} />
      <RangeDisplay range={range} />

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 24 }}>

        {/* Hero — savings rate */}
        <div style={{
          position: 'relative',
          margin: '6px 16px 8px', padding: '20px 22px',
          borderRadius: 22,
          color: 'var(--paper)',
          background: `
            radial-gradient(120% 80% at 0% 0%, oklch(0.42 0.09 155 / 0.55) 0%, transparent 55%),
            radial-gradient(110% 90% at 100% 100%, oklch(0.52 0.13 35 / 0.42) 0%, transparent 55%),
            linear-gradient(160deg, oklch(0.26 0.018 60) 0%, oklch(0.20 0.014 60) 100%)
          `,
          boxShadow: '0 16px 40px oklch(0.22 0.012 60 / 0.28), inset 0 1px 0 oklch(0.98 0.006 85 / 0.08)',
          overflow: 'hidden',
        }}>
          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'oklch(0.98 0.006 85 / 0.65)', letterSpacing: 1.2, textTransform: 'uppercase',
            marginBottom: 4,
          }}>Savings rate · April</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
            <span style={{
              fontFamily: 'Newsreader, serif',
              fontVariantNumeric: 'tabular-nums',
              fontSize: 56, lineHeight: 1, letterSpacing: -1.2,
              color: savingsRate >= 0 ? 'oklch(0.82 0.12 155)' : 'oklch(0.78 0.14 45)',
            }}>{savingsRate >= 0 ? '' : '−'}{Math.abs(savingsRate).toFixed(0)}</span>
            <span style={{
              fontFamily: 'Newsreader, serif', fontSize: 28,
              color: 'oklch(0.98 0.006 85 / 0.7)',
            }}>%</span>
          </div>
          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 12.5,
            color: 'oklch(0.98 0.006 85 / 0.7)', marginTop: 6, lineHeight: 1.4,
          }}>
            {savingsRate >= 30 ? 'Strong month — you held back nearly a third of what came in.' :
             savingsRate >= 10 ? `Net ${currency}${fmt(net)} saved so far. On track for a positive close.` :
             savingsRate >= 0  ? 'Thin margin — most of what you earned went out again.' :
                                 `Overspent by ${currency}${fmt(Math.abs(net))} — review big-ticket items below.`}
          </div>
        </div>

        {/* Insight cards */}
        <IncomeExpenseBar currency={currency} />
        <DailyTrendCard   currency={currency} />
        <HighlightsCard   currency={currency} />
        <DayOfWeekCard    currency={currency} />

        {/* Footer hint */}
        <div style={{
          textAlign: 'center', padding: '14px 24px 4px',
          fontFamily: 'Newsreader, serif', fontStyle: 'italic',
          fontSize: 13, color: 'var(--ink-4)', lineHeight: 1.5,
        }}>Insights are computed from your local transactions only.<br/>Nothing leaves your device.</div>
      </div>
    </div>
  );
}

Object.assign(window, { InsightsScreen });
