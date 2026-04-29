// FinnFlow — Add/Edit Categories + Sub-categories
// Two-screen flow:
//   CategoriesScreen   — list of all categories (Expense / Income tabs), tap a row to manage subs
//   SubCategoriesScreen — list of subs for a category, add/edit/delete
//   AddCategorySheet   — bottom sheet for new/edit, picks name + icon + color + type

const ICON_CHOICES  = ['utensils','car','home','heart','book','bag','film','phone','sparkle','gift','bank','wallet','briefcase','laptop','trending','dots'];
const COLOR_CHOICES = [
  '#C44536','#D18842','#7A5C3E','#6E8A4A','#4A8A5C','#2E8B94',
  '#3A6EA5','#3E4A8A','#7A4FA0','#B5456E','#B85A3E','#556B74','#8A8A8A'
];

function CategoriesScreen({ onBack, onOpenSubs }) {
  const [type, setType] = React.useState('EXPENSE');
  const [editing, setEditing] = React.useState(null);   // null | category | 'new'
  const cats = type === 'EXPENSE' ? EXPENSE_CATS : INCOME_CATS;

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
          flex: 1,
        }}>Categories</div>
        <IconButton onClick={() => setEditing('new')}>
          <Ic.plus size={22} />
        </IconButton>
      </div>

      {/* Type toggle */}
      <div style={{ display: 'flex', justifyContent: 'center', padding: '4px 0 14px' }}>
        <Segmented
          value={type} onChange={setType}
          items={[
            { id: 'EXPENSE', label: `Expense · ${EXPENSE_CATS.length}` },
            { id: 'INCOME',  label: `Income · ${INCOME_CATS.length}` },
          ]}
        />
      </div>

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 80 }}>

        {/* Section header */}
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          padding: '4px 22px 8px',
        }}>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>{cats.length} categories · drag to reorder</span>
          <span style={{
            fontFamily: 'Geist, system-ui', fontSize: 10.5,
            color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
          }}>Subs</span>
        </div>

        {cats.map(c => (
          <CategoryRow key={c.id} cat={c}
            onEdit={() => setEditing(c)}
            onOpenSubs={() => onOpenSubs?.(c)}
          />
        ))}

        <div style={{
          margin: '20px 22px 0',
          padding: '14px 16px',
          border: '1px dashed var(--rule)',
          borderRadius: 14,
          fontFamily: 'Geist, system-ui', fontSize: 12.5,
          color: 'var(--ink-3)',
          lineHeight: 1.45,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--ink-2)', fontWeight: 600, marginBottom: 4 }}>
            <Ic.info size={14} /> About categories
          </div>
          Categories with existing transactions can't be deleted — archive them instead so old records keep their label.
        </div>
      </div>

      {/* FAB to add */}
      <button onClick={() => setEditing('new')} style={{
        position: 'absolute', right: 20, bottom: 20,
        width: 56, height: 56, borderRadius: 18,
        background: 'var(--ink)', color: 'var(--paper)',
        border: 0, cursor: 'pointer',
        boxShadow: '0 10px 28px rgba(42,38,33,0.28)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 5,
      }}><Ic.plus size={22} /></button>

      {/* Edit / new sheet */}
      {editing && (
        <CategoryEditSheet
          cat={editing === 'new' ? null : editing}
          defaultType={type}
          onClose={() => setEditing(null)}
          onSave={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function CategoryRow({ cat, onEdit, onOpenSubs }) {
  const I = Ic[cat.icon] || Ic.dots;
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '10px 22px',
    }}>
      {/* drag affordance */}
      <div style={{
        width: 14, color: 'var(--ink-4)',
        display: 'flex', flexDirection: 'column', gap: 2,
        cursor: 'grab', flexShrink: 0,
      }}>
        <div style={{ height: 1.5, width: 11, background: 'currentColor' }} />
        <div style={{ height: 1.5, width: 11, background: 'currentColor' }} />
        <div style={{ height: 1.5, width: 11, background: 'currentColor' }} />
      </div>

      {/* icon swatch */}
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: cat.color + '1c', color: cat.color,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}><I size={18} /></div>

      <button onClick={onOpenSubs} style={{
        flex: 1, minWidth: 0, textAlign: 'left',
        background: 'transparent', border: 0, cursor: 'pointer', padding: 0,
      }}>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 14.5, fontWeight: 500,
          color: 'var(--ink)',
        }}>{cat.name}</div>
        <div style={{
          fontFamily: 'Geist, system-ui', fontSize: 11.5,
          color: 'var(--ink-3)', marginTop: 2,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{cat.subs.slice(0, 3).join(' · ')}{cat.subs.length > 3 ? ' …' : ''}</div>
      </button>

      <button onClick={onOpenSubs} style={{
        background: 'transparent', border: '1px solid var(--rule)',
        padding: '5px 10px', borderRadius: 999,
        fontFamily: 'Geist Mono, ui-monospace, monospace', fontSize: 11.5,
        color: 'var(--ink-2)', cursor: 'pointer', flexShrink: 0,
      }}>{cat.subs.length}</button>

      <IconButton onClick={onEdit}><Ic.pencil size={16} /></IconButton>
    </div>
  );
}

