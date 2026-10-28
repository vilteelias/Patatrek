# Devoluciones: registrar devolución de reserva (CU-27)

## Qué agrega

**Administrador** (`/admin/devoluciones`, como el prototipo pág. 31; se habilita *Devoluciones* en el menú)

- Arriba hay tres indicadores: **Reservas pendientes**, **Monto total a reintegrar** y **Devoluciones realizadas**.
- Una tabla muestra las reservas en estado **Pendiente de devolución** (paso 2 del CU). Para cada una aparecen:
  - el N.º de reserva;
  - el cliente titular;
  - la excursión y la salida;
  - el **motivo** (cancelación por contingencia o rechazo de una reprogramación);
  - el **monto abonado**.

  Debajo se ven las que ya se devolvieron, con la marca **✓ Devuelta**.
- **Ejecutar devolución** abre la confirmación, con el pago original y el **monto a reintegrar**: todo lo abonado, sea la seña o el pago completo. Al confirmar:
  1. Se crea la transacción de reintegro **Pendiente** (tabla `pago`, modo `REINTEGRO`).
  2. Se envía a la **pasarela simulada** con el N.º de la transacción original.
  3. La pasarela responde con el **N.º de reintegro** (`REI-…`).
  4. La reserva pasa a **Devuelta** y el pago original a **Devuelto**.
  5. Se manda el **comprobante por correo**, que en la consola del backend aparece como `[Correo simulado]`. Lleva la fecha, el motivo, el N.º de reserva y el monto (RF-24).
  6. Aparece el mensaje *"Reintegro finalizado"* y la lista se actualiza.
- **Flujo 2a:** si no quedan pendientes, aparece *"No hay reembolsos pendientes por tramitar."*
- El backend no deja devolver dos veces la misma reserva, ni una que no esté *Pendiente de devolución*.
- **Sigue sin haber datos de tarjeta (corrección 2):** el reintegro vuelve al medio con el que pagó el cliente y PataTrek nunca lo conoce.

En **Reservas** (CU-20), esas reservas ahora muestran el pago como *Devuelto*. En el detalle se ve la transacción de *Reintegro*.

## Datos de ejemplo (se cargan solos, sin resetear la base)

Las reservas pendientes de devolución las genera la **contingencia de salida (CU-14)**, que es parte de Salidas. Para poder mostrar este caso de uso ya mismo, si todavía no hay ninguna, el backend carga:

| Reserva de | Salida | Motivo | Pagó |
|---|---|---|---|
| Julián Ferreyra (DNI 36444555) | Perito Moreno en 5 días, **Cancelada** | Contingencia climática | Seña (50 %) |
| Melina Cabral (DNI 40222333) | La misma | Contingencia climática | Total |
| Diego Sosa (DNI 32111999) | Laguna del Bosque en 16 días, reprogramada | Rechazó la reprogramación | Total |
| Paula Medina (DNI 41555666) | Perito Moreno cancelada | Contingencia climática | **Ya devuelta** (historial) |

Cuando esté la contingencia de mi compañera, sus reservas *Pendiente de devolución* aparecen en esta misma pantalla sin cambiar nada.

## Cómo agregarlo

Este zip va **después del de autenticación**: reemplaza `ReservaService.java`, `ReservaRepository.java`, `PasarelaPagosSimulada.java`, `ModoPago.java`, `App.jsx` y `formato.js`.

1. En IntelliJ: `main` → **Git → Pull**. Tiene que estar mergeado lo de autenticación.
2. `git checkout -b devoluciones`
3. Descomprimí:
   ```powershell
   Expand-Archive -Path "$HOME\Downloads\patatrek-devoluciones.zip" -DestinationPath "E:\Patatrek" -Force
   ```
4. **Reiniciá el backend.** Levantá el frontend y probalo.
5. Commit (con los Unversioned Files), push y pull request `devoluciones` → `main`.

No hace falta tocar `application.properties` ni la base:
- la tabla `pago` ya estaba;
- `REINTEGRO` entra en `modo_pago VARCHAR(10)`;
- `DEVUELTO` ya figuraba en `estado_pago`.

Las pruebas nuevas están en `DevolucionApiTest`, que son 4 tests.

## Para mostrarlo en la demo (unos 2 minutos)

1. Como **administrador**, entrá a **Devoluciones**. Mostrá los indicadores y el motivo de cada reserva.
2. **Ejecutar devolución** en la de Julián Ferreyra. *"Pagó solo la seña, así que se le devuelve la seña completa."* Confirmá: aparece el N.º de reintegro y la fila pasa a **✓ Devuelta**.
3. Mostrá el comprobante en la consola del backend.
4. En **Reservas**, filtrá por el DNI `36444555`: estado *Devuelta*, pago *Devuelto*. En el detalle se ve la transacción de *Reintegro*.
5. Si querés mostrar el flujo 2a, devolvé las otras dos: aparece *"No hay reembolsos pendientes por tramitar"*.

## Endpoints nuevos

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/admin/devoluciones` | `{ pendientes, montoTotalPendiente, realizadas }` |
| POST | `/api/admin/devoluciones/{idReserva}` | Ejecuta el reintegro total → `{ importeReintegrado, nroTransaccionReintegro, mensaje, devolucion }` |
