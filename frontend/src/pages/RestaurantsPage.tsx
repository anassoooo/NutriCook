import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import type { LocationDTO } from '../types'

type Tab = 'restaurants' | 'stores'

export default function RestaurantsPage() {
  const [tab,       setTab]       = useState<Tab>('restaurants')
  const [locations, setLocations] = useState<LocationDTO[]>([])
  const [loading,   setLoading]   = useState(false)
  const [error,     setError]     = useState('')
  const [searched,  setSearched]  = useState(false)

  const handleFind = () => {
    if (!navigator.geolocation) { setError('Geolocation is not supported by your browser.'); return }
    setLoading(true); setError('')
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          const endpoint = tab === 'restaurants'
            ? `/location/restaurants?lat=${coords.latitude}&lng=${coords.longitude}`
            : `/location/stores?lat=${coords.latitude}&lng=${coords.longitude}`
          const res = await api.get<LocationDTO[]>(endpoint)
          setLocations(res.data)
          setSearched(true)
        } catch {
          setError('Could not fetch locations. Please try again.')
        } finally { setLoading(false) }
      },
      () => { setError('Location access denied. Please allow location access in your browser.'); setLoading(false) }
    )
  }

  const switchTab = (t: Tab) => { setTab(t); setLocations([]); setSearched(false); setError('') }

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Header */}
      <div>
        <h1 className="page-title">Find Nearby</h1>
        <p className="page-subtitle">Healthy restaurants and grocery stores near you — powered by OpenStreetMap.</p>
      </div>

      {/* Controls */}
      <div className="card flex items-center gap-4 flex-wrap">
        <div className="flex rounded-xl p-1 gap-0.5" style={{ background: 'rgba(255,255,255,0.07)' }}>
          {(['restaurants', 'stores'] as Tab[]).map(t => (
            <button
              key={t}
              onClick={() => switchTab(t)}
              className="px-4 py-2 rounded-lg text-sm font-semibold transition-all"
              style={tab === t
                ? { background: 'rgba(255,255,255,0.15)', color: 'white' }
                : { color: 'rgba(255,255,255,0.62)' }}
            >
              {t === 'restaurants' ? '🍽️ Restaurants' : '🛒 Grocery Stores'}
            </button>
          ))}
        </div>

        <button
          onClick={handleFind}
          disabled={loading}
          className="btn-primary"
        >
          {loading ? (
            <>
              <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              Locating…
            </>
          ) : '📍 Find Near Me'}
        </button>

        {searched && locations.length > 0 && (
          <span className="badge-gray ml-auto">{locations.length} results</span>
        )}
      </div>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-2 text-sm rounded-xl px-4 py-3" style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.25)', color: '#fca5a5' }}>
          <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {error}
        </div>
      )}

      {/* Results */}
      <AnimatePresence mode="wait">
        {locations.length > 0 && (
          <motion.div
            key="results"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-3"
          >
            {locations.map((loc, i) => (
              <motion.div
                key={loc.osmNodeId}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                className="card-hover flex items-center gap-4"
              >
                <div className="w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0" style={{ background: 'rgba(16,185,129,0.15)' }}>
                  {tab === 'restaurants' ? '🍽️' : '🛒'}
                </div>

                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-slate-900 truncate">{loc.name || 'Unnamed location'}</p>
                  {loc.address && (
                    <p className="text-sm text-slate-400 truncate mt-0.5">{loc.address}</p>
                  )}
                  <a
                    href={`https://www.openstreetmap.org/node/${loc.osmNodeId}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-xs text-brand-600 hover:text-brand-700 font-medium mt-1 inline-block"
                  >
                    View on OpenStreetMap →
                  </a>
                </div>

                <div className="text-right shrink-0">
                  <p className="text-xs text-slate-400">{loc.latitude.toFixed(4)}</p>
                  <p className="text-xs text-slate-400">{loc.longitude.toFixed(4)}</p>
                </div>
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {searched && locations.length === 0 && !loading && (
        <div className="card text-center py-16">
          <div className="text-5xl mb-3">🗺️</div>
          <p className="font-semibold" style={{ color: 'rgba(255,255,255,0.6)' }}>No {tab} found</p>
          <p className="text-sm mt-1" style={{ color: 'rgba(255,255,255,0.58)' }}>Nothing within 2 km of your location.</p>
        </div>
      )}

      {!searched && !loading && (
        <div className="card text-center py-16 border-dashed">
          <div className="text-5xl mb-3">📍</div>
          <p className="font-semibold" style={{ color: 'rgba(255,255,255,0.6)' }}>Ready to search</p>
          <p className="text-sm mt-1" style={{ color: 'rgba(255,255,255,0.58)' }}>Click "Find Near Me" and allow location access.</p>
        </div>
      )}
    </div>
  )
}
