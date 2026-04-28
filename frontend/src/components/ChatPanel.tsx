import { useState, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan } from '../types'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

interface Props {
  planId: number
  onPlanUpdate: (plan: DietPlan) => void
}

export default function ChatPanel({ planId, onPlanUpdate }: Props) {
  const { userId } = useAuth()
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: "Hi! I'm your AI dietitian 🥗 Ask me anything about today's plan, or tell me what you'd like to change — e.g. \"Replace dinner with something lighter\" or \"I don't like salmon\".",
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open])

  const send = async () => {
    const text = input.trim()
    if (!text || loading || !userId) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      const { data } = await api.post<{ reply: string; updatedPlan: DietPlan | null }>(
        `/users/${userId}/diet-plans/${planId}/chat`,
        { message: text },
      )
      setMessages(prev => [...prev, { role: 'assistant', content: data.reply }])
      if (data.updatedPlan) onPlanUpdate(data.updatedPlan)
    } catch {
      setMessages(prev => [
        ...prev,
        { role: 'assistant', content: 'Sorry, something went wrong. Please try again.' },
      ])
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* Floating button */}
      <button
        onClick={() => setOpen(v => !v)}
        className="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full shadow-xl flex items-center justify-center text-white text-2xl transition-transform hover:scale-110 active:scale-95"
        style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}
        title="AI Dietitian"
      >
        {open ? '✕' : '✨'}
      </button>

      {/* Chat panel */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 24, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 24, scale: 0.96 }}
            transition={{ duration: 0.22, ease: 'easeOut' }}
            className="fixed bottom-24 right-6 z-50 w-[360px] max-w-[calc(100vw-3rem)] rounded-2xl overflow-hidden flex flex-col"
            style={{
              background: 'white',
              boxShadow: '0 24px 80px rgba(0,0,0,0.18), 0 0 0 1px rgba(0,0,0,0.06)',
              height: 480,
            }}
          >
            {/* Header */}
            <div
              className="px-5 py-4 flex items-center gap-3 shrink-0"
              style={{ background: 'linear-gradient(135deg,#064e3b,#0d9488)' }}
            >
              <div className="w-9 h-9 rounded-xl bg-white/20 flex items-center justify-center text-lg">✨</div>
              <div>
                <p className="font-bold text-white text-sm leading-tight">AI Dietitian</p>
                <p className="text-emerald-300 text-xs">Powered by Groq · Llama 3.3</p>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
              {messages.map((msg, i) => (
                <div
                  key={i}
                  className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[82%] px-3.5 py-2.5 rounded-2xl text-sm leading-relaxed ${
                      msg.role === 'user'
                        ? 'text-white rounded-br-sm'
                        : 'bg-slate-100 text-slate-800 rounded-bl-sm'
                    }`}
                    style={
                      msg.role === 'user'
                        ? { background: 'linear-gradient(135deg,#16a34a,#0d9488)' }
                        : undefined
                    }
                  >
                    {msg.content}
                  </div>
                </div>
              ))}

              {loading && (
                <div className="flex justify-start">
                  <div className="bg-slate-100 rounded-2xl rounded-bl-sm px-4 py-3 flex gap-1.5 items-center">
                    <span className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            {/* Input */}
            <div className="px-4 py-3 border-t border-slate-100 shrink-0 flex gap-2">
              <input
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && !e.shiftKey && send()}
                placeholder="Ask me anything…"
                disabled={loading}
                className="flex-1 bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/25 focus:border-brand-500 transition-all disabled:opacity-50"
              />
              <button
                onClick={send}
                disabled={!input.trim() || loading}
                className="w-9 h-9 rounded-xl flex items-center justify-center text-white shrink-0 disabled:opacity-40 transition-opacity"
                style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}
