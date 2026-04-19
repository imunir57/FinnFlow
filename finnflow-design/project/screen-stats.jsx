// FinnFlow — Stats + Category Stats screens
// Donut chart, income/expense toggle, month/year/custom range selector
// List of categories below chart

function Segmented({ value, onChange, items, small = false }) {
  return (
    <div style={{
      display: 'inline-flex', padding: 3,
      background: 'var(--ink-wash)', borderRadius: 999,
    }}>
      {items.map(it => {
        const active = it.id === value;
        return (
          <button key={it.id} onClick={() => onChange(it.id)}
            style={{
              border: 0, cursor: 'pointer',
              padding: small ? '6px 14px' : '8px 18px',
              fontFamily: 'Geist, system-ui', fontSize: small ? 12 : 13,
              fontWeight: active ? 600 : 500,
              color: active ? 'var(--paper)' : 'var(--ink-2)',
              background: active ? 'var(--ink)' : 'transparent',
              borderRadius: 999,
              letterSpacing: 0.1,
              transition: 'background 120ms, color 120ms',
            }}>{it.label}</button>
        );
      })}
    </div>
  );
}

function RangeTabs({ value, onChange }) {
  const items = [
    { id: 'month', label: 'Month' },
    { id: 'year',  label: 'Year' },
    { id: 'custom',label: 'Custom' },
  ];
  return (
    <div style={{
      display: 'flex', alignItems: 'center',
      borderBottom: '1px solid var(--rule)',
      padding: '0 18px',
      gap: 22,
    }}>
      {items.map(it => {
        const active = it.id === value;
        return (
          <button key={it.id} onClick={() => onChange(it.id)}
            style={{
              border: 0, background: 'none', cursor: 'pointer',
              padding: '14px 0 12px',
              fontFamily: 'Geist, system-ui',
              fontSize: 13, fontWeight: active ? 600 : 500,
              color: active ? 'var(--ink)' : 'var(--ink-3)',
              borderBottom: active ? '2px solid var(--ink)' : '2px solid transparent',
              marginBottom: -1,
              letterSpacing: 0.1,
            }}>{it.label}</button>
        );
      })}
    </div>
  );
}

function RangeDisplay({ range }) {
  const label =
    range === 'month'  ? 'April 2026'
    : range === 'year' ? '2026'
    :                    'Mar 15 – Apr 19, 2026';
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 18px 6px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Ic.chevL size={16} />
        <span style={{
          fontFamily: 'Geist, system-ui', fontSize: 14, fontWeight: 600,
          color: 'var(--ink)',
        }}>{label}</span>
        <Ic.chevR size={16} />
      </div>
      <button style={{
        border: '1px solid var(--rule)', background: 'transparent',
        padding: '5px 10px', borderRadius: 999,
        fontFamily: 'Geist, system-ui', fontSize: 11,
        color: 'var(--ink-2)', letterSpacing: 0.3, cursor: 'pointer',
        display: 'flex', alignItems: 'center', gap: 5,
      }}>
        <Ic.calendar size={13} />
        Pick
      </button>
    </div>
  );
}

