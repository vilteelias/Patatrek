# Módulo Reserva y Pago (CU-18 y CU-26)

## Qué incluye

**Cliente (por ahora la clienta de demostración Sofía Ibarra, hasta el módulo de Autenticación)**

- En cada tarjeta del catálogo hay un botón **Ver salidas**. Lleva al detalle de la excursión con sus próximas salidas. Si una salida no se puede reservar, se explica por qué: no quedan cupos, o faltan 72 horas o menos.
- **Asistente de reserva** (`/reservar/:idSalida`), con los mismos pasos que el prototipo:
  1. Participantes
  2. Datos
  3. Alojamiento (solo en salidas de varios días)
  4. Equipamiento (solo si la excursión lo exige)
  5. Resumen
  6. Pago
- **Mis reservas**, el **detalle de cada reserva** y **Pagar el saldo restante** de una reserva señada.

## Las 3 correcciones de la cátedra

| Corrección | Dónde se ve en el programa |
|---|---|
| **Campos obligatorios al registrar un participante** | En el paso "Datos" está la leyenda *"Los campos marcados con * son obligatorios"*. Cada campo lleva un asterisco rojo, y si falta algo el error aparece debajo de ese mismo campo. El backend también valida y responde indicando el campo exacto, por ejemplo `participantes[1].dni`. |
| **Recursos retenidos** | Al confirmar el Resumen, la reserva queda "Pendiente de pago" y se ve un **contador de 10 minutos**. Mientras tanto, los recursos quedan apartados: `salida.cupos_retenidos`, `asignacion_refugio.plazas_retenidas`, `equipamiento.stock_retenido` y `asignacion_equipamiento.estado_asignacion = RETENIDO`. Si el pago se **aprueba**, pasan a ocupados (CONSOLIDADO). Si se **rechaza** o **vence** el plazo, se liberan y la reserva queda Cancelada. Una tarea automática revisa los vencimientos cada 30 segundos. |
| **Sin datos de tarjeta** | Ni PataTrek ni la pasarela simulada piden número, vencimiento, CVV ni titular. La **pasarela se abre en una ventana emergente**, como pidieron las profesoras: el cliente elige un medio que ya tiene guardado en la pasarela (son datos ficticios), y PataTrek solo recibe el resultado. La tabla `pago` guarda solo importe, modo, estado y número de transacción. |

## Paso a paso para agregarlo al repo

1. **Descomprimí** el zip en la carpeta del repo (`E:\Patatrek`). Si te pregunta, **reemplazá** los archivos existentes.
   Los únicos archivos que ya existían y se reemplazan son estos 9 (todo lo demás es nuevo):
   - Backend: `ManejadorErrores.java`, `ClienteRepository.java`, `ReservaRepository.java`,
     `ParticipanteRepository.java` y `AsignacionEquipamientoRepository.java`
   - Frontend: `App.jsx`, `App.css`, `TarjetaExcursion.jsx` y `public-layout.jsx`

   **No se toca nada de `Salida`, `SalidaRepository` ni del paquete `salida`**, así que no choca con lo que está haciendo tu compañera.
2. En `backend/src/main/resources/application.properties` **agregá a mano** estas 2 líneas. No reemplaces el archivo, así no perdés tu usuario y `${DB_PASSWORD}`:
   ```properties
   patatrek.cliente-demo-correo=sofia.ibarra@mail.com
   patatrek.minutos-retencion=10
   ```
3. La base **no hace falta resetearla**. La tabla `pago` ya estaba en el script, y al arrancar el backend se carga sola la clienta Sofía Ibarra junto con estas salidas de ejemplo:
   - **Sendero Laguna del Bosque**, en 7 días: el caso simple. Admite menores y no pide equipamiento.
   - **Ascenso Cerro Perito Moreno**, en 9 días: quedan **2 cupos**, no admite menores y pide Casco y Guantes.
   - **Ascenso Cerro Perito Moreno**, en 2 días: **no se puede reservar** porque faltan 72 horas o menos.
   - Ya existían la **Travesía Cerro Alto**, en 20 días, con refugio y 3 ítems de equipamiento, y el **Circuito Lagos**, en 10 días, sin refugio, así que solo admite "Carpa propia".

   Para la demo del viernes **sí conviene resetear**: corré `00_limpiar_base.sql`, después `patatrek_sqlserver.sql` y reiniciá el backend. Así todas las fechas se calculan desde ese día.
