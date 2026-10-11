import { useState, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan } from '../types'
import { ChatIcon, CloseIcon, SendIcon } from './Icons'

/** Server limit (BR-AI-06). */
export const MAX_MESSAGE_LENGTH = 500

interface Message {
  role: 'user' | 'assistant'
  content: string
}

interface Props {
  planId: number
  onPlanUpdate: (plan: DietPlan) => void
  triggerMessage?: string
  onTriggerConsumed?: () => void
  /** Changes when the page asks to open the panel (the desktop header button). */
  openRequest?: number
}

const WELCOME =
  'Ask about today\'s plan, or say what to change, for example "something lighter for dinner" or "no salmon, please". Any swap I suggest is checked against your allergies and limits before it is saved.'

export default function ChatPanel({ planId, onPlanUpdate, triggerMessage, onTriggerConsumed, openRequest }: Props) {
  const { userId } = useAuth()
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([{ role: 'assistant', content: WELCOME }])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open])

  useEffect(() => {
    if (triggerMessage) {
      setOpen(true)
      setInput(triggerMessage)
      onTriggerConsumed?.()
    }
  }, [triggerMessage])

  useEffect(() => {
    if (openRequest) setOpen(true)
  }, [openRequest])

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
    } catch (err) {
      // RATE_LIMITED, AI_UNAVAILABLE, ... each have their own message (Constitution VIII)
      const message = toApiError(err).message
      setMessages(prev => [...prev, { role: 'assistant', content: message }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(v => !v)}
        aria-expanded={open}
        aria-label={open ? 'Close the assistant' : 'Ask the assistant'}
        className={`fixed bottom-24 right-4 z-40 inline-flex items-center gap-2 rounded-full bg-ink px-4 py-3 text-sm font-semibold text-chalk shadow-lift transition-colors hover:bg-brand-600 lg:bottom-8 lg:right-8 ${
          open ? '' : 'lg:hidden'
        }`}
      >
        {open ? <CloseIcon size={18} /> : <ChatIcon size={18} />}
        <span className="hidden sm:inline">{open ? 'Close' : 'Ask the assistant'}</span>
      </button>

      <AnimatePresence>
        {open && (
          <motion.section
            aria-label="Assistant"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 12 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="fixed bottom-40 right-4 z-40 flex h-[min(520px,calc(100vh-13rem))] w-[380px] max-w-[calc(100vw-2rem)] flex-col overflow-hidden rounded-panel border border-line bg-paper shadow-lift lg:bottom-24 lg:right-8"
          >
            <header className="border-b border-line px-5 py-4">
              <p className="font-heading text-lg font-semibold">Assistant</p>
              <p className="text-xs text-ink-faint">Proposes changes; the planner checks them</p>
            </header>

            <div className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
              {messages.map((msg, i) => (
                <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <p
                    className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed ${
                      msg.role === 'user' ? 'rounded-br-md bg-ink text-chalk' : 'rounded-bl-md bg-wash text-ink'
                    }`}
                  >
                    {msg.content}
                  </p>
                </div>
              ))}

              {loading && (
                <div className="flex justify-start" aria-label="The assistant is writing">
                  <div className="flex gap-1.5 rounded-2xl rounded-bl-md bg-wash px-4 py-3">
                    {[0, 150, 300].map(d => (
                      <span key={d} className="h-1.5 w-1.5 animate-bounce rounded-full bg-ink-faint" style={{ animationDelay: `${d}ms` }} />
                    ))}
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            <div className="border-t border-line px-3 pb-2 pt-3">
              <div className="flex gap-2">
                <input
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && !e.shiftKey && send()}
                  placeholder="Ask me anything…"
                  maxLength={MAX_MESSAGE_LENGTH}
                  aria-describedby="chat-counter"
                  disabled={loading}
                  className="input flex-1"
                />
                <button
                  type="button"
                  onClick={send}
                  aria-label="Send message"
                  disabled={!input.trim() || loading}
                  className="grid h-[42px] w-[42px] shrink-0 place-items-center rounded-ctl bg-ink text-chalk transition-opacity disabled:opacity-30"
                >
                  <SendIcon size={18} />
                </button>
              </div>
              <p id="chat-counter" className="mt-1 text-right text-[11px] text-ink-faint num">
                {input.length}/{MAX_MESSAGE_LENGTH}
              </p>
            </div>
          </motion.section>
        )}
      </AnimatePresence>
    </>
  )
}
