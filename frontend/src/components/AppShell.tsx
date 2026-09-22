import type { ReactNode } from 'react'

interface AppShellProps {
  currentPath: string
  pageTitle: string
  pageContext?: string
  onNavigate: (path: string) => void
  children: ReactNode
}

const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', match: (p: string) => p === '/' || p === '/dashboard' },
  { path: '/tickets', label: 'Tickets', match: (p: string) => p === '/tickets' || (p.startsWith('/tickets/') && !p.endsWith('/new')) },
  { path: '/tickets/new', label: 'Create Ticket', match: (p: string) => p === '/tickets/new' },
]

export function AppShell({
  currentPath,
  pageTitle,
  pageContext,
  onNavigate,
  children,
}: AppShellProps) {
  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Primary">
        <div className="sidebar-brand">
          <span className="brand-mark" aria-hidden="true">
            ST
          </span>
          <div>
            <p className="brand-name">Support Tickets</p>
            <p className="brand-sub">Operations Portal</p>
          </div>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => {
            const active = item.match(currentPath)
            return (
              <button
                key={item.path}
                type="button"
                className={`nav-link${active ? ' active' : ''}`}
                aria-current={active ? 'page' : undefined}
                onClick={() => onNavigate(item.path)}
              >
                {item.label}
              </button>
            )
          })}
        </nav>
      </aside>

      <div className="shell-main">
        <header className="topbar">
          <div>
            <p className="topbar-app">Support Ticket Management System</p>
            <h1 className="topbar-title">{pageTitle}</h1>
            {pageContext ? <p className="topbar-context">{pageContext}</p> : null}
          </div>
        </header>
        <main className="content">{children}</main>
      </div>
    </div>
  )
}
