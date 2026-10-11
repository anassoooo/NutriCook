/** @type {import('tailwindcss').Config} */
// NutriCook design tokens: "the market plate". Colour is reserved for food data
// (the four ingredient colours); everything else is aubergine ink on chalk.
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        chalk: '#F3F5F1',
        paper: '#FFFFFF',
        ink: {
          DEFAULT: '#2B1E33', // aubergine
          soft: '#5E5466',
          faint: '#8E8795',
        },
        line: '#D9DED3',
        wash: '#E8ECE4',
        // food roles and macros (BR-CMP-02): one colour per role, everywhere
        harissa: { DEFAULT: '#C2362B', wash: '#F6E1DE' }, // protein
        semolina: { DEFAULT: '#D49B16', wash: '#F7EDD2' }, // carbs
        cucumber: { DEFAULT: '#2F8C6B', wash: '#DCEEE6' }, // fruit and vegetables
        olive: { DEFAULT: '#8A8A1F', wash: '#EDEDD3' }, // fat
        // states
        alert: { DEFAULT: '#B4232A', wash: '#FBE7E6' },
        caution: { DEFAULT: '#8A5A00', wash: '#FFF3D6' },
        // kept for older class names; maps onto the aubergine ink
        brand: {
          50: '#F4F1F6', 100: '#E9E3EE', 200: '#D3C7DC', 300: '#B3A1C0',
          400: '#8B739B', 500: '#5E4A6E', 600: '#43334F', 700: '#2B1E33',
          800: '#21172A', 900: '#18101F', 950: '#100A15',
        },
      },
      fontFamily: {
        sans: ['"Instrument Sans"', 'system-ui', 'sans-serif'],
        heading: ['"Bricolage Grotesque"', '"Instrument Sans"', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        ctl: '10px', // controls
        panel: '18px', // the few surfaces that are panels
      },
      boxShadow: {
        lift: '0 1px 0 rgba(43,30,51,0.04), 0 8px 24px -16px rgba(43,30,51,0.35)',
      },
      keyframes: {
        rise: { '0%': { opacity: '0', transform: 'translateY(6px)' }, '100%': { opacity: '1', transform: 'none' } },
      },
      animation: {
        'fade-in': 'rise 0.25s ease-out both',
        rise: 'rise 0.25s ease-out both',
      },
    },
  },
  plugins: [],
}
