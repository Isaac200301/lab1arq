import { useState } from 'react'
import { customersApi, formatCurrency } from '../api/client'
import CreateAccountForm from './CreateAccountForm'

/**
 * Vista 1: Consultar clientes.
 * Consume GET /api/customers, PUT /api/customers/{id} y DELETE /api/customers/{id}
 */
export default function CustomerList({ customers, loading, error, onRefresh }) {
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [feedback, setFeedback] = useState(null)
  const [search, setSearch] = useState('')

  function startEditing(customer) {
    setFeedback(null)
    setEditingId(customer.id)
    setDraft({
      firstName: customer.firstName ?? '',
      lastName: customer.lastName ?? '',
      accountNumber: customer.accountNumber ?? '',
      balance: customer.balance ?? 0,
    })
  }

  function cancelEditing() {
    setEditingId(null)
    setDraft(null)
  }

  function handleDraftChange(event) {
    const { name, value } = event.target
    setDraft((previous) => ({ ...previous, [name]: value }))
  }

  async function saveEdit(id) {
    setBusyId(id)
    setFeedback(null)
    try {
      await customersApi.update(id, {
        firstName: draft.firstName.trim(),
        lastName: draft.lastName.trim(),
        accountNumber: draft.accountNumber.trim(),
        balance: Number(draft.balance),
      })
      cancelEditing()
      setFeedback({ type: 'success', text: 'Cliente actualizado.' })
      onRefresh?.()
    } catch (err) {
      setFeedback({ type: 'error', text: err.message })
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(customer) {
    const confirmed = window.confirm(
      `Se va a eliminar a ${customer.firstName} ${customer.lastName} (cuenta ${customer.accountNumber}). Esta accion no se puede deshacer.`
    )
    if (!confirmed) return

    setBusyId(customer.id)
    setFeedback(null)
    try {
      await customersApi.remove(customer.id)
      setFeedback({ type: 'success', text: 'Cliente eliminado.' })
      onRefresh?.()
    } catch (err) {
      setFeedback({ type: 'error', text: err.message })
    } finally {
      setBusyId(null)
    }
  }

  const term = search.trim().toLowerCase()
  const visibleCustomers = term
    ? customers.filter((customer) =>
        [customer.firstName, customer.lastName, customer.accountNumber]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(term))
      )
    : customers

  const totalBalance = customers.reduce((sum, customer) => sum + (customer.balance ?? 0), 0)

  return (
    <div className="view">
      <CreateAccountForm onCreated={onRefresh} />

      <section className="card">
        <div className="card-header">
          <h2 className="card-title">Clientes registrados</h2>
          <button type="button" className="btn btn-ghost" onClick={onRefresh} disabled={loading}>
            {loading ? 'Cargando…' : 'Refrescar'}
          </button>
        </div>

        {customers.length > 0 && (
          <div className="toolbar">
            <input
              className="search"
              type="search"
              placeholder="Buscar cliente o cuenta…"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <div className="summary">
              <span>{customers.length} cliente{customers.length === 1 ? '' : 's'}</span>
              <span className="summary-divider" aria-hidden="true" />
              <span>Saldo total {formatCurrency(totalBalance)}</span>
            </div>
          </div>
        )}

        {feedback && <p className={`alert alert-${feedback.type}`}>{feedback.text}</p>}
        {error && <p className="alert alert-error">{error}</p>}

        {loading && customers.length === 0 && <p className="state">Cargando clientes…</p>}

        {!loading && !error && customers.length === 0 && (
          <p className="state">Todavia no hay clientes. Crea el primero con el formulario de arriba.</p>
        )}

        {visibleCustomers.length === 0 && customers.length > 0 && (
          <p className="state">Ningun cliente coincide con «{search}».</p>
        )}

        {visibleCustomers.length > 0 && (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nombre</th>
                  <th>Apellido</th>
                  <th>Numero de cuenta</th>
                  <th className="numeric">Saldo</th>
                  <th className="actions-column">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {visibleCustomers.map((customer) => {
                  const isEditing = editingId === customer.id
                  const isBusy = busyId === customer.id

                  return (
                    <tr key={customer.id}>
                      <td className="muted">{customer.id}</td>

                      {isEditing ? (
                        <>
                          <td>
                            <input
                              name="firstName"
                              value={draft.firstName}
                              onChange={handleDraftChange}
                              maxLength={50}
                            />
                          </td>
                          <td>
                            <input
                              name="lastName"
                              value={draft.lastName}
                              onChange={handleDraftChange}
                              maxLength={50}
                            />
                          </td>
                          <td>
                            <input
                              name="accountNumber"
                              value={draft.accountNumber}
                              onChange={handleDraftChange}
                            />
                          </td>
                          <td className="numeric">
                            <input
                              name="balance"
                              type="number"
                              step="0.01"
                              min="0"
                              value={draft.balance}
                              onChange={handleDraftChange}
                            />
                          </td>
                          <td className="actions-column">
                            <button
                              type="button"
                              className="btn btn-small btn-primary"
                              onClick={() => saveEdit(customer.id)}
                              disabled={isBusy}
                            >
                              {isBusy ? 'Guardando…' : 'Guardar'}
                            </button>
                            <button
                              type="button"
                              className="btn btn-small btn-ghost"
                              onClick={cancelEditing}
                              disabled={isBusy}
                            >
                              Cancelar
                            </button>
                          </td>
                        </>
                      ) : (
                        <>
                          <td>{customer.firstName}</td>
                          <td>{customer.lastName}</td>
                          <td className="mono">{customer.accountNumber}</td>
                          <td className="numeric">{formatCurrency(customer.balance)}</td>
                          <td className="actions-column">
                            <button
                              type="button"
                              className="btn btn-small btn-ghost"
                              onClick={() => startEditing(customer)}
                              disabled={isBusy}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className="btn btn-small btn-danger"
                              onClick={() => handleDelete(customer)}
                              disabled={isBusy}
                            >
                              {isBusy ? 'Eliminando…' : 'Eliminar'}
                            </button>
                          </td>
                        </>
                      )}
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
