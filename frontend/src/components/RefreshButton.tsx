import { RefreshCw } from 'lucide-react'

export function RefreshButton({ loading, onRefresh }: { loading: boolean; onRefresh: () => void }) {
  return <button className="button" onClick={onRefresh} disabled={loading}><RefreshCw size={14} className={loading ? 'spin' : ''} />{loading ? 'Checking' : 'Refresh'}</button>
}