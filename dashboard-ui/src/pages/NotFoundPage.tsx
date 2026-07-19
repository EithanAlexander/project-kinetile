import { Link } from 'react-router-dom'
import { BRAND_MARK_URL } from '../config/brand'

/**
 * Unknown URL: playful copy + brand mark. Shown for unmatched routes and invalid dashboard slugs.
 */
export default function NotFoundPage() {
  return (
    <div className="rgf-app">
      <div className="rgf-app-gradient" aria-hidden />
      <div className="relative mx-auto flex min-h-[70vh] max-w-4xl flex-col items-center justify-center px-4 py-16 text-center">
        <img
          src={BRAND_MARK_URL}
          alt="Project Kinetile"
          className="rgf-brand-mark mb-6 shadow-lg"
          decoding="async"
        />
        <p className="font-mono text-2xl font-semibold tracking-tight text-[var(--rgf-accent-dim)] sm:text-3xl">
          404: Fully Off The Grid!
        </p>
        <div className="mt-2 flex w-full max-w-full justify-center overflow-x-auto [scrollbar-width:thin]">
          <h1 className="whitespace-nowrap text-xl font-semibold tracking-tight sm:text-2xl">
            You&apos;ve wandered off outside the sensor zone! 😳
            <br />
            Double-check the URL you tried to access
          </h1>
        </div>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link to="/dashboards/feasibility" className="rgf-btn-primary">
            All-time feasibility
          </Link>
          <Link to="/calculator" className="rgf-btn-secondary">
            Calculator
          </Link>
        </div>
      </div>
    </div>
  )
}
