// FinnFlow — mock data + shared constants
// BDT / paper-ledger aesthetic

const CURRENCY_DEFAULT = '৳';

// ── Categories (matches SeedData.kt) ─────────────────────────
const EXPENSE_CATS = [
  { id: 1,  name: 'Food & Dining',   color: '#C44536', icon: 'utensils',    subs: ['Restaurant','Groceries','Coffee','Fast Food','Bakery','Street Food'] },
  { id: 2,  name: 'Transportation',  color: '#3A6EA5', icon: 'car',         subs: ['Bus','Train','CNG / Rickshaw','Ride Share','Fuel','Parking','Taxi'] },
  { id: 3,  name: 'Housing',         color: '#7A5C3E', icon: 'home',        subs: ['Rent','Electricity','Water','Gas','Internet','Maintenance','Furniture'] },
  { id: 4,  name: 'Health',          color: '#B5456E', icon: 'heart',       subs: ['Doctor','Medicine','Hospital','Lab Tests','Dental','Pharmacy','Gym'] },
  { id: 5,  name: 'Education',       color: '#7A4FA0', icon: 'book',        subs: ['Tuition','Books','Stationery','Coaching','Online Course','Exam Fee'] },
  { id: 6,  name: 'Shopping',        color: '#D18842', icon: 'bag',         subs: ['Clothing','Electronics','Footwear','Accessories','Home Goods','Beauty'] },
  { id: 7,  name: 'Entertainment',   color: '#2E8B94', icon: 'film',        subs: ['Movies','Streaming','Games','Books','Music','Sports','Events'] },
  { id: 8,  name: 'Communication',   color: '#556B74', icon: 'phone',       subs: ['Mobile Recharge','Internet Pack','Phone Bill'] },
  { id: 9,  name: 'Personal Care',   color: '#B85A3E', icon: 'sparkle',     subs: ['Haircut','Grooming','Toiletries','Cosmetics'] },
  { id: 10, name: 'Family & Gifts',  color: '#6E8A4A', icon: 'gift',        subs: ['Gifts','Donations','Family Support','Charity'] },
  { id: 11, name: 'Finance',         color: '#3E4A8A', icon: 'bank',        subs: ['Loan Repayment','Insurance','Bank Charges','Tax','Investment'] },
  { id: 12, name: 'Other Expense',   color: '#8A8A8A', icon: 'dots',        subs: ['Miscellaneous'] },
];

const INCOME_CATS = [
  { id: 101, name: 'Salary',             color: '#4A8A5C', icon: 'wallet',    subs: ['Monthly Salary','Bonus','Overtime','Allowance'] },
  { id: 102, name: 'Business',           color: '#2E8B94', icon: 'briefcase', subs: ['Sales','Service Revenue','Commission','Consulting'] },
  { id: 103, name: 'Freelance',          color: '#3A6EA5', icon: 'laptop',    subs: ['Project Payment','Part-time Work','Online Work'] },
  { id: 104, name: 'Investment Returns', color: '#6E8A4A', icon: 'trending',  subs: ['Dividends','Interest','Capital Gains','Rental Income'] },
  { id: 105, name: 'Other Income',       color: '#8A8A8A', icon: 'dots',      subs: ['Gift Received','Refund','Miscellaneous'] },
];

const ALL_CATS = [...EXPENSE_CATS, ...INCOME_CATS];
const catById = (id) => ALL_CATS.find(c => c.id === id);