// Donut chart — segmented by category, with big % labels inside each slice
function Donut({ data, size = 220, stroke = 34, currency, total, label, centerColor = 'var(--ink)' }) {
  const r  = (size - stroke) / 2;
  const cx = size / 2, cy = size / 2;
  const C  = 2 * Math.PI * r;
  const sumAll = data.reduce((s, d) => s + d.total, 0) || 1;
  let offset = 0;
  const gap = 2;

  // Position % labels on the ring at each slice's midpoint
  const labelPositions = (() => {
    let acc = 0;
    return data.map(d => {
      const frac = d.total / sumAll;
      const mid  = acc + frac / 2; // 0..1 around the circle
      acc += frac;
      // rotate -90deg (so 0 = top)
      const angle = mid * 2 * Math.PI - Math.PI / 2;
      const x = cx + r * Math.cos(angle);
      const y = cy + r * Math.sin(angle);
      return { x, y, frac };
    });
  })();

  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size}>
        <g style={{ transform: 'rotate(-90deg)', transformOrigin: `${cx}px ${cy}px` }}>
          <circle cx={cx} cy={cy} r={r} fill="none"
            stroke="var(--rule)" strokeWidth={stroke} />
          {data.map((d, i) => {
            const frac = d.total / sumAll;
            const len  = Math.max(0, frac * C - gap);
            const el = (
              <circle key={i} cx={cx} cy={cy} r={r} fill="none"
                stroke={d.cat.color} strokeWidth={stroke}
                strokeDasharray={`${len} ${C - len}`}
                strokeDashoffset={-offset}
                strokeLinecap="butt" />
            );
            offset += frac * C;
            return el;
          })}
        </g>
        {/* % labels inside each slice */}
        {data.map((d, i) => {
          const p = labelPositions[i];
          const pct = p.frac * 100;
          if (pct < 4) return null; // too small to label
          return (
            <text key={`pl-${i}`} x={p.x} y={p.y}
              textAnchor="middle" dominantBaseline="central"
              fill="#fff"
              style={{
                fontFamily: 'Geist, system-ui',
                fontSize: pct < 8 ? 10 : pct < 14 ? 12 : 13,
                fontWeight: 700,
              }}>
              {pct.toFixed(0)}%
            </text>
          );
        })}
      </svg>
      {/* center label — now smaller */}
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
      }}>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 9.5,
          color: 'var(--ink-3)', letterSpacing: 0.8, textTransform: 'uppercase',
          marginBottom: 1,
        }}>{label}</div>
        <div style={{
          fontFamily: 'Newsreader, serif',
          fontVariantNumeric: 'tabular-nums',
          fontSize: 18, color: centerColor, letterSpacing: -0.2, lineHeight: 1.05,
        }}>
          <span style={{ opacity: 0.5, fontSize: 11, marginRight: 2 }}>{currency}</span>
          {fmt(total)}
        </div>
      </div>
    </div>
  );
}

// Color-dot + name legend for the donut
function DonutLegend({ data, total, currency }) {
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: '6px 14px',
      padding: '10px 18px 14px',
    }}>
      {data.map((d, i) => {
        const pct = (d.total / (total || 1)) * 100;
        const name = d.cat.name || d.name || '—';
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
            <span style={{
              width: 10, height: 10, borderRadius: 3, background: d.cat.color, flexShrink: 0,
            }} />
            <span style={{
              flex: 1, minWidth: 0,
              fontFamily: 'Geist, system-ui', fontSize: 11.5,
              color: 'var(--ink-2)',
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>{name}</span>
            <span style={{
              fontFamily: 'Geist Mono, ui-monospace, monospace',
              fontSize: 11, color: 'var(--ink-3)', fontVariantNumeric: 'tabular-nums',
            }}>{pct.toFixed(1)}%</span>
          </div>
        );
      })}
    </div>
  );
}