// ── Sub-categories screen ──────────────────────────────────────────────
function SubCategoriesScreen({ category, onBack }) {
  const [subs, setSubs] = React.useState(category.subs);
  const [draft, setDraft] = React.useState('');
  const [editingIdx, setEditingIdx] = React.useState(-1);
  const [editDraft, setEditDraft] = React.useState('');
  const I = Ic[category.icon] || Ic.dots;

  const addSub = () => {
    const v = draft.trim();
    if (!v) return;
    setSubs([...subs, v]);
    setDraft('');
  };

  return (
    <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', background: 'var(--paper)' }}>
      {/* Top bar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '8px 14px 6px',
      }}>
        <IconButton onClick={onBack}><Ic.back size={20} /></IconButton>
        <div style={{
          width: 32, height: 32, borderRadius: 10,
          background: category.color + '1c', color: category.color,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><I size={16} /></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontFamily: 'Geist, system-ui', fontSize: 11,
            color: 'var(--ink-3)', letterSpacing: 0.6, textTransform: 'uppercase',
          }}>Sub-categories</div>
          <div style={{
            fontFamily: 'Newsreader, serif', fontSize: 20,
            color: 'var(--ink)', lineHeight: 1.1,
          }}>{category.name}</div>
        </div>
      </div>

      {/* Add input */}
      <div style={{
        display: 'flex', gap: 8,
        padding: '10px 18px 14px',
      }}>
        <input value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') addSub(); }}
          placeholder="Add sub-category…"
          style={{
            flex: 1, padding: '12px 14px',
            border: '1px solid var(--rule)',
            borderRadius: 12,
            background: 'var(--card)',
            fontFamily: 'Geist, system-ui', fontSize: 14,
            color: 'var(--ink)', outline: 'none',
          }} />
        <button onClick={addSub}
          disabled={!draft.trim()}
          style={{
            border: 0,
            background: draft.trim() ? 'var(--ink)' : 'var(--ink-wash)',
            color: draft.trim() ? 'var(--paper)' : 'var(--ink-3)',
            padding: '0 18px', borderRadius: 12,
            fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
            cursor: draft.trim() ? 'pointer' : 'default',
            display: 'flex', alignItems: 'center', gap: 4,
          }}><Ic.plus size={14} /> Add</button>
      </div>

      {/* List */}
      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingBottom: 24 }}>
        <div style={{
          padding: '4px 22px 8px',
          fontFamily: 'Geist, system-ui', fontSize: 10.5,
          color: 'var(--ink-3)', letterSpacing: 1, textTransform: 'uppercase',
        }}>{subs.length} sub-categories</div>

        {subs.map((s, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '12px 22px',
            borderBottom: '1px solid var(--rule)',
          }}>
            <div style={{
              width: 6, height: 6, borderRadius: '50%',
              background: category.color, flexShrink: 0,
            }} />
            {editingIdx === i ? (
              <>
                <input value={editDraft} autoFocus
                  onChange={(e) => setEditDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      const next = [...subs]; next[i] = editDraft.trim() || s;
                      setSubs(next); setEditingIdx(-1);
                    }
                    if (e.key === 'Escape') setEditingIdx(-1);
                  }}
                  style={{
                    flex: 1, border: 0, borderBottom: '1.5px solid var(--ink)',
                    background: 'transparent', padding: '4px 2px',
                    fontFamily: 'Geist, system-ui', fontSize: 14,
                    color: 'var(--ink)', outline: 'none',
                  }} />
                <IconButton onClick={() => {
                  const next = [...subs]; next[i] = editDraft.trim() || s;
                  setSubs(next); setEditingIdx(-1);
                }}><Ic.check size={16} /></IconButton>
              </>
            ) : (
              <>
                <span style={{
                  flex: 1,
                  fontFamily: 'Geist, system-ui', fontSize: 14,
                  color: 'var(--ink)',
                }}>{s}</span>
                <IconButton onClick={() => { setEditingIdx(i); setEditDraft(s); }}>
                  <Ic.pencil size={14} />
                </IconButton>
                <IconButton onClick={() => setSubs(subs.filter((_, j) => j !== i))}>
                  <Ic.trash size={14} />
                </IconButton>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Bottom sheet: add / edit category ──────────────────────────────────
function CategoryEditSheet({ cat, defaultType, onClose, onSave }) {
  const [name, setName]   = React.useState(cat?.name || '');
  const [icon, setIcon]   = React.useState(cat?.icon || 'dots');
  const [color, setColor] = React.useState(cat?.color || '#3A6EA5');
  const [type, setType]   = React.useState(
    cat ? (EXPENSE_CATS.find(c => c.id === cat.id) ? 'EXPENSE' : 'INCOME') : defaultType
  );
  const I = Ic[icon] || Ic.dots;

  return (
    <>
      <div onClick={onClose} style={{
        position: 'absolute', inset: 0,
        background: 'rgba(28,24,20,0.45)',
        zIndex: 20,
      }} />
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        background: 'var(--paper)',
        borderTopLeftRadius: 28, borderTopRightRadius: 28,
        zIndex: 21, padding: '10px 18px 22px',
        boxShadow: '0 -8px 30px rgba(28,24,20,0.18)',
        maxHeight: '90%',
        display: 'flex', flexDirection: 'column',
      }}>
        {/* drag handle */}
        <div style={{
          width: 40, height: 4, borderRadius: 2,
          background: 'var(--rule)',
          margin: '0 auto 10px',
        }} />

        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          marginBottom: 14,
        }}>
          {/* preview swatch */}
          <div style={{
            width: 44, height: 44, borderRadius: 12,
            background: color + '22', color,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}><I size={22} /></div>
          <div style={{ flex: 1 }}>
            <div style={{
              fontFamily: 'Newsreader, serif', fontSize: 20,
              color: 'var(--ink)',
            }}>{cat ? 'Edit category' : 'New category'}</div>
            <div style={{
              fontFamily: 'Geist, system-ui', fontSize: 11.5,
              color: 'var(--ink-3)', marginTop: 1,
            }}>Pick an icon and a color</div>
          </div>
          <IconButton onClick={onClose}><Ic.x size={18} /></IconButton>
        </div>

        <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingRight: 2 }}>
          <FieldLabel>Name</FieldLabel>
          <input value={name} onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Subscriptions"
            style={{
              width: '100%', boxSizing: 'border-box',
              padding: '12px 14px',
              border: '1px solid var(--rule)',
              borderRadius: 12, background: 'var(--card)',
              fontFamily: 'Geist, system-ui', fontSize: 14,
              color: 'var(--ink)', outline: 'none',
            }} />

          <FieldLabel style={{ marginTop: 16 }}>Type</FieldLabel>
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <Segmented value={type} onChange={setType}
              items={[
                { id: 'EXPENSE', label: 'Expense' },
                { id: 'INCOME',  label: 'Income' },
              ]}
            />
          </div>

          <FieldLabel style={{ marginTop: 16 }}>Icon</FieldLabel>
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 6,
          }}>
            {ICON_CHOICES.map(ic => {
              const II = Ic[ic] || Ic.dots;
              const active = ic === icon;
              return (
                <button key={ic} onClick={() => setIcon(ic)}
                  style={{
                    aspectRatio: '1', borderRadius: 10,
                    border: active ? '1.5px solid ' + color : '1px solid var(--rule)',
                    background: active ? color + '1c' : 'var(--paper)',
                    color: active ? color : 'var(--ink-2)',
                    cursor: 'pointer',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}><II size={16} /></button>
              );
            })}
          </div>

          <FieldLabel style={{ marginTop: 16 }}>Color</FieldLabel>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {COLOR_CHOICES.map(c => {
              const active = c === color;
              return (
                <button key={c} onClick={() => setColor(c)}
                  style={{
                    width: 32, height: 32, borderRadius: '50%',
                    background: c, border: 0, cursor: 'pointer',
                    boxShadow: active
                      ? '0 0 0 2px var(--paper), 0 0 0 4px ' + c
                      : 'inset 0 0 0 1px rgba(0,0,0,0.1)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: '#fff',
                  }}>{active && <Ic.check size={14} />}</button>
              );
            })}
          </div>

          {cat && (
            <button style={{
              marginTop: 22, padding: '12px',
              width: '100%',
              background: 'transparent',
              border: '1px solid var(--neg)',
              color: 'var(--neg)',
              borderRadius: 12,
              fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            }}>
              <Ic.trash size={14} /> Delete category
            </button>
          )}
        </div>

        <div style={{
          display: 'flex', gap: 8, paddingTop: 12,
          borderTop: '1px solid var(--rule)', marginTop: 8,
        }}>
          <button onClick={onClose} style={{
            flex: 1, padding: '12px',
            background: 'transparent', border: '1px solid var(--rule)',
            color: 'var(--ink-2)', borderRadius: 12,
            fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
            cursor: 'pointer',
          }}>Cancel</button>
          <button onClick={onSave}
            disabled={!name.trim()}
            style={{
              flex: 1, padding: '12px',
              background: name.trim() ? 'var(--ink)' : 'var(--ink-wash)',
              color:      name.trim() ? 'var(--paper)' : 'var(--ink-3)',
              border: 0, borderRadius: 12,
              fontFamily: 'Geist, system-ui', fontSize: 13, fontWeight: 600,
              cursor: name.trim() ? 'pointer' : 'default',
            }}>{cat ? 'Save changes' : 'Create category'}</button>
        </div>
      </div>
    </>
  );
}

Object.assign(window, { CategoriesScreen, SubCategoriesScreen, CategoryEditSheet });
