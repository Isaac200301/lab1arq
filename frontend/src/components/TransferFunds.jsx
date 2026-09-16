import { useMemo, useState } from 'react'
import { transactionsApi, formatCurrency } from '../api/client'

/**
 * Vista 2: Realizar transferencia de dinero entre cuentas.
 * Consume POST /api/transactions
 */
export default function TransferFunds({ customers, loading, onTransferCompleted }) {
  const [senderAccountNumber, setSenderAccountNumber] = useState('')
  const [receiverAccountNumber, setReceiverAccountNumber] = useState('')
  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState(null)

  const sender = useMemo(
    () => customers.find((customer) => customer.accountNumber === senderAccountNumber),
    [customers, senderAccountNumber]
  )
  const receiver = useMemo(
    () => customers.find((customer) => customer.accountNumber === receiverAccountNumber),
    [customers, receiverAccountNumber]
  )

  const numericAmount = Number(amount)
  const amountIsValid = amount !== '' && !Number.isNaN(numericAmount) && numericAmount > 0
  const sameAccount =
    senderAccountNumber !== '' && senderAccountNumber === receiverAccountNumber
  const insufficientFunds =
    sender && amountIsValid && sender.balance < numericAmount

  const canSubmit =
    senderAccountNumber &&
    receiverAccountNumber &&
    amountIsValid &&
    !sameAccount &&
    !insufficientFunds &&
    !submitting

  async function handleSubmit(event) {
    event.preventDefault()
    setFeedback(null)
    setSubmitting(true)

    try {
      // No se envia timestamp: la marca de tiempo la asigna el servidor.
      const transaction = await transactionsApi.transfer({
        senderAccountNumber,
        receiverAccountNumber,
        amount: numericAmount,
      })

      setFeedback({
        type: 'success',
        text: `Transferencia #${transaction.id} completada: ${formatCurrency(
          transaction.amount
        )} de ${transaction.senderAccountNumber} a ${transaction.receiverAccountNumber}.`,
      })
      setAmount('')
      onTransferCompleted?.()
    } catch (error) {
      setFeedback({ type: 'error', text: error.message })
    } finally {
      setSubmitting(false)
    }
  }

  if (!loading && customers.length < 2) {
    return (
      <div className="view">
        <section className="card">
          <h2 className="card-title">Transferir dinero</h2>
          <p className="state">
            Se necesitan al menos dos clientes registrados para poder transferir. Ve a la
            pestaña «Clientes» y crea los que falten.
          </p>
        </section>
      </div>
    )
  }

  return (
    <div className="view">
      <section className="card">
        <h2 className="card-title">Transferir dinero</h2>

        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field">
            <span>Cuenta de origen</span>
            <select
              value={senderAccountNumber}
              onChange={(event) => setSenderAccountNumber(event.target.value)}
              required
            >
              <option value="">Selecciona una cuenta…</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.accountNumber}>
                  {customer.accountNumber} — {customer.firstName} {customer.lastName}
                </option>
              ))}
            </select>
            {sender && (
              <small className="hint">Saldo disponible: {formatCurrency(sender.balance)}</small>
            )}
          </label>

          <label className="field">
            <span>Cuenta de destino</span>
            <select
              value={receiverAccountNumber}
              onChange={(event) => setReceiverAccountNumber(event.target.value)}
              required
            >
              <option value="">Selecciona una cuenta…</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.accountNumber}>
                  {customer.accountNumber} — {customer.firstName} {customer.lastName}
                </option>
              ))}
            </select>
            {receiver && (
              <small className="hint">Saldo actual: {formatCurrency(receiver.balance)}</small>
            )}
          </label>

          <label className="field field-wide">
            <span>Monto a transferir</span>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              placeholder="0.00"
              required
            />
          </label>

          {sameAccount && (
            <p className="alert alert-error field-wide">
              La cuenta de origen y la de destino no pueden ser la misma.
            </p>
          )}

          {insufficientFunds && (
            <p className="alert alert-error field-wide">
              Saldo insuficiente. {sender.firstName} tiene {formatCurrency(sender.balance)}.
            </p>
          )}

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={!canSubmit}>
              {submitting ? 'Procesando…' : 'Transferir'}
            </button>
          </div>
        </form>

        {feedback && <p className={`alert alert-${feedback.type}`}>{feedback.text}</p>}
      </section>
    </div>
  )
}
