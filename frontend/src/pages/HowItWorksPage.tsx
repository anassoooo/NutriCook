import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

const STEPS = [
  {
    number: '01',
    title: 'Create your account',
    desc: 'Sign up in seconds with your email. No credit card, no friction — just your email and a password.',
    icon: '👤',
    color: '#16a34a',
  },
  {
    number: '02',
    title: 'Build your profile',
    desc: 'Tell us your age, weight, height, goal (lose/maintain/gain), activity level, and dietary preferences. Takes under 2 minutes.',
    icon: '📋',
    color: '#3b82f6',
  },
  {
    number: '03',
    title: 'Generate your AI plan',
    desc: 'Our Groq-powered AI analyses your profile and instantly generates a complete weekly meal plan with macros, portions, and Tunisian-friendly recipes.',
    icon: '✨',
    color: '#8b5cf6',
  },
  {
    number: '04',
    title: 'Track & improve',
    desc: 'Log your daily progress — weight, calories, water, exercise. Watch your trend charts improve. Earn achievements. Ask the AI to adjust your plan anytime.',
    icon: '📈',
    color: '#f59e0b',
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

const HEADING = 'Simple steps to better nutrition.'

export default function HowItWorksPage() {
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
        <div className="mt-16 mb-14 text-center">
          <motion.h1
            variants={charContainer}
            initial="hidden"
            animate="show"
            className="text-4xl md:text-5xl lg:text-6xl font-black text-white mx-auto"
            style={{ letterSpacing: '-0.02em', lineHeight: 1.1, maxWidth: 680 }}
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
            From sign-up to your first AI meal plan in under 5 minutes.
          </motion.p>
        </div>

        {/* Timeline */}
        <div className="max-w-2xl mx-auto w-full pb-4">
          {STEPS.map((step, i) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, x: -28 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.6, ease: 'easeOut', delay: 0.5 + i * 0.18 }}
              className="flex gap-6 relative"
            >
              {/* Left column: circle + line */}
              <div className="flex flex-col items-center shrink-0">
                {/* Numbered circle */}
                <div
                  className="w-14 h-14 rounded-full flex items-center justify-center text-white font-black text-lg shrink-0 z-10"
                  style={{
                    background: `linear-gradient(135deg, ${step.color}, ${step.color}bb)`,
                    boxShadow: `0 0 20px ${step.color}55`,
                  }}
                >
                  {step.number}
                </div>

                {/* Connecting line — not shown after last step */}
                {i < STEPS.length - 1 && (
                  <motion.div
                    initial={{ scaleY: 0 }}
                    animate={{ scaleY: 1 }}
                    transition={{ duration: 0.6, delay: 0.7 + i * 0.18, ease: 'easeOut' }}
                    style={{
                      width: 2,
                      flexGrow: 1,
                      minHeight: 48,
                      background: `linear-gradient(to bottom, ${step.color}88, ${STEPS[i + 1].color}44)`,
                      transformOrigin: 'top',
                      marginTop: 4,
                      marginBottom: 4,
                    }}
                  />
                )}
              </div>

              {/* Right column: card */}
              <div
                className="liquid-glass rounded-2xl p-6 mb-6 flex-1"
                style={{ border: `1px solid ${step.color}30` }}
              >
                <div className="flex items-center gap-3 mb-3">
                  <span className="text-2xl">{step.icon}</span>
                  <h3 className="text-lg font-bold text-white">{step.title}</h3>
                </div>
                <p className="text-sm leading-relaxed" style={{ color: 'rgba(255,255,255,0.68)' }}>
                  {step.desc}
                </p>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Bottom CTA */}
        <motion.div
          variants={fadeUp(1.5)}
          initial="hidden"
          animate="show"
          className="mt-8 mb-16 text-center"
        >
          <Link
            to={isAuthenticated ? '/dashboard' : '/auth'}
            className="inline-block text-white text-lg font-bold px-10 py-4 rounded-xl hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)', boxShadow: '0 4px 20px rgba(22,163,74,.5)' }}
          >
            {isAuthenticated ? 'Go to Dashboard →' : "Get started — it's free"}
          </Link>
          <p className="mt-3 text-sm" style={{ color: 'rgba(255,255,255,0.55)' }}>Join hundreds of users already tracking smarter.</p>
        </motion.div>

      </div>
    </div>
  )
}
