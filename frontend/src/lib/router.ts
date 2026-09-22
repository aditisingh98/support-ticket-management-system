import { useCallback, useEffect, useState } from 'react'

export type NavigateOptions = {
  replace?: boolean
}

function currentPath(): string {
  return `${window.location.pathname}${window.location.search}`
}

export function useRouter() {
  const [path, setPath] = useState(currentPath)

  useEffect(() => {
    const onChange = () => setPath(currentPath())
    window.addEventListener('popstate', onChange)
    return () => window.removeEventListener('popstate', onChange)
  }, [])

  const navigate = useCallback((to: string, options?: NavigateOptions) => {
    if (options?.replace) {
      window.history.replaceState({}, '', to)
    } else {
      window.history.pushState({}, '', to)
    }
    setPath(currentPath())
  }, [])

  return { path, pathname: path.split('?')[0] ?? path, navigate }
}

export function matchRoute(
  pathname: string,
):
  | { name: 'dashboard' }
  | { name: 'tickets' }
  | { name: 'create' }
  | { name: 'detail'; ticketId: string }
  | { name: 'notfound' } {
  if (pathname === '/' || pathname === '/dashboard') {
    return { name: 'dashboard' }
  }
  if (pathname === '/tickets') {
    return { name: 'tickets' }
  }
  if (pathname === '/tickets/new') {
    return { name: 'create' }
  }
  const detail = pathname.match(/^\/tickets\/([^/]+)$/)
  if (detail?.[1]) {
    return { name: 'detail', ticketId: decodeURIComponent(detail[1]) }
  }
  return { name: 'notfound' }
}
