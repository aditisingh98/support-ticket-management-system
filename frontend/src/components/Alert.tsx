import type { ReactNode } from 'react'

type AlertTone = 'error' | 'success' | 'info'

interface AlertProps {
  tone?: AlertTone
  title?: string
  children: ReactNode
}

export function Alert({ tone = 'info', title, children }: AlertProps) {
  return (
    <div className={`alert alert-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {title ? <strong className="alert-title">{title}</strong> : null}
      <div className="alert-body">{children}</div>
    </div>
  )
}
