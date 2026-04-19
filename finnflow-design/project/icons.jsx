// FinnFlow — minimal line-icon set
// 24x24, stroke-based. Single color via currentColor.

const Ic = {};

function makeIcon(paths, { fill = 'none', strokeWidth = 1.6 } = {}) {
  return ({ size = 22, color = 'currentColor', style = {} }) => (
    <svg width={size} height={size} viewBox="0 0 24 24"
         fill={fill === 'fill' ? color : 'none'}
         stroke={color} strokeWidth={strokeWidth}
         strokeLinecap="round" strokeLinejoin="round"
         style={style}>
      {paths}
    </svg>
  );
}

Ic.home       = makeIcon(<><path d="M3 11 L12 4 L21 11 V20 A1 1 0 0 1 20 21 H15 V14 H9 V21 H4 A1 1 0 0 1 3 20 Z"/></>);
Ic.chart      = makeIcon(<><path d="M4 20 V10"/><path d="M10 20 V4"/><path d="M16 20 V14"/><path d="M22 20 V8"/></>);
Ic.calendar   = makeIcon(<><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10 H21"/><path d="M8 3 V7"/><path d="M16 3 V7"/></>);
Ic.gear       = makeIcon(<><circle cx="12" cy="12" r="3"/><path d="M12 2 V4 M12 20 V22 M4.2 4.2 L5.6 5.6 M18.4 18.4 L19.8 19.8 M2 12 H4 M20 12 H22 M4.2 19.8 L5.6 18.4 M18.4 5.6 L19.8 4.2"/></>);
Ic.plus       = makeIcon(<><path d="M12 5 V19"/><path d="M5 12 H19"/></>, { strokeWidth: 2 });
Ic.dots       = makeIcon(<><circle cx="12" cy="5" r="1.4" fill="currentColor"/><circle cx="12" cy="12" r="1.4" fill="currentColor"/><circle cx="12" cy="19" r="1.4" fill="currentColor"/></>);
Ic.chevL      = makeIcon(<><path d="M15 6 L9 12 L15 18"/></>);
Ic.chevR      = makeIcon(<><path d="M9 6 L15 12 L9 18"/></>);
Ic.chevD      = makeIcon(<><path d="M6 9 L12 15 L18 9"/></>);
Ic.back       = makeIcon(<><path d="M19 12 H5"/><path d="M12 5 L5 12 L12 19"/></>);
Ic.check      = makeIcon(<><path d="M5 12 L10 17 L19 7"/></>, { strokeWidth: 2 });
Ic.x          = makeIcon(<><path d="M6 6 L18 18"/><path d="M18 6 L6 18"/></>);
Ic.pencil     = makeIcon(<><path d="M4 20 L4 16 L16 4 L20 8 L8 20 Z"/><path d="M14 6 L18 10"/></>);
Ic.trash      = makeIcon(<><path d="M4 7 H20"/><path d="M9 7 V4 H15 V7"/><path d="M6 7 L7 20 H17 L18 7"/></>);
Ic.search     = makeIcon(<><circle cx="11" cy="11" r="7"/><path d="M16 16 L21 21"/></>);

// Category glyphs
Ic.utensils   = makeIcon(<><path d="M6 3 V12 A2 2 0 0 0 8 14 V21"/><path d="M6 3 V9 M10 3 V9 M8 3 V14"/><path d="M16 3 C14 5 14 9 16 10 V21"/></>);
Ic.car        = makeIcon(<><path d="M3 14 L5 8 H19 L21 14 V19 H18 V17 H6 V19 H3 Z"/><circle cx="7" cy="15.5" r="1" fill="currentColor"/><circle cx="17" cy="15.5" r="1" fill="currentColor"/></>);
Ic.heart      = makeIcon(<><path d="M12 20 C5 14 3 10 5 7 C7 4 11 5 12 8 C13 5 17 4 19 7 C21 10 19 14 12 20 Z"/></>);
Ic.book       = makeIcon(<><path d="M5 4 H11 C12 4 13 5 13 6 V20 C13 19 12 18 11 18 H5 Z"/><path d="M13 6 C13 5 14 4 15 4 H19 V18 H15 C14 18 13 19 13 20"/></>);
Ic.bag        = makeIcon(<><path d="M5 8 H19 L18 21 H6 Z"/><path d="M9 8 V6 A3 3 0 0 1 15 6 V8"/></>);
Ic.film       = makeIcon(<><rect x="3" y="5" width="18" height="14" rx="1"/><path d="M3 9 H7 M3 15 H7 M17 9 H21 M17 15 H21 M7 5 V19 M17 5 V19"/></>);
Ic.phone      = makeIcon(<><path d="M5 3 H11 L13 7 L10 10 C11 13 13 15 16 16 L19 13 L23 15 V21 C23 21 22 22 21 22 C12 22 2 12 2 3 C2 2 3 1 3 1"/></>);
Ic.sparkle    = makeIcon(<><path d="M12 3 L13 10 L20 11 L13 12 L12 19 L11 12 L4 11 L11 10 Z"/></>);
Ic.gift       = makeIcon(<><rect x="3" y="9" width="18" height="4"/><rect x="4" y="13" width="16" height="8"/><path d="M12 9 V21"/><path d="M12 9 C10 5 6 5 7 9 M12 9 C14 5 18 5 17 9"/></>);
Ic.bank       = makeIcon(<><path d="M3 10 L12 4 L21 10 Z"/><path d="M5 10 V18 M9 10 V18 M15 10 V18 M19 10 V18"/><path d="M3 20 H21"/></>);
Ic.wallet     = makeIcon(<><path d="M3 7 H17 A4 4 0 0 1 21 11 V17 A4 4 0 0 1 17 21 H5 A2 2 0 0 1 3 19 Z"/><path d="M3 7 V5 A2 2 0 0 1 5 3 H15"/><circle cx="17" cy="14" r="1.5" fill="currentColor"/></>);
Ic.briefcase  = makeIcon(<><rect x="3" y="7" width="18" height="13" rx="1"/><path d="M9 7 V5 A1 1 0 0 1 10 4 H14 A1 1 0 0 1 15 5 V7"/><path d="M3 13 H21"/></>);
Ic.laptop     = makeIcon(<><rect x="4" y="5" width="16" height="10" rx="1"/><path d="M2 19 H22"/></>);
Ic.trending   = makeIcon(<><path d="M3 17 L9 11 L13 15 L21 7"/><path d="M15 7 H21 V13"/></>);

Object.assign(window, { Ic });
