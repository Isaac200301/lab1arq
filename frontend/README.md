# Frontend — UdeaBank (lab1arq)

Frontend en React + Vite que consume la API REST de Spring Boot del laboratorio.

## Requisitos

- Node.js 18 o superior (`node -v` para verificar)
- El backend de Spring Boot corriendo en `http://localhost:8088`
- MySQL corriendo con el schema `udeabank`

## Cómo ejecutarlo

Desde esta carpeta (`lab1arq/frontend`):

```bash
npm install     # solo la primera vez
npm run dev
```

Abre `http://localhost:5173`.

**Importante:** el backend debe estar arriba *antes* de usar la app. Si no, todas
las vistas muestran un error de conexión.

## Las tres vistas

| Pestaña | Qué hace | Endpoints que consume |
|---|---|---|
| **Clientes** | Lista todos los clientes, permite crear, editar y eliminar | `GET/POST /api/customers`, `PUT/DELETE /api/customers/{id}` |
| **Transferencias** | Transfiere dinero entre dos cuentas, validando saldo | `POST /api/transactions` |
| **Histórico** | Movimientos de un cliente, con totales enviados y recibidos | `GET /api/transactions/{accountNumber}` |

## Estructura

```
frontend/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.jsx                      punto de entrada
    ├── App.jsx                       layout, navegación y estado compartido
    ├── api/
    │   └── client.js                 cliente HTTP + utilidades de formato
    ├── components/
    │   ├── CustomerList.jsx          vista 1
    │   ├── CreateAccountForm.jsx     formulario de alta (dentro de la vista 1)
    │   ├── TransferFunds.jsx         vista 2
    │   └── TransactionHistory.jsx    vista 3
    └── css/
        ├── index.css                 variables y reset
        └── App.css                   estilos de la aplicación
```

## Decisiones de diseño

**La lista de clientes vive en `App.jsx`, no en cada vista.** Las tres pantallas
la necesitan y una transferencia cambia los saldos, así que tener una sola
fuente de verdad evita que una vista muestre datos viejos después de una
operación hecha en otra.

**Un solo cliente HTTP en `api/client.js`.** Concentra el manejo de cabeceras,
de respuestas vacías (`DELETE` responde 204 sin cuerpo) y de los dos formatos
de error que devuelve el backend.

**Sin router.** Con tres vistas y sin necesidad de URLs compartibles, un estado
`activeTab` alcanza y evita una dependencia más.

## CORS

La comunicación entre el puerto 5173 (Vite) y el 8088 (Spring) la habilita la
clase `com.udea.lab1arq.config.WebConfig` del backend. Si cambias el puerto del
frontend, hay que agregar el nuevo origen en `allowedOrigins`.
