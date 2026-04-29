import { Link, NavLink } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

const NAV_LINKS = [
  { label: 'Features',     to: '/features'      },
  { label: 'How it works', to: '/how-it-works'  },
  { label: 'Nutrition AI', to: '/features#nutrition-ai' },
]

const HEADING = ['Fuel your body,', 'shape your future.']

/* ── Framer-motion variants ─────────────────────────── */
const navVariant = {
  hidden: { opacity: 0, y: -12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
}

const charContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.028, delayChildren: 0.25 } },
}

const charItem = {
  hidden: { opacity: 0, x: -16 },
  show: { opacity: 1, x: 0, transition: { duration: 0.45, ease: 'easeOut' } },
}

const fadeUp = (delay = 0) => ({
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.7, ease: 'easeOut', delay } },
})

/* ── Animated heading ───────────────────────────────── */
function AnimatedHeading() {
  return (
    <motion.h1
      variants={charContainer}
      initial="hidden"
      animate="show"
      className="text-5xl md:text-6xl lg:text-7xl text-white mb-5 font-heading font-black"
      style={{ letterSpacing: '-0.02em', lineHeight: 1.08 }}
    >
      {HEADING.map((line, li) => (
        <span key={li} style={{ display: 'block' }}>
          {line.split('').map((char, ci) => (
            <motion.span
              key={ci}
              variants={charItem}
              style={{ display: 'inline-block', whiteSpace: char === ' ' ? 'pre' : 'normal' }}
            >
              {char === ' ' ? ' ' : char}
            </motion.span>
          ))}
        </span>
      ))}
    </motion.h1>
  )
}

/* ── Main component ─────────────────────────────────── */
export default function LandingPage() {
  const { isAuthenticated } = useAuth()

  const ctaTo = isAuthenticated ? '/dashboard' : '/auth'

  return (
    <div className="relative w-full h-screen flex flex-col overflow-hidden bg-black">

      {/* Video background */}
      <video
        autoPlay
        loop
        muted
        playsInline
        style={{
          position: 'absolute', inset: 0,
          width: '100%', height: '100%',
          objectFit: 'cover',
          zIndex: 0,
        }}
      >
        <source src={VIDEO_SRC} type="video/mp4" />
      </video>

      {/* Dark veil so text is always readable */}
      <div className="absolute inset-0 bg-black/30 z-[1]" />

      {/* Content */}
      <div className="relative z-10 flex flex-col h-full px-6 md:px-12 lg:px-16 pt-6">

        {/* Navbar */}
        <motion.nav
          variants={navVariant}
          initial="hidden"
          animate="show"
          className="liquid-glass rounded-xl px-5 py-2.5 flex items-center justify-between"
        >
          {/* Logo */}
          <div className="flex items-center gap-2.5 select-none">
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'linear-gradient(135deg,#16a34a,#0d9488)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(22,163,74,.4)',
            }}>
              <span style={{ color: 'white', fontWeight: 900, fontSize: 13 }}>N</span>
            </div>
            <span className="text-xl font-black tracking-tight text-white"
              style={{ letterSpacing: '-0.02em' }}>
              NutriCook
            </span>
          </div>

          {/* Centre links */}
          <div className="hidden md:flex items-center gap-8">
            {NAV_LINKS.map(({ label, to }) => (
              <NavLink key={to} to={to}
                className="text-sm text-white/80 hover:text-white transition-colors duration-200">
                {label}
              </NavLink>
            ))}
          </div>

          {/* CTA */}
          <Link
            to={ctaTo}
            className="text-white px-6 py-2 rounded-lg text-sm font-semibold hover:opacity-90 transition-opacity duration-200"
            style={{
              background: 'linear-gradient(135deg,#16a34a,#0d9488)',
              boxShadow: '0 2px 8px rgba(22,163,74,.45)',
            }}
          >
            {isAuthenticated ? 'Go to Dashboard →' : 'Get Started Free'}
          </Link>
        </motion.nav>

        {/* Hero content — pinned to bottom */}
        <div className="flex-1 flex flex-col justify-end pb-12 lg:pb-16">
          <div className="lg:grid lg:grid-cols-2 lg:items-end gap-8">

            {/* Left */}
            <div>
              <AnimatedHeading />

              <motion.p
                variants={fadeUp(0.85)}
                initial="hidden"
                animate="show"
                className="text-base md:text-lg text-gray-300 mb-6"
              >
                AI-personalised meal plans, macro tracking, and real nutrition
                insights — built for the way you live.
              </motion.p>

              <motion.div
                variants={fadeUp(1.15)}
                initial="hidden"
                animate="show"
                className="flex flex-wrap gap-4"
              >
                <Link
                  to={ctaTo}
                  className="btn-primary animate-pulse-glow text-lg px-8 py-4"
                >
                  {isAuthenticated ? 'Go to Dashboard →' : 'Generate My Plan ✨'}
                </Link>

                <Link
                  to={isAuthenticated ? '/progress' : '#how-it-works'}
                  className="liquid-glass border border-white/20 px-8 py-3 rounded-lg font-medium text-white
                             hover:bg-white hover:text-black transition-all duration-200"
                >
                  {isAuthenticated ? 'View Progress' : 'See How It Works'}
                </Link>
              </motion.div>
            </div>

            {/* Right — tagline pill */}
            <motion.div
              variants={fadeUp(1.4)}
              initial="hidden"
              animate="show"
              className="flex items-end justify-start lg:justify-end mt-8 lg:mt-0"
            >
              <div className="liquid-glass border border-white/20 px-6 py-3 rounded-xl">
                <span className="text-lg md:text-xl lg:text-2xl font-light text-white">
                  AI Meal Plans. Macro Tracking. Real Results.
                </span>
              </div>
            </motion.div>

          </div>
        </div>
      </div>
    </div>
  )
}
