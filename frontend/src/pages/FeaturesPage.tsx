import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

const FEATURES = [
  {
    icon: '🤖',
    title: 'AI Meal Planning',
    desc: 'Groq-powered AI generates a full weekly meal plan tailored to your goals, dietary preferences, and caloric targets — in seconds.',
    color: '#16a34a',
  },
  {
    icon: '📊',
    title: 'Macro Tracking',
    desc: 'Log calories, protein, carbs, and fats every day. Visual progress rings and trend charts keep you accountable at a glance.',
    color: '#3b82f6',
  },
  {
    icon: '💬',
    title: 'Nutrition AI Chat',
    desc: 'Ask anything about your plan. The AI knows your current meals and progress, so every answer is personalised to you.',
    color: '#8b5cf6',
  },
  {
    icon: '🏆',
    title: 'Achievements',
    desc: 'Earn badges as you hit milestones — first plan generated, 7-day streak, weight goals reached, and many more.',
    color: '#f59e0b',
  },
  {
    icon: '🥗',
    title: 'Dietary Preferences',
    desc: 'Vegan, keto, gluten-free, dairy-free — set your restrictions once and every generated plan respects them automatically.',
    color: '#06b6d4',
  },
  {
    icon: '📍',
    title: 'Find Nearby',
    desc: 'Discover healthy restaurants and grocery stores within 2 km of your location, powered by OpenStreetMap.',
    color: '#ef4444',
  },
]

const charContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.03, delayChildren: 0.2 } },
}
const charItem = {
  hidden: { opacity: 0, x: -14 },
  show: { opacity: 1, x: 0, transition: { duration: 0.4, ease: 'easeOut' } },
}
const fadeUp = (delay = 0) => ({
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.6, ease: 'easeOut', delay } },
})
const cardVariants = {
  hidden: { opacity: 0, y: 24 },
  show: (i: number) => ({
    opacity: 1, y: 0,
    transition: { duration: 0.55, ease: 'easeOut', delay: 0.55 + i * 0.1 },
  }),
}

const HEADING = 'Everything you need to eat smarter.'

export default function FeaturesPage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="relative min-h-screen flex flex-col overflow-x-hidden bg-black">

      {/* Video background */}
      <video
        autoPlay loop muted playsInline
        style={{ position: 'fixed', inset: 0, width: '100%', height: '100%', objectFit: 'cover', zIndex: 0 }}
      >
        <source src={VIDEO_SRC} type="video/mp4" />
      </video>
      <div className="fixed inset-0 bg-black/60 z-[1]" />

      {/* Content */}
      <div className="relative z-10 flex flex-col min-h-screen px-6 md:px-12 lg:px-16">

        {/* Mini navbar */}
        <motion.nav
          initial={{ opacity: 0, y: -12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="liquid-glass rounded-xl px-5 py-2.5 flex items-center justify-between mt-6"
        >
          <Link to="/" className="flex items-center gap-2.5 select-none">
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'linear-gradient(135deg,#16a34a,#0d9488)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(22,163,74,.4)',
            }}>
              <span style={{ color: 'white', fontWeight: 900, fontSize: 13 }}>N</span>
            </div>
            <span className="text-xl font-black tracking-tight text-white" style={{ letterSpacing: '-0.02em' }}>
              NutriCook
            </span>
          </Link>

          <div className="flex items-center gap-3">
            <Link to="/" className="text-sm text-white/75 hover:text-white transition-colors">← Home</Link>
            <Link
              to={isAuthenticated ? '/dashboard' : '/auth'}
              className="text-white px-5 py-2 rounded-lg text-sm font-semibold hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)', boxShadow: '0 2px 8px rgba(22,163,74,.45)' }}
            >
              {isAuthenticated ? 'Dashboard →' : 'Get Started Free'}
            </Link>
          </div>
        </motion.nav>

        {/* Hero heading */}
        <div className="mt-16 mb-10 text-center">
          <motion.h1
            variants={charContainer}
            initial="hidden"
            animate="show"
            className="text-4xl md:text-5xl lg:text-6xl font-black text-white mx-auto"
            style={{ letterSpacing: '-0.02em', lineHeight: 1.1, maxWidth: 720 }}
          >
            {HEADING.split(' ').map((word, wi) => (
              <span key={wi} style={{ display: 'inline-block', whiteSpace: 'nowrap', marginRight: '0.28em' }}>
                {word.split('').map((char, ci) => (
                  <motion.span key={ci} variants={charItem} style={{ display: 'inline-block' }}>
                    {char}
                  </motion.span>
                ))}
              </span>
            ))}
          </motion.h1>

          <motion.p
            variants={fadeUp(0.8)}
            initial="hidden"
            animate="show"
            className="mt-4 text-lg text-white/70 max-w-xl mx-auto"
          >
            One app. Six powerful tools. Built to make nutrition effortless.
          </motion.p>
        </div>

        {/* Feature cards grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 max-w-5xl mx-auto w-full">
          {FEATURES.map((f, i) => (
            <motion.div
              key={f.title}
              custom={i}
              variants={cardVariants}
              initial="hidden"
              animate="show"
              className="liquid-glass rounded-2xl p-6 flex flex-col gap-3 hover:scale-[1.025] transition-transform duration-200"
              style={{ border: `1px solid ${f.color}30` }}
            >
              <div
                className="w-12 h-12 rounded-xl flex items-center justify-center text-2xl shrink-0"
                style={{ background: `${f.color}20` }}
              >
                {f.icon}
              </div>
              <h3 className="text-lg font-bold text-white">{f.title}</h3>
              <p className="text-sm leading-relaxed" style={{ color: 'rgba(255,255,255,0.68)' }}>{f.desc}</p>
            </motion.div>
          ))}
        </div>

        {/* Bottom CTA */}
        <motion.div
          variants={fadeUp(1.4)}
          initial="hidden"
          animate="show"
          className="mt-16 mb-16 text-center"
        >
          <Link
            to={isAuthenticated ? '/dashboard' : '/auth'}
            className="inline-block text-white text-lg font-bold px-10 py-4 rounded-xl hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)', boxShadow: '0 4px 20px rgba(22,163,74,.5)' }}
          >
            {isAuthenticated ? 'Go to Dashboard →' : 'Start for free →'}
          </Link>
          <p className="mt-3 text-sm" style={{ color: 'rgba(255,255,255,0.55)' }}>No credit card required.</p>
        </motion.div>

      </div>
    </div>
  )
}