// ── Transactions (current month: April 2026) ────────────────
// date format: 'YYYY-MM-DD'
const TX = [
  // Today — Apr 19
  { id: 1,  date: '2026-04-19', type: 'EXPENSE', amount: 320,   catId: 1,  subId: 'Coffee',        note: 'Morning coffee at North End' },
  { id: 2,  date: '2026-04-19', type: 'EXPENSE', amount: 1450,  catId: 1,  subId: 'Groceries',     note: 'Agora — weekly shop' },
  { id: 3,  date: '2026-04-19', type: 'EXPENSE', amount: 180,   catId: 2,  subId: 'CNG / Rickshaw',note: 'Home to office' },

  // Apr 18
  { id: 4,  date: '2026-04-18', type: 'EXPENSE', amount: 2200,  catId: 6,  subId: 'Clothing',      note: 'Spring shirt, Aarong' },
  { id: 5,  date: '2026-04-18', type: 'EXPENSE', amount: 650,   catId: 1,  subId: 'Restaurant',    note: 'Dinner with Rafi' },

  // Apr 17
  { id: 6,  date: '2026-04-17', type: 'INCOME',  amount: 18000, catId: 103,subId: 'Project Payment', note: 'Landing page client' },
  { id: 7,  date: '2026-04-17', type: 'EXPENSE', amount: 240,   catId: 8,  subId: 'Mobile Recharge', note: 'Grameenphone' },

  // Apr 15
  { id: 8,  date: '2026-04-15', type: 'EXPENSE', amount: 4500,  catId: 3,  subId: 'Electricity',   note: 'DESCO — March bill' },
  { id: 9,  date: '2026-04-15', type: 'EXPENSE', amount: 1200,  catId: 3,  subId: 'Internet',      note: 'Link3 monthly' },

  // Apr 14
  { id: 10, date: '2026-04-14', type: 'EXPENSE', amount: 560,   catId: 7,  subId: 'Movies',        note: 'Star Cineplex — 2 tickets' },
  { id: 11, date: '2026-04-14', type: 'EXPENSE', amount: 420,   catId: 1,  subId: 'Fast Food',     note: 'KFC' },

  // Apr 12
  { id: 12, date: '2026-04-12', type: 'EXPENSE', amount: 3800,  catId: 4,  subId: 'Doctor',        note: 'Check-up + labs' },

  // Apr 10
  { id: 13, date: '2026-04-10', type: 'EXPENSE', amount: 18000, catId: 3,  subId: 'Rent',          note: 'April rent' },
  { id: 14, date: '2026-04-10', type: 'EXPENSE', amount: 200,   catId: 1,  subId: 'Coffee',        note: '' },

  // Apr 8
  { id: 15, date: '2026-04-08', type: 'EXPENSE', amount: 780,   catId: 2,  subId: 'Fuel',          note: 'Padma filling' },
  { id: 16, date: '2026-04-08', type: 'INCOME',  amount: 3500,  catId: 104,subId: 'Dividends',     note: 'DSE — Q1' },

  // Apr 5
  { id: 17, date: '2026-04-05', type: 'EXPENSE', amount: 950,   catId: 1,  subId: 'Groceries',     note: 'Meena Bazar' },
  { id: 18, date: '2026-04-05', type: 'EXPENSE', amount: 1500,  catId: 10, subId: 'Gifts',         note: 'Sister birthday' },

  // Apr 3
  { id: 19, date: '2026-04-03', type: 'EXPENSE', amount: 2400,  catId: 5,  subId: 'Online Course', note: 'Design Systems, Frontend Masters' },
  { id: 20, date: '2026-04-03', type: 'EXPENSE', amount: 340,   catId: 9,  subId: 'Haircut',       note: '' },

  // Apr 1 — payday
  { id: 21, date: '2026-04-01', type: 'INCOME',  amount: 65000, catId: 101,subId: 'Monthly Salary',note: 'April salary' },
  { id: 22, date: '2026-04-01', type: 'EXPENSE', amount: 880,   catId: 11, subId: 'Bank Charges',  note: 'Annual card fee' },
];

// Precompute: by-day groups for April 2026
function groupByDate(list) {
  const m = {};
  for (const t of list) (m[t.date] ||= []).push(t);
  return Object.entries(m).sort((a,b) => b[0].localeCompare(a[0]));
}

function sumBy(list, type) {
  return list.filter(t => t.type === type).reduce((s,t) => s+t.amount, 0);
}

// Stats aggregation for pie/list by category
function aggregateByCategory(list, type) {
  const map = new Map();
  for (const t of list) {
    if (t.type !== type) continue;
    const cat = catById(t.catId);
    if (!cat) continue;
    const cur = map.get(cat.id) || { cat, total: 0, count: 0 };
    cur.total += t.amount;
    cur.count += 1;
    map.set(cat.id, cur);
  }
  return [...map.values()].sort((a,b) => b.total - a.total);
}

function aggregateBySubCategory(list, catId) {
  const map = new Map();
  for (const t of list) {
    if (t.catId !== catId) continue;
    const key = t.subId || 'Uncategorized';
    const cur = map.get(key) || { name: key, total: 0, count: 0 };
    cur.total += t.amount;
    cur.count += 1;
    map.set(key, cur);
  }
  return [...map.values()].sort((a,b) => b.total - a.total);
}

// ── Number formatting (BDT-style comma grouping) ─────────────
function fmt(n, opts = {}) {
  const { dec = 0, sign = false } = opts;
  const abs = Math.abs(n);
  const s = abs.toLocaleString('en-IN', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  if (sign) return (n >= 0 ? '+' : '−') + s;
  return (n < 0 ? '−' : '') + s;
}

Object.assign(window, {
  CURRENCY_DEFAULT,
  EXPENSE_CATS, INCOME_CATS, ALL_CATS, catById,
  TX, groupByDate, sumBy, aggregateByCategory, aggregateBySubCategory,
  fmt,
});
