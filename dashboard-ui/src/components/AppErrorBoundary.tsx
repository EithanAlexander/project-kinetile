import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Props = { children: ReactNode }

type State = {
  error: Error | null
  errorInfo: ErrorInfo | null
}

type ErrorSnapshot = {
  message: string
  stack?: string
  componentStack?: string
  time: string
}

/**
 * Catches render errors in the React tree, logs a structured payload for investigation, and offers recovery.
 */
export class AppErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    const isDev = import.meta.env.DEV
    const payload: ErrorSnapshot = {
      message: error.message,
      stack: isDev ? error.stack : undefined,
      componentStack: isDev ? (errorInfo.componentStack ?? undefined) : undefined,
      time: new Date().toISOString(),
    }
    ;(globalThis as typeof globalThis & { __KINETILE_LAST_ERROR__?: ErrorSnapshot }).__KINETILE_LAST_ERROR__ = payload
    console.error('[Kinetile AppErrorBoundary]', JSON.stringify(payload, null, 2))
    console.error(error)
    this.setState({ errorInfo })
  }

  handleReload = () => {
    globalThis.location.reload()
  }

  render() {
    const { error, errorInfo } = this.state
    if (error) {
      return (
        <div className="rgf-app">
          <div className="rgf-app-gradient" aria-hidden />
          <div className="relative mx-auto flex min-h-[70vh] max-w-lg flex-col items-center justify-center px-4 py-16 text-center">
            <p className="rgf-kicker">Unexpected error</p>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight">
              The dashboard lost its footing
            </h1>
            <p className="rgf-lead mt-3 text-sm">
              Something threw while rendering. Details were written to the browser console and to{' '}
              <code className="rgf-code">window.__KINETILE_LAST_ERROR__</code> for support. Try
              reloading; if it keeps happening, note the time and message below.
            </p>
            <p className="rgf-notice mt-4 max-w-md text-left font-mono text-xs">
              <span className="rgf-notice-body">{error.message}</span>
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <button type="button" onClick={this.handleReload} className="rgf-btn-primary">
                Reload page
              </button>
              <Link to="/dashboards/feasibility" className="rgf-btn-secondary">
                Back to dashboards
              </Link>
            </div>
            {import.meta.env.DEV && errorInfo?.componentStack ? (
              <details className="mt-10 w-full max-w-2xl text-left">
                <summary className="cursor-pointer text-xs text-[var(--rgf-text-subtle)]">
                  Component stack (dev)
                </summary>
                <pre className="rgf-dev-pre">{errorInfo.componentStack}</pre>
              </details>
            ) : null}
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
