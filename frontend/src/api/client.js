// Cliente HTTP unico para toda la app.
//
// Centralizar aqui el fetch evita repetir en cada componente el manejo de
// cabeceras, de respuestas vacias (DELETE devuelve 204 sin cuerpo) y de los
// dos formatos de error que devuelve el backend:
//   - GlobalExceptionHandler  -> { "message": "..." }
//   - TransactionController   -> un string plano en el cuerpo

const BASE_URL = 'http://localhost:8088/api'

async function request(path, options = {}) {
  let response

  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    // fetch solo rechaza por fallo de red, no por codigos 4xx/5xx.
    throw new Error(
      'No se pudo conectar con el servidor. Verifica que la aplicacion Spring Boot este corriendo en el puerto 8088.'
    )
  }

  const rawBody = await response.text()

  let body = null
  if (rawBody) {
    try {
      body = JSON.parse(rawBody)
    } catch {
      body = rawBody
    }
  }

  if (!response.ok) {
    throw new Error(extractErrorMessage(body, response.status))
  }

  return body
}

function extractErrorMessage(body, status) {
  if (typeof body === 'string' && body.trim()) return body
  if (body && typeof body === 'object') {
    if (body.message) return body.message
    if (body.error) return body.error
  }
  return `El servidor respondio con un error ${status}`
}

export const customersApi = {
  getAll: () => request('/customers'),
  getById: (id) => request(`/customers/${id}`),
  create: (customer) =>
    request('/customers', { method: 'POST', body: JSON.stringify(customer) }),
  update: (id, customer) =>
    request(`/customers/${id}`, { method: 'PUT', body: JSON.stringify(customer) }),
  remove: (id) => request(`/customers/${id}`, { method: 'DELETE' }),
}

export const transactionsApi = {
  getByAccount: (accountNumber) =>
    request(`/transactions/${encodeURIComponent(accountNumber)}`),
  transfer: (transaction) =>
    request('/transactions', { method: 'POST', body: JSON.stringify(transaction) }),
}

// --- Utilidades de formato -------------------------------------------------

const currencyFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  minimumFractionDigits: 2,
})

export function formatCurrency(value) {
  if (value === null || value === undefined) return '—'
  return currencyFormatter.format(value)
}

export function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