// Category/subcategory row in stats list
function StatsRow({ color, icon, name, count, amount, percent, currency, onClick, progressTrack = true }) {
  const I = icon ? (Ic[icon] || Ic.dots) : null;
  return (
    <button onClick={onClick} style={{
      width: '100%', background: 'none', border: 0,
      padding: '12px 18px', cursor: 'pointer', textAlign: 'left',
      display: 'flex', flexDirection: 'column', gap: 8,
      borderBottom: '1px solid var(--rule)',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
        {I && (
          <div style={{
            width: 34, height: 34, borderRadius: 10,
            background: color + '1a', color,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}><I size={17} /></div>
        )}
        {!I && (
          <div style={{
            width: 10, height: 10, borderRadius: '50%', background: color,
            marginLeft: 12, marginRight: 2, flexShrink: 0,
          }} />
        )}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 14.5,
            color: 'var(--ink)', fontWeight: 500,
          }}>{name}</div>
          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 11.5,
            color: 'var(--ink-3)',
          }}>{count} {count === 1 ? 'txn' : 'txns'} · {percent.toFixed(1)}%</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{
            fontFamily: 'Geist Mono, ui-monospace, monospace',
            fontVariantNumeric: 'tabular-nums',
            fontSize: 14.5, color: 'var(--ink)', fontWeight: 500,
          }}>
            <span style={{ opacity: 0.5, marginRight: 2, fontSize: 12 }}>{currency}</span>
            {fmt(amount)}
          </div>
        </div>
      </div>
      {progressTrack && (
        <div style={{
          height: 3, background: 'var(--rule)', borderRadius: 2,
          width: '100%', overflow: 'hidden', marginLeft: 0,
        }}>
          <div style={{
            height: '100%', width: percent + '%', background: color,
            borderRadius: 2,
          }} />
        </div>
      )}
    </button>
  );
}

function StatsScreen({ currency, onOpenCategory, onBack }) {
  const [range, setRange] = React.useState('month');
  const [type,  setType]  = React.useState('EXPENSE');

  const agg   = aggregateByCategory(TX, type);
  const total = agg.reduce((s, a) => s + a.total, 0);

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      {/* Custom top bar with title */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 18px 8px',
      }}>
        <div style={{
          fontFamily: 'Newsreader, ui-serif, Georgia, serif',
          fontSize: 26, color: 'var(--ink)', letterSpacing: -0.3,
        }}>Statistics</div>
        <IconButton><Ic.dots size={20} /></IconButton>
      </div>

      <RangeTabs value={range} onChange={setRange} />
      <RangeDisplay range={range} />

      {/* Income/Expense toggle */}
      <div style={{ padding: '8px 18px 12px', display: 'flex', justifyContent: 'center' }}>
        <Segmented
          value={type}
          onChange={setType}
          items={[
            { id: 'EXPENSE', label: 'Expense' },
            { id: 'INCOME',  label: 'Income' },
          ]}
        />
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        {/* Donut + legend */}
        <div style={{ display: 'flex', justifyContent: 'center', padding: '4px 0 8px' }}>
          <Donut
            data={agg} currency={currency} total={total}
            label={type === 'EXPENSE' ? 'Total out' : 'Total in'}
            centerColor={type === 'EXPENSE' ? 'var(--ink)' : 'var(--pos)'}
          />
        </div>
        <DonutLegend data={agg} total={total} currency={currency} />

        {/* Category list */}
        <div style={{ padding: '0 0 8px', borderTop: '1px solid var(--rule)' }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between',
            padding: '0 18px 10px',
            fontFamily: 'Geist, system-ui', fontSize: 10,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>
            <span>Category</span>
            <span>Amount</span>
          </div>
          {agg.map(a => (
            <StatsRow key={a.cat.id}
              color={a.cat.color} icon={a.cat.icon}
              name={a.cat.name} count={a.count}
              amount={a.total}
              percent={(a.total / (total || 1)) * 100}
              currency={currency}
              onClick={() => onOpenCategory?.(a.cat)}
            />
          ))}
        </div>
        <div style={{ height: 20 }} />
      </div>
    </div>
  );
}

