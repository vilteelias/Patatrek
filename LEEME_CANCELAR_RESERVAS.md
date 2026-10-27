# Cancelar reserva (CU-23) + Mis reservas con filtros (CU-21) + Reservas del administrador (CU-20)

## Qué agrega

**Cliente**
- **Mis reservas** ahora tiene filtros, como en el prototipo (pág. 13): *Salida desde*, *Salida hasta*, *Estado de reserva* y *Limpiar filtros*. Cada reserva muestra el importe total, el importe pendiente, la fecha de inicio, el estado del pago y el enlace *Gestionar reserva*.
- En el detalle de una reserva **Señada o Confirmada** aparece el botón **Cancelar reserva** (prototipo pág. 15):
  - El aviso dice que se cancelan todos los servicios, que **no se devuelve lo abonado** (con el monto) y que la acción es **irreversible**.
  - El **motivo es opcional** (flujo 3.b).
  - *No, mantener reserva* → muestra *"No se realizaron cambios en la reserva"* (flujo 3.a).
  - *Sí, cancelar reserva* → se liberan los **cupos**, las **plazas de refugio** y el **equipamiento**, la reserva queda **Cancelada**, se manda el correo (simulado en la consola) y se vuelve a *Mis reservas* con el aviso.
  - No se puede cancelar una reserva *Pendiente de pago*, porque esa se libera sola a los 10 minutos. Tampoco una de una salida en curso o finalizada.

**Administrador** (`/admin/reservas`, se habilita *Reservas* en el menú lateral)
- Catálogo general de todas las reservas (RF-17), con filtros por **nombre de excursión**, **ID de salida**, **DNI del cliente**, **estado** y **rango de fechas de la salida**.
- Columnas: reserva y fecha, excursión y salida, cliente titular con DNI, participantes, estado de la reserva, **estado del pago** (Aprobado / Pendiente / Rechazado) y total.
- Al hacer clic en una fila se abre el detalle: participantes, alojamiento, equipamiento y pagos con su N.º de transacción.

## Cómo agregarlo

Este zip se aplica **sobre lo de reserva y pago**. Hay dos casos:

**A) Si todavía NO mergeaste `reserva-pago` a `main`:**
1. Quedate en la rama `reserva-pago`.
2. Descomprimí el zip en `E:\Patatrek` y aceptá reemplazar.
3. Levantá, probá, y hacé commit y push en la misma rama. El pull request se actualiza solo.

**B) Si ya la mergeaste:**
1. `git checkout main`, después **Git → Pull** en IntelliJ.
2. Creá una rama nueva:
   ```powershell
   git checkout -b reserva-cancelar
   ```
3. Descomprimí el zip en `E:\Patatrek`, aceptá reemplazar, probá, hacé commit y push, y abrí el pull request.

No hace falta tocar `application.properties` ni la base. Los archivos que se reemplazan son todos de reserva, salvo dos:
- `ManejadorErrores.java`: se agrega el mensaje para un filtro inválido en la URL.
- `App.jsx`: se agrega la ruta `/admin/reservas`.

## Datos de ejemplo

Con la base **reseteada**, las reservas de ejemplo ahora tienen su **pago aprobado registrado**, así el administrador ve los pagos. Si no reseteás, igual figuran como *Aprobado* en la columna *Pago*, pero en el detalle dicen "Sin pagos registrados".

## Para mostrarlo en la demo (unos 2 minutos)

1. Como cliente: reservá **Sendero Laguna del Bosque** y pagá. Después, en **Mis reservas** → *Gestionar reserva* → **Cancelar reserva**.
2. Mostrá el aviso de que no hay devolución. Tocá *No, mantener reserva* para mostrar que no cambia nada. Volvé a abrir, escribí un motivo y confirmá.
3. En **Mis reservas**, filtrá por estado *Cancelada*.
4. Entrá a `http://localhost:5173/admin/reservas`:
   - Filtrá por DNI `38555777` (Sofía).
   - Abrí la reserva cancelada: se ve el motivo y el pago.
   - Mostrá que la salida volvió a tener los cupos (desde *Ver salidas* en el catálogo).

## Endpoints nuevos

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/cliente/reservas?desde=&hasta=&estado=` | CU-21 con filtros (todos opcionales) |
| POST | `/api/reservas/{id}/cancelacion` | CU-23. Cuerpo `{ "motivo": "..." }`, motivo opcional |
| GET | `/api/admin/reservas?excursion=&idSalida=&desde=&hasta=&dni=&estado=` | CU-20 |
| GET | `/api/admin/reservas/{id}` | Detalle para el administrador |
