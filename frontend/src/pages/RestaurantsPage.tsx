import { useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import type { LocationDTO } from '../types'
import { PinIcon } from '../components/Icons'

type Tab = 'restaurants' | 'stores'

const TABS: { value: Tab; label: string; singular: string }[] = [
  { value: 'restaurants', label: 'Restaurants', singular: 'restaurant' },
  { value: 'stores', label: 'Grocery stores', singular: 'store' },
]

/** The server searches this radius (app.osm.search-radius-meters). */
const RADIUS_M = 2000

interface Place extends LocationDTO {
  distance: number
  /** bearing in degrees, clockwise from north */
  bearing: number
}

/** Distance and direction from the user, for sorting and drawing only. */
function locate(from: { lat: number; lng: number }, to: LocationDTO): { distance: number; bearing: number } {
  const rad = Math.PI / 180
  const dLat = (to.latitude - from.lat) * rad
  const dLng = (to.longitude - from.lng) * rad
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(from.lat * rad) * Math.cos(to.latitude * rad) * Math.sin(dLng / 2) ** 2
  const distance = 2 * 6_371_000 * Math.asin(Math.sqrt(a))
  const y = Math.sin(dLng) * Math.cos(to.latitude * rad)
  const x = Math.cos(from.lat * rad) * Math.sin(to.latitude * rad) - Math.sin(from.lat * rad) * Math.cos(to.latitude * rad) * Math.cos(dLng)
  const bearing = (Math.atan2(y, x) / rad + 360) % 360
  return { distance, bearing }
}

const formatDistance = (m: number) => (m < 1000 ? `${Math.round(m / 10) * 10} m` : `${(m / 1000).toFixed(1)} km`)

/** You at the centre, every place where it really is (north up), within the search radius. */
function Radar({ places, active, onPick }: { places: Place[]; active: number | null; onPick: (i: number) => void }) {
  const R = 140
  return (
    <svg viewBox="-160 -160 320 320" className="h-auto w-full max-w-[320px]" role="img" aria-label={`Map of ${places.length} places within ${RADIUS_M / 1000} km`}>
      <circle r={R} fill="#FFFFFF" stroke="#D9DED3" />
      {[0.5, 1].map(f => (
        <circle key={f} r={R * f} fill="none" stroke="#D9DED3" strokeDasharray={f === 1 ? undefined : '3 5'} />
      ))}
      <text x={4} y={-R * 0.5 - 4} fontSize={10} fill="#8E8795">1 km</text>
      <text x={0} y={-R - 6} fontSize={11} fill="#8E8795" textAnchor="middle">N</text>
      {places.map((p, i) => {
        const r = Math.min(p.distance / RADIUS_M, 1) * R
        const a = ((p.bearing - 90) * Math.PI) / 180
        const on = active === i
        return (
          <circle
            key={p.osmNodeId}
            cx={r * Math.cos(a)}
            cy={r * Math.sin(a)}
            r={on ? 7 : 4.5}
            fill={on ? '#2B1E33' : '#FFFFFF'}
            stroke="#2B1E33"
            strokeWidth={1.75}
            onClick={() => onPick(i)}
            className="cursor-pointer"
          >
            <title>{`${p.name || 'Unnamed'}, ${formatDistance(p.distance)}`}</title>
          </circle>
        )
      })}
      {/* you: a crosshair at the centre */}
      <path d="M-9 0H9M0 -9V9" stroke="#2B1E33" strokeWidth={2} strokeLinecap="round" />
      <circle r={13} fill="none" stroke="#2B1E33" strokeOpacity={0.25} />
    </svg>
  )
}

export default function RestaurantsPage() {
  const [tab, setTab] = useState<Tab>('restaurants')
  const [places, setPlaces] = useState<Place[]>([])
  const [active, setActive] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)
  const current = TABS.find(t => t.value === tab)!

  const handleFind = () => {
    if (!navigator.geolocation) { setError('Your browser cannot share its location.'); return }
    setLoading(true); setError(''); setActive(null)
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          const endpoint = tab === 'restaurants'
            ? `/location/restaurants?lat=${coords.latitude}&lng=${coords.longitude}`
            : `/location/stores?lat=${coords.latitude}&lng=${coords.longitude}`
          const res = await api.get<LocationDTO[]>(endpoint)
          const from = { lat: coords.latitude, lng: coords.longitude }
          setPlaces(res.data.map(l => ({ ...l, ...locate(from, l) })).sort((a, b) => a.distance - b.distance))
          setSearched(true)
        } catch (err) {
          // MAP_UNAVAILABLE ("try again in a minute") is not the same as "nothing nearby"
          setPlaces([])
          setSearched(false)
          setError(toApiError(err).message)
        } finally { setLoading(false) }
      },
      () => { setError('Location access is off. Allow it for this site in your browser, then try again.'); setLoading(false) },
    )
  }

  const switchTab = (t: Tab) => { setTab(t); setPlaces([]); setSearched(false); setError(''); setActive(null) }

  return (
    <div>
      <header>
        <h1 className="page-title">Nearby</h1>
        <p className="page-subtitle">
          Places to eat or shop within {RADIUS_M / 1000} km, from OpenStreetMap. Your exact position is never sent: it is
          rounded to about 100 m first.
        </p>
      </header>

      <div className="mt-8 flex flex-wrap items-center gap-3">
        <div className="flex gap-1.5" role="group" aria-label="Kind of place">
          {TABS.map(t => (
            <button
              key={t.value}
              type="button"
              aria-pressed={tab === t.value}
              onClick={() => switchTab(t.value)}
              className={`rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors ${
                tab === t.value ? 'bg-ink text-chalk' : 'text-ink-soft hover:bg-wash hover:text-ink'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
        <button type="button" onClick={handleFind} disabled={loading} className="btn-primary sm:ml-auto">
          <PinIcon size={17} />
          {loading ? 'Finding your location…' : 'Find near me'}
        </button>
      </div>

      {error && <div role="alert" className="banner-error mt-6">{error}</div>}

      {places.length > 0 && (
        <div className="mt-8 grid gap-8 md:grid-cols-[320px_minmax(0,1fr)] md:items-start">
          <div className="md:sticky md:top-10">
            <Radar places={places} active={active} onPick={setActive} />
            <p className="mt-2 text-xs text-ink-faint">You are at the cross. North is up. Pick a dot or a name to see it in OpenStreetMap.</p>
          </div>
          <div>
            <p className="text-sm text-ink-soft num">
              {places.length} {places.length === 1 ? current.singular : current.label.toLowerCase()}, nearest first
            </p>
            <ol className="mt-2 divide-y divide-line border-y border-line">
              {places.map((p, i) => (
                <li key={p.osmNodeId}>
                  <button
                    type="button"
                    onClick={() => setActive(i)}
                    aria-current={active === i}
                    className={`flex w-full items-baseline gap-4 px-2 py-3 text-left transition-colors ${active === i ? 'bg-paper' : 'hover:bg-wash'}`}
                  >
                    <span className="w-14 shrink-0 text-sm font-semibold num">{formatDistance(p.distance)}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-semibold">{p.name || 'Unnamed place'}</span>
                      {p.address && <span className="block truncate text-sm text-ink-soft">{p.address}</span>}
                    </span>
                  </button>
                  {active === i && (
                    <a
                      href={`https://www.openstreetmap.org/?mlat=${p.latitude}&mlon=${p.longitude}#map=18/${p.latitude}/${p.longitude}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mb-3 ml-[4.5rem] inline-block text-sm font-semibold underline underline-offset-2"
                    >
                      Open in OpenStreetMap
                    </a>
                  )}
                </li>
              ))}
            </ol>
          </div>
        </div>
      )}

      {searched && places.length === 0 && !loading && (
        <div className="mt-8 rounded-panel border border-dashed border-line px-6 py-12 text-center">
          <p className="font-heading text-xl font-semibold">No {tab} found</p>
          <p className="mt-1 text-sm text-ink-soft">Nothing is mapped within {RADIUS_M / 1000} km of you.</p>
        </div>
      )}

      {!searched && !loading && !error && (
        <div className="mt-8 rounded-panel border border-dashed border-line px-6 py-12 text-center">
          <p className="font-heading text-xl font-semibold">Where are you?</p>
          <p className="mt-1 text-sm text-ink-soft">Choose “Find near me” and allow location access when your browser asks.</p>
        </div>
      )}
    </div>
  )
}
