import { useEffect, useState } from 'react'
import { transactionsApi, formatCurrency, formatDateTime } from '../api/client'

/**
 * Vista 3: Historico de transacciones por cliente.
 * Consume GET /api/transactions/{accountNumber}
 */
export default function TransactionHistory({ customers }) {
  const [accountNumber, setAccountNumber] = useState('')
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!accountNumber) {
      setTransactions([])
      setError(null)
      return
    }

    // ignore evita una condicion de carrera: si el usuario cambia de cuenta
    // antes de que termine la peticion anterior, la respuesta vieja llegaria
    // despues y pisaria a la nueva.
    let ignore = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await transactionsApi.getByAccount(accountNumber)
        if (!ignore) setTransactions(data ?? [])
      } catch (err) {
        if (!ignore) {
          setError(err.message)
          setTransactions([])
        }
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    load()
    return () => {
      ignore = true
    }
  }, [accountNumber])

  const selectedCustomer = customers.find(
    (customer) => customer.accountNumber === accountNumber
  )

  const totalReceived = transactions
    .filter((transaction) => transaction.receiverAccountNumber === accountNumber)
    .reduce((sum, transaction) => sum + (transaction.amount ?? 0), 0)

  const totalSent = transactions
    .filter((transaction) => transaction.senderAccountNumber === accountNumber)
    .reduce((sum, transaction) => sum + (transaction.amount ?? 0), 0)

  const sortedTransactions = [...transactions].sort((a, b) => {
    const dateA = a.timestamp ? new Date(a.timestamp).getTime() : 0
    const dateB = b.timestamp ? new Date(b.timestamp).getTime() : 0
    if (dateB !== dateA) return dateB - dateA
    return (b.id ?? 0) - (a.id ?? 0)
  })

  return (
    <div className="view">
      <section className="card">
        <h2 className="card-title">Historico de transacciones</h2>

        <label className="field field-wide">
          <span>Cliente</span>
          <select
            value={accountNumber}
            onChange={(event) => setAccountNumber(event.target.value)}
          >
            <option value="">Selecciona un cliente…</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.accountNumber}>
                {customer.firstName} {customer.lastName} — {customer.accountNumber}
              </option>
            ))}
          </select>
        </label>

        {selectedCustomer && !loading && !error && (
          <div className="stat-row">
            <div className="stat">
              <span className="stat-label">Movimientos</span>
              <span className="stat-value">{transactions.length}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Total enviado</span>
              <span className="stat-value stat-out">{formatCurrency(totalSent)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Total recibido</span>
              <span className="stat-value stat-in">{formatCurrency(totalReceived)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Saldo actual</span>
              <span className="stat-value">{formatCurrency(selectedCustomer.balance)}</span>
            </div>
          </div>
        )}

        {error && <p className="alert alert-error">{error}</p>}

        {loading && <p className="state">Cargando movimientos…</p>}

        {!accountNumber && !loading && (
          <p className="state">Selecciona un cliente para ver sus movimientos.</p>
        )}

        {accountNumber && !loading && !error && transactions.length === 0 && (
          <p className="state">Esta cuenta todavia no registra movimientos.</p>
        )}

        {sortedTransactions.length > 0 && (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Contraparte</th>
                  <th className="numeric">Monto</th>
                </tr>
              </thead>
              <tbody>
                {sortedTransactions.map((transaction) => {
                  const isOutgoing = transaction.senderAccountNumber === accountNumber
                  const counterparty = isOutgoing
                    ? transaction.receiverAccountNumber
                    : transaction.senderAccountNumber
                  const counterpartyCustomer = customers.find(
                    (customer) => customer.accountNumber === counterparty
                  )

                  return (
                    <tr key={transaction.id}>
                      <td className="muted">{transaction.id}</td>
                      <td>{formatDateTime(transaction.timestamp)}</td>
                      <td>
                        <span className={`badge ${isOutgoing ? 'badge-out' : 'badge-in'}`}>
                          {isOutgoing ? 'Enviada' : 'Recibida'}
                        </span>
                      </td>
                      <td>
                        <span className="mono">{counterparty}</span>
                        {counterpartyCustomer && (
                          <span className="muted">
                            {' '}
                            · {counterpartyCustomer.firstName} {counterpartyCustomer.lastName}
                          </span>
                        )}
                      </td>
                      <td className={`numeric ${isOutgoing ? 'amount-out' : 'amount-in'}`}>
                        {isOutgoing ? '−' : '+'}
                        {formatCurrency(transaction.amount)}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