// ── Category detail (sub-category breakdown) ─────────────
function CategoryStatsScreen({ category, currency, onBack }) {
  const [range, setRange] = React.useState('month');

  const agg = aggregateBySubCategory(
    TX.filter(t => t.catId === category.id), category.id
  );
  const total = agg.reduce((s, a) => s + a.total, 0);
  const count = agg.reduce((s, a) => s + a.count, 0);
  const I = Ic[category.icon] || Ic.dots;

  // fake sub-colors as tints of the category color
  const shadeFor = (i, n) => {
    const lightness = 0.35 + (i / Math.max(1, n - 1)) * 0.25;
    return `oklch(${lightness.toFixed(3)} 0.08 ${hueFromHex(category.color)})`;
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      {/* Top bar: back + category name */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 12px 8px',
      }}>
        <IconButton onClick={onBack}><Ic.back size={20} /></IconButton>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <div style={{
            width: 26, height: 26, borderRadius: 8,
            background: category.color + '22',
            color: category.color,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I size={15} /></div>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 15, fontWeight: 600,
            color: 'var(--ink)',
          }}>{category.name}</span>
        </div>
        <IconButton><Ic.dots size={20} /></IconButton>
      </div>

      <RangeTabs value={range} onChange={setRange} />
      <RangeDisplay range={range} />

      {/* Summary pill */}
      <div style={{ padding: '4px 18px 10px',
        display: 'flex', justifyContent: 'space-around', alignItems: 'center',
        gap: 8,
      }}>
        <SummaryCell label="Spent" currency={currency} value={total} />
        <div style={{ width: 1, height: 28, background: 'var(--rule)' }} />
        <SummaryCell label="Transactions" value={count} plain />
        <div style={{ width: 1, height: 28, background: 'var(--rule)' }} />
        <SummaryCell label="Avg / txn" currency={currency} value={Math.round(total/Math.max(1,count))} />
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        {/* Donut + legend by subcategory */}
        <div style={{ display: 'flex', justifyContent: 'center', padding: '4px 0 8px' }}>
          <Donut
            data={agg.map((a, i) => ({ total: a.total, cat: { color: shadeFor(i, agg.length), name: a.name } }))}
            currency={currency} total={total}
            label="Total"
          />
        </div>
        <DonutLegend
          data={agg.map((a, i) => ({ total: a.total, cat: { color: shadeFor(i, agg.length), name: a.name } }))}
          total={total} currency={currency}
        />

        <div style={{
          display: 'flex', justifyContent: 'space-between',
          padding: '12px 18px 10px',
          borderTop: '1px solid var(--rule)',
          fontFamily: 'Geist, system-ui', fontSize: 10,
          color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
        }}>
          <span>Sub-category</span>
          <span>Amount</span>
        </div>

        {agg.map((a, i) => (
          <StatsRow key={a.name}
            color={shadeFor(i, agg.length)}
            name={a.name} count={a.count}
            amount={a.total}
            percent={(a.total / (total || 1)) * 100}
            currency={currency}
          />
        ))}
        <div style={{ height: 20 }} />
      </div>
    </div>
  );
}

function SummaryCell({ label, value, currency, plain }) {
  return (
    <div style={{ textAlign: 'center', flex: 1 }}>
      <div style={{
        fontFamily: 'Geist Mono, ui-monospace, monospace',
        fontVariantNumeric: 'tabular-nums',
        fontSize: 16, color: 'var(--ink)', fontWeight: 500,
      }}>
        {!plain && <span style={{ opacity: 0.5, fontSize: 12, marginRight: 2 }}>{currency}</span>}
        {fmt(value)}
      </div>
      <div style={{
        fontFamily: 'Geist, system-ui', fontSize: 10,
        color: 'var(--ink-3)', letterSpacing: 0.6, textTransform: 'uppercase',
        marginTop: 2,
      }}>{label}</div>
    </div>
  );
}

// Rough hex -> hue for oklch. Just a small lookup from category colors.
function hueFromHex(hex) {
  // Map commonly-used seed colors to decent hues
  const m = {
    '#C44536': 25, '#3A6EA5': 245, '#7A5C3E': 55, '#B5456E': 350,
    '#7A4FA0': 300, '#D18842': 55, '#2E8B94': 195, '#556B74': 230,
    '#B85A3E': 30,  '#6E8A4A': 130,'#3E4A8A': 265,'#8A8A8A': 250,
    '#4A8A5C': 150, '#3A6EA5': 240,
  };
  return m[hex] || 220;
}

Object.assign(window, { StatsScreen, CategoryStatsScreen, Segmented, RangeTabs, Donut, StatsRow });
