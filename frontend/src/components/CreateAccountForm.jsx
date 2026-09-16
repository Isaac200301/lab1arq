import { useState } from 'react'
import { customersApi } from '../api/client'

const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  accountNumber: '',
  balance: '',
}

/**
 * Formulario de creacion de clientes.
 * Consume POST /api/customers
 */
export default function CreateAccountForm({ onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState(null)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFeedback(null)

    const balance = Number(form.balance)
    if (Number.isNaN(balance) || balance < 0) {
      setFeedback({ type: 'error', text: 'El saldo inicial debe ser un numero mayor o igual a cero.' })
      return
    }

    setSubmitting(true)
    try {
      const created = await customersApi.create({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        accountNumber: form.accountNumber.trim(),
        balance,
      })
      setForm(EMPTY_FORM)
      setFeedback({
        type: 'success',
        text: `Cliente ${created.firstName} ${created.lastName} creado con la cuenta ${created.accountNumber}.`,
      })
      onCreated?.()
    } catch (error) {
      setFeedback({ type: 'error', text: error.message })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h2 className="card-title">Crear cliente</h2>

      <form className="form-grid" onSubmit={handleSubmit}>
        <label className="field">
          <span>Nombre</span>
          <input
            name="firstName"
            value={form.firstName}
            onChange={handleChange}
            maxLength={50}
            required
          />
        </label>

        <label className="field">
          <span>Apellido</span>
          <input
            name="lastName"
            value={form.lastName}
            onChange={handleChange}
            maxLength={50}
            required
          />
        </label>

        <label className="field">
          <span>Numero de cuenta</span>
          <input
            name="accountNumber"
            value={form.accountNumber}
            onChange={handleChange}
            placeholder="1234567890"
            required
          />
        </label>

        <label className="field">
          <span>Saldo inicial</span>
          <input
            name="balance"
            type="number"
            step="0.01"
            min="0"
            value={form.balance}
            onChange={handleChange}
            placeholder="0.00"
            required
          />
        </label>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creando…' : 'Crear cliente'}
          </button>
        </div>
      </form>

      {feedback && <p className={`alert alert-${feedback.type}`}>{feedback.text}</p>}
    </section>
  )
}
