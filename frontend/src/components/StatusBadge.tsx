import type { ReactNode } from 'react'

export function StatusBadge({ state, children }: { state: 'up' | 'down' | 'pending'; children: ReactNode }) {
  return <span className={`badge ${state}`}>{children}</span>
}