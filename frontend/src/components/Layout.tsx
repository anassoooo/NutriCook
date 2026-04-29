import { Outlet, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import Navbar from './Navbar'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

export default function Layout() {
  const location = useLocation()
  return (
    <div className="dark-layout relative min-h-screen flex flex-col overflow-x-hidden">

      {/* Video background — shared with landing & auth */}
      <video
        autoPlay loop muted playsInline
        style={{ position: 'fixed', inset: 0, width: '100%', height: '100%', objectFit: 'cover', zIndex: 0 }}
      >
        <source src={VIDEO_SRC} type="video/mp4" />
      </video>

      {/* Dark veil — heavier than landing so content stays readable */}
      <div className="fixed inset-0 bg-black/65 z-[1]" />

      {/* App shell — above video */}
      <div className="relative z-10 flex flex-col min-h-screen">
        <Navbar />
        <main className="flex-1 max-w-6xl mx-auto w-full px-4 py-8">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.38, ease: 'easeOut' }}
          >
            <Outlet />
          </motion.div>
        </main>
      </div>
    </div>
  )
}
