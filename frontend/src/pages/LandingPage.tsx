import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Wordmark } from '../components/Brand'
import DayPlate, { RoleKey } from '../components/DayPlate'
import { CheckIcon } from '../components/Icons'
import { SAMPLE_MEALS } from '../lib/samplePlan'

const STEPS = [
  { title: 'Tell it about you', body: 'Age, body, activity and goal set your daily target. Allergies, diets and health conditions set what may be planned.' },
  { title: 'Get five meals', body: 'Breakfast to dinner, each portioned from real foods so the day lands within 5% of your target.' },
  { title: 'Swap what you don’t fancy', body: 'Pick another food of the same kind, or ask the assistant. Every swap is checked against your limits before it is saved.' },
  { title: 'Check off what you eat', body: 'Your day counts itself. Progress and achievements follow what you actually ate, not what was planned.' },
]

const SAFEGUARDS = [
  { title: 'Allergies are never a guess', body: 'Foods carry the 14 allergens of EU labelling law. A food whose allergens are not verified is left out, not assumed safe.' },
  { title: 'The assistant proposes, the planner decides', body: 'The AI writes explanations and suggests swaps. Calories, portions and safety checks always come from the planner itself.' },
  { title: 'Every rule is written down and tested', body: '76 business rules, from the calorie floor to how streaks count, each with automated tests named after it.' },
]

/** The real plate component on a sample day: tick a meal and the plate fills in. */
function PlateDemo() {
  const [eaten, setEaten] = useState<Set<number>>(() => new Set(SAMPLE_MEALS.filter(m => m.completed).map(m => m.id)))
  const meals = SAMPLE_MEALS.map(m => ({ ...m, completed: eaten.has(m.id) }))
  const total = meals.reduce((s, m) => s + m.calories, 0)
  const eatenKcal = meals.filter(m => m.completed).reduce((s, m) => s + m.calories, 0)
  const flip = (id: number) => setEaten(prev => { const s = new Set(prev); if (s.has(id)) s.delete(id); else s.add(id); return s })

  return (
    <div className="grid items-center gap-8 sm:grid-cols-[minmax(0,1fr)_200px]">
      <div>
        <DayPlate meals={meals} eatenKcal={eatenKcal} plannedKcal={total} targetKcal={null} />
        <div className="mt-4"><RoleKey /></div>
      </div>
      <ul className="space-y-1.5" aria-label="Sample day">
        {meals.map(m => (
          <li key={m.id}>
            <button
              type="button"
              role="checkbox"
              aria-checked={m.completed}
              onClick={() => flip(m.id)}
              className="flex w-full items-center gap-3 rounded-ctl px-2 py-1.5 text-left transition-colors hover:bg-wash"
            >
              <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-full border-2 ${m.completed ? 'border-ink bg-ink text-chalk' : 'border-ink-faint'}`} aria-hidden>
                {m.completed && <CheckIcon size={11} strokeWidth={3} />}
              </span>
              <span className="flex-1 text-sm font-semibold">{m.name}</span>
              <span className="text-xs text-ink-soft num">{m.calories}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default function LandingPage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-[1180px] items-center justify-between gap-6 px-6 py-6">
        <Wordmark />
        <nav aria-label="Page" className="hidden items-center gap-1 md:flex">
          <a href="#how" className="btn-ghost">How it works</a>
          <a href="#safe" className="btn-ghost">Safety</a>
        </nav>
        {isAuthenticated ? (
          <Link to="/dashboard" className="btn-primary">Open today</Link>
        ) : (
          <div className="flex items-center gap-2">
            <Link to="/auth" className="btn-ghost">Sign in</Link>
            <Link to="/auth" className="btn-primary hidden sm:inline-flex">Get started</Link>
          </div>
        )}
      </header>

      <main>
        {/* Hero: the product itself */}
        <section className="mx-auto grid max-w-[1180px] items-center gap-12 px-6 pb-20 pt-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,560px)] lg:pt-16">
          <div>
            <h1 className="font-heading text-[2.9rem] font-bold leading-[1.02] tracking-[-0.03em] sm:text-[4rem] lg:text-[4.6rem]">
              Five meals a day, planned around you.
            </h1>
            <p className="mt-6 max-w-[48ch] text-lg text-ink-soft">
              NutriCook builds your day from your body, your goal, your allergies and your diet. Then it counts what you
              actually eat.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={isAuthenticated ? '/dashboard' : '/auth'} className="btn-primary px-6 py-3 text-[15px]">
                {isAuthenticated ? 'Open today' : 'Plan my first day'}
              </Link>
              <a href="#how" className="btn-secondary px-6 py-3 text-[15px]">See how it works</a>
            </div>
            <p className="mt-6 text-sm text-ink-faint">Free. Built on USDA food data. Not medical advice.</p>
          </div>
          <div className="rounded-panel border border-line bg-paper p-6 sm:p-8">
            <p className="mb-6 text-sm text-ink-soft">A sample day. Tick a meal to see the plate fill in.</p>
            <PlateDemo />
          </div>
        </section>

        {/* How a day works: a real sequence, so it is numbered */}
        <section id="how" className="scroll-mt-8 border-t border-line">
          <div className="mx-auto max-w-[1180px] px-6 py-20">
            <h2 className="font-heading text-3xl font-bold sm:text-4xl">How a day works</h2>
            <ol className="mt-10 grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-4">
              {STEPS.map((s, i) => (
                <li key={s.title}>
                  <span className="grid h-9 w-9 place-items-center rounded-full border-2 border-ink font-heading font-bold num">{i + 1}</span>
                  <h3 className="mt-4 font-heading text-xl font-semibold">{s.title}</h3>
                  <p className="mt-2 text-ink-soft">{s.body}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* Safety */}
        <section id="safe" className="scroll-mt-8 bg-ink text-chalk">
          <div className="mx-auto grid max-w-[1180px] gap-12 px-6 py-20 lg:grid-cols-[minmax(0,380px)_1fr]">
            <h2 className="font-heading text-3xl font-bold text-chalk sm:text-4xl">Careful with what goes on your plate</h2>
            <div className="divide-y divide-chalk/15">
              {SAFEGUARDS.map(s => (
                <div key={s.title} className="py-6 first:pt-0 last:pb-0">
                  <h3 className="font-heading text-xl font-semibold text-chalk">{s.title}</h3>
                  <p className="mt-2 max-w-[62ch] text-chalk/75">{s.body}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-[1180px] px-6 py-20">
          <div className="flex flex-col items-start gap-6 border-y border-line py-12 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-heading text-3xl font-bold">Your first day takes a minute.</h2>
              <p className="mt-2 text-ink-soft">Three questions, then five meals.</p>
            </div>
            <Link to={isAuthenticated ? '/dashboard' : '/auth'} className="btn-primary px-6 py-3 text-[15px]">
              {isAuthenticated ? 'Open today' : 'Plan my first day'}
            </Link>
          </div>
        </section>
      </main>

      <footer className="mx-auto flex max-w-[1180px] flex-wrap items-center justify-between gap-4 px-6 pb-10 text-sm text-ink-faint">
        <span>NutriCook, an engineering project. Plans are not medical advice.</span>
        <a href="https://github.com/anassoooo/NutriCook" className="underline underline-offset-2 hover:text-ink">Source on GitHub</a>
      </footer>
    </div>
  )
}
