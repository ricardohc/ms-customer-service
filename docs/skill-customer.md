# Skill — ms-customer-service

> Contrato: `ms-customer-service-openapi.yaml` (fuente de verdad).
> Reglas transversales: ver `common-rules.md`.
> **Reactividad**: Spring WebFlux + Reactor (`Mono`/`Flux`). No usa RxJava.

## Responsabilidad
- Clientes `PERSONAL` y `BUSINESS`.
- Documento único (DNI, CEX, PASSPORT, RUC).
- Perfil `STANDARD`, `VIP`, `PYME`.
- Estado `ACTIVE`, `INACTIVE`.
- Baja lógica (no borrado físico) con validación de productos activos.

## Endpoints
| Método | Ruta | operationId | Código esperado |
|---|---|---|---|
| GET | `/api/v1/customers` | `findAllCustomers` | 200 |
| POST | `/api/v1/customers` | `createCustomer` | 201 |
| GET | `/api/v1/customers/{customerId}` | `findCustomerById` | 200 |
| PATCH | `/api/v1/customers/{customerId}` | `updateCustomer` | 200 |
| DELETE | `/api/v1/customers/{customerId}` | `deactivateCustomer` | 204 |

## Headers
- Todas las operaciones: `X-Correlation-ID`, `X-Channel-ID`.
- POST, PATCH, DELETE: `X-Transaction-ID` (UUID).

## Persistencia
- Base: `banking_customer`.
- Colección: `customers`.
- Escritura directa a MongoDB propia vía `ReactiveMongoRepository`.
- Índice único: `(documentType, documentNumber)`.

## Reglas de negocio
- `POST` valida unicidad de `documentType + documentNumber`. Duplicado → `409`.
- `PATCH` permite actualizar datos de contacto y perfil. **No** permite cambiar `customerType` ni `documentNumber`.
- `DELETE` es baja lógica: cambia `status` a `INACTIVE`. Si el cliente tiene productos activos → `409`.
- **Perfil vs requisito de producto**:
  - El perfil `VIP` o `PYME` **se asigna** al cliente cuando cumple la condición de negocio; **no** exige tarjeta de crédito en `ms-customer-service`.
  - La **tarjeta de crédito es requisito para adquirir productos** VIP/PYME (cuenta de ahorro VIP o cuenta corriente PYME), y esa validación la realiza `ms-account-service` al abrir la cuenta.
  - `ms-customer-service` **no** consulta a `ms-card-service` para asignar el perfil. Solo guarda el perfil.
- PERSONAL: máximo una SAVINGS y una CHECKING (validado por `ms-account-service`).
- BUSINESS: no puede tener SAVINGS ni FIXED_TERM (validado por `ms-account-service`).

## Eventos (Parte III)
- Tópico: `banking.customer.events.v1`.
- Productor: `ms-customer-service`.
- Eventos: `CustomerCreated`, `CustomerUpdated`, `CustomerDeactivated`.
- Envelope obligatorio (ver `skill-transaction-kafka.md`).
- Outbox con `outbox=true`, at-least-once, deduplicación por `eventId`.
- **Estado del contrato**: los payloads deben formalizarse en el catálogo Kafka oficial antes de implementarse.

## Pruebas mínimas
- Crear PERSONAL y BUSINESS válidos → 201.
- Documento duplicado → 409.
- PATCH sin cambiar tipo/documento.
- DELETE con productos activos → 409.
- DELETE sin productos → 204 y estado INACTIVE.
- Asignar perfil VIP/PYME sin tarjeta de crédito → permitido en este servicio.

## Criterios de aceptación
1. Endpoints y DTOs idénticos al YAML.
2. Persistencia solo en `banking_customer`.
3. Reglas de perfil y baja lógica cubiertas por tests.
4. No se leen bases de otros servicios.
5. Controladores reactivos con Reactor (`Mono`/`Flux`).