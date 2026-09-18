import { Activity, ArrowUpRight, Braces, ChevronRight, CircleHelp, LayoutDashboard, ScrollText, Terminal } from 'lucide-react'
import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router-dom'
import { useHealth } from './hooks/useHealth'
import { OverviewPage } from './pages/OverviewPage'
import { ApiPage } from './pages/ApiPage'
import { LogsPage } from './pages/LogsPage'

function Workspace() {
  const health = useHealth()
  return (
    <div className="workspace">
      <aside className="sidebar">
        <Link to="/" className="brand"><span className="brand-icon"><Activity size={23} /></span><span>Log Analyzer<span className="brand-caption">DEVELOPER WORKSPACE</span></span></Link>
        <div className="workspace-label"><span className="workspace-avatar">L</span><span>Local workspace<small>Development</small></span><span className="local-dot" /></div>
        <p className="nav-heading">WORKSPACE</p>
        <nav aria-label="Main navigation">
          <NavLink to="/" end><ScrollText size={17} />Logs</NavLink>
          <NavLink to="/system"><LayoutDashboard size={17} />Overview</NavLink>
          <NavLink to="/api-details"><Braces size={17} />Health API</NavLink>
        </nav>
        <div className="sidebar-bottom">
          <a href="https://docs.docker.com/compose/" target="_blank" rel="noreferrer"><CircleHelp size={17} />Compose documentation<ArrowUpRight size={14} /></a>
          <div className="version"><Terminal size={16} /><span>Local development</span><span>v0.1</span></div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar"><div>Workspace<ChevronRight size={14} /><span>Log Analyzer</span></div><span className="environment"><span className="local-dot" />Local environment</span></header>
        <main><Routes>
          <Route path="/" element={<LogsPage />} />
          <Route path="/system" element={<OverviewPage health={health} />} />
          <Route path="/api-details" element={<ApiPage health={health} />} />
          <Route path="*" element={<section className="not-found"><h1>Page not found</h1><Link to="/">Return to overview</Link></section>} />
        </Routes></main>
        <footer><span>Log Analyzer <span className="footer-divider">/</span> Development workspace</span><span>React + Spring Boot + PostgreSQL</span></footer>
      </div>
    </div>
  )
}

export default function App() {
  return <BrowserRouter><Workspace /></BrowserRouter>
}
