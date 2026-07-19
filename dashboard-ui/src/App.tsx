import { Link, Navigate, Outlet, Route, Routes, useLocation, useParams } from 'react-router-dom'
import { BRAND_MARK_URL } from './config/brand'
import ThemeControls from './components/ThemeControls'
import AboutPanel from './features/about/AboutPanel'
import StepEnergyCalculator from './features/calculator/StepEnergyCalculator'
import ActivationSummary from './features/activations/ActivationSummary'
import LocationInsights from './features/feasibility/LocationInsights'
import TimeSeriesDashboard from './features/trends/TimeSeriesDashboard'
import EnergyLedger from './features/ledger/EnergyLedger'
import TileInventory from './features/infrastructure/TileInventory'
import NotFoundPage from './pages/NotFoundPage'

const DASHBOARD_TABS = [
  { id: 'feasibility', label: 'All-time feasibility' },
  { id: 'activations', label: 'Activation statistics' },
  { id: 'trends', label: 'Trends over time' },
  { id: 'ledger', label: 'Compression ledger' },
  { id: 'inventory', label: 'Tile inventory' },
] as const

const TOP_TABS = [
  { id: 'dashboards', label: 'Dashboards', to: '/dashboards/feasibility' },
  { id: 'calculator', label: 'Calculator', to: '/calculator' },
  { id: 'about', label: 'About', to: '/about' },
] as const

function DashboardRoutes() {
  const { dashboardTab } = useParams<{ dashboardTab: string }>()
  const valid = DASHBOARD_TABS.some((t) => t.id === dashboardTab)
  if (!valid) {
    return <NotFoundPage />
  }
  switch (dashboardTab) {
    case 'feasibility':
      return <LocationInsights />
    case 'activations':
      return <ActivationSummary />
    case 'trends':
      return <TimeSeriesDashboard />
    case 'ledger':
      return <EnergyLedger />
    case 'inventory':
      return <TileInventory />
    default:
      return <NotFoundPage />
  }
}

function resolveTopTab(pathname: string) {
  if (pathname.startsWith('/calculator')) return 'calculator'
  if (pathname.startsWith('/about')) return 'about'
  return 'dashboards'
}

function AppLayout() {
  const location = useLocation()
  const pathname = location.pathname

  const topTab = resolveTopTab(pathname)

  return (
    <div className="rgf-app">
      <div className="rgf-app-gradient" aria-hidden />
      <div className="relative mx-auto w-[min(98vw,1720px)] px-2 py-6 sm:px-3 lg:px-4 lg:py-8">
        <header className="rgf-header-rule">
          <div>
            <div className="rgf-title-row">
              <img
                src={BRAND_MARK_URL}
                alt=""
                className="rgf-brand-mark"
                decoding="async"
              />
              <h1 className="rgf-title">Project Kinetile</h1>
            </div>
          </div>

          <div className="mt-6 flex flex-col gap-4">
            <nav className="rgf-nav-main" aria-label="Main sections">
              {TOP_TABS.map((tab) => {
                const isActive = tab.id === topTab
                return (
                  <Link
                    key={tab.id}
                    to={tab.to}
                    data-active={isActive ? 'true' : 'false'}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    {tab.label}
                  </Link>
                )
              })}
            </nav>

            {topTab === 'dashboards' && (
              <nav className="rgf-nav-dash" aria-label="Dashboard views">
                {DASHBOARD_TABS.map((tab) => {
                  const to = `/dashboards/${tab.id}`
                  const isActive = pathname === to
                  return (
                    <Link
                      key={tab.id}
                      to={to}
                      data-active={isActive ? 'true' : 'false'}
                      aria-current={isActive ? 'page' : undefined}
                    >
                      {tab.label}
                    </Link>
                  )
                })}
              </nav>
            )}
          </div>
        </header>

        <main className="rgf-main">
          <Outlet />
        </main>

        <footer className="rgf-disclaimer" role="note">
          <strong>Disclaimer:</strong> All energy figures and real-world equivalents are illustrative approximations based on
          the hardware threshold-activation model (100 N threshold; joules scale from roughly 2–5 J
          per activation based on load and step intensity).
          <br />They are not
          certified measurements and should not be used as the sole basis for investment, policy, or
          engineering decisions.
        </footer>
      </div>

      <ThemeControls />
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<AppLayout />}>
        <Route index element={<Navigate to="/dashboards/feasibility" replace />} />
        <Route path="dashboards/:dashboardTab" element={<DashboardRoutes />} />
        <Route path="calculator" element={<StepEnergyCalculator />} />
        <Route path="about" element={<AboutPanel />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
