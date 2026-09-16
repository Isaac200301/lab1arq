import { useCallback, useEffect, useState } from 'react'
import CustomerList from './components/CustomerList'
import TransferFunds from './components/TransferFunds'
import TransactionHistory from './components/TransactionHistory'
import { customersApi, formatCurrency } from './api/client'
import './css/App.css'

const TABS = [
  { id: 'customers', label: 'Clientes' },
  { id: 'transfer', label: 'Transferencias' },
  { id: 'history', label: 'Historico' },
]

export default function App() {
  const [activeTab, setActiveTab] = useState('customers')

  // La lista de clientes vive aqui, no dentro de cada vista: las tres la
  // necesitan, y despues de una transferencia los saldos cambian, asi que
  // conviene tener una sola fuente de verdad que se refresque de una vez.
  const [customers, setCustomers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadCustomers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await customersApi.getAll()
      setCustomers(data ?? [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCustomers()
  }, [loadCustomers])

  const totalBalance = customers.reduce(
    (sum, customer) => sum + (customer.balance ?? 0),
    0
  )

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">UB</span>
            <div>
              <h1>UdeaBank</h1>
              <p className="brand-subtitle">Laboratorio 1 · Arquitectura de Software</p>
            </div>
          </div>

          <div className="header-balance">
            <span className="header-balance-label">Saldo consolidado</span>
            <span className="header-balance-value">
              {loading && customers.length === 0 ? '—' : formatCurrency(totalBalance)}
            </span>
            <span className="header-balance-meta">
              {customers.length} cuenta{customers.length === 1 ? '' : 's'} registrada
              {customers.length === 1 ? '' : 's'}
            </span>
          </div>

          <nav className="tabs" aria-label="Secciones">
            {TABS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                className={`tab ${activeTab === tab.id ? 'tab-active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
                aria-current={activeTab === tab.id ? 'page' : undefined}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <main className="app-main">
        {activeTab === 'customers' && (
          <CustomerList
            customers={customers}
            loading={loading}
            error={error}
            onRefresh={loadCustomers}
          />
        )}

        {activeTab === 'transfer' && (
          <TransferFunds
            customers={customers}
            loading={loading}
            onTransferCompleted={loadCustomers}
          />
        )}

        {activeTab === 'history' && <TransactionHistory customers={customers} />}
      </main>

      <footer className="app-footer">
        API: <code>http://localhost:8088/api</code>
      </footer>
    </div>
  )
}