4. Levantá el backend y el frontend como siempre:
   - backend: `.\mvnw spring-boot:run`
   - frontend: `npm run dev`

   Las pantallas nuevas no necesitan ninguna librería nueva.
5. Las pruebas automáticas nuevas están en `ReservaApiTest`, que son 6 tests. Se corren con `.\mvnw test`.
6. Commit: incluí también los archivos **Unversioned Files**. Sigue valiendo excluir `frontend/public/src`, `node_modules`, `target` y `uploads`.

## Guion sugerido para mostrarlo (unos 4 minutos)

1. **Catálogo** → "Ver salidas" en **Ascenso Cerro Perito Moreno**. Mostrá que la salida de dentro de 2 días dice *"Las reservas cierran 72 horas antes"*.
2. Reservá la de dentro de 9 días. En **Participantes**, el botón + no deja pasar de 2 (cupos disponibles).
3. En **Datos**, tocá Continuar con todo vacío: aparecen los asteriscos y un error debajo de cada campo (**corrección 3**). Poné una fecha de nacimiento de un menor: *"Esta excursión no admite menores"*.
4. En **Equipamiento**, pedí 1 Casco. En el **Resumen** se ve el monto, junto con el aviso de que los recursos quedan retenidos 10 minutos.
5. Tocá **Continuar al pago**. Mostrá el **contador** (**corrección 1**) y que **no hay campos de tarjeta** (**corrección 2**).
6. Elegí **Seña** y tocá **Ir a la pasarela de pagos**. Se abre la **ventana emergente de la pasarela**, con el importe y los medios de pago. Elegí uno, dejá "Aprobar pago" y tocá **Pagar**. La pasarela procesa, muestra *"¡Listo! Se acreditó tu pago"* con el N.º de operación, y con **Volver a PataTrek** aparece la reserva **Señada**. Si volvés a la excursión, la salida aparece **sin cupos** (quedó Completa).
7. Para el flujo alternativo, en **Sendero Laguna del Bosque** reservá y, en la pasarela, elegí **Rechazar pago**: la reserva queda Cancelada y los cupos vuelven. Si en la pasarela tocás **Cancelar y volver a PataTrek**, no se registra ningún pago (flujo 4c) y la reserva sigue esperando el pago mientras corre el contador. Reservá otra vez y tocá **"Simular vencimiento del plazo de retención"**: aparece *"Venció el plazo para pagar"*.
8. En **Mis reservas** se ven todas, con su estado.

> El correo con el comprobante se muestra en la consola del backend como `[Correo simulado]`, igual que en el módulo del guía.

## Endpoints nuevos

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/cliente/yo` | Clienta de demostración |
| GET | `/api/cliente/reservas` | Mis reservas |
| GET | `/api/excursiones/{id}/salidas` | Próximas salidas, indicando si se pueden reservar |
| GET | `/api/reservas/salidas/{idSalida}` | Datos para el asistente (precio, refugios, equipamiento) |
| POST | `/api/reservas` | CU-18: crea la reserva "Pendiente de pago" y retiene los recursos |
| GET | `/api/reservas/{id}` | Detalle (cancela la reserva si ya venció) |
| POST | `/api/reservas/{id}/pagos` | CU-26: `{ modoPago: SENA/TOTAL/SALDO, aprobar: true/false }`, sin datos de tarjeta |
| POST | `/api/reservas/{id}/simular-vencimiento` | Solo para la demo |
