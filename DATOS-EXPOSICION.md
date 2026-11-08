# Datos de la exposición — PataTrek

Al arrancar con la base vacía, el backend carga un conjunto completo de datos (`DatosExposicion.java`):

- **Catálogo:** 7 excursiones (6 activas), 12 ítems de equipamiento y 5 refugios.
- **Usuarios:** 2 administradores, 7 guías y 26 clientes.
- **Operación:** 29 salidas en todos los estados y 64 reservas con 122 participantes.
- **Historial:** pagos, certificados, asistencias, valoraciones y contingencias.

**Las fechas se calculan desde el día en que se cargan.** Por eso, para el viernes 20/11 conviene cargarlos ese mismo día a la mañana: así "hoy" es el día de la exposición. Para la documentación del 9/11 se pueden cargar ahora y volver a cargarlos el 20.

## Cómo cargarlos (SQL Server)

> ⚠️ Esto **borra todo** lo que haya en la base `PataTrek` y la vuelve a crear.

```powershell
cd E:\Patatrek\backend
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=create"
```

En la consola tiene que aparecer `Datos de la exposición cargados: ...`. Después detené el backend y, de ahí en adelante, levantalo como siempre (desde IntelliJ o con `.\mvnw spring-boot:run`): los datos se mantienen.

- Los certificados de ejemplo se guardan como PDF en `backend/uploads/certificados/`, así que se pueden abrir desde el panel del administrador.
- Las pruebas automáticas (`.\mvnw test`) siguen usando sus propios datos chicos (perfil `demo`) y no tocan tu base.

## Cuentas

Todas tienen la contraseña **`Patatrek2026`**.

| Rol | Correo | Para qué sirve en la exposición |
|---|---|---|
| Administrador | `admin@patatrek.com` | Todo el panel |
| Administrador | `hernan.gallardo@patatrek.com` | Segundo administrador |
| Guía | `martin.almeida@patatrek.com` | Tiene **hoy** el Ascenso al Perito Moreno, listo para el control de asistencia |
| Guía | `valentina.ortiz@patatrek.com` | Tiene **hoy** la Laguna del Bosque **en curso**, para marcar la finalización |
| Guía | `federico.nancucheo@patatrek.com` | Travesía de 3 días en curso (empezó ayer) |
| Guía | `brenda.coronel@patatrek.com`, `lucas.ibarra@patatrek.com`, `carla.huenchul@patatrek.com`, `joaquin.rivas@patatrek.com` | Salidas como responsable y como ayudante |
| Cliente | `sofia.ibarra@mail.com` | La clienta "completa" (ver abajo) |
| Cliente | `rocio.delgado@mail.com`, `bruno.molina@mail.com`, `lucia.fernandez@mail.com`, `pablo.herrera@mail.com`, … | Otros clientes con reservas |

Los correos `@mail.com` de los datos son ficticios: el sistema simula esos envíos y no manda nada real. Para mostrar un correo real, registrá en vivo un cliente con su Gmail y hacé una reserva con él.

## Qué mostrar en cada caso de uso

| CU | Dónde y con quién |
|---|---|
| CU-01 / 02 / 03 | Registrar un cliente nuevo, iniciar sesión, cerrar sesión |
| CU-04 a 08 | Administrador → Excursiones. La Travesía Río Azul está dada de baja. |
| CU-09 Planificar salida | Administrador → Salidas → Planificar salida |
| CU-10 Listar salidas | Administrador → Salidas: hay salidas Disponibles, Completas, En curso, Finalizadas y Canceladas |
| CU-11 / 12 / 13 | El **Ascenso al Perito Moreno de dentro de 19 días** no tiene reservas: sirve para Modificar, Personal y cupos y Dar de baja. La **Laguna de dentro de 6 días** está **Completa**: si le agregás un ayudante, vuelve a Disponible. |
| CU-14 Contingencias | El **Glaciar Torrecillas en 9 días** ya fue reprogramado por nevadas. El **Bosque de Arrayanes en 15 días** tiene el recorrido modificado. El **Perito Moreno en 5 días** se canceló por viento. Para registrar una en vivo, usá cualquier salida Disponible. |
| CU-15 Control de asistencia | Guía Martín Almeida → salida de hoy del Perito Moreno. Hay participantes con certificado aprobado, uno con certificado sin revisar y uno sin certificado, y equipamiento prestado. |
| CU-16 Marcar finalización | Guía Valentina Ortiz → Laguna del Bosque de hoy (en curso) |
| CU-17 Ver observación | Administrador → Salidas → en una salida Finalizada, **Ver observación** |
| CU-18 Realizar reserva | Cualquier cliente → catálogo → una salida futura (por ejemplo, la Laguna en 27 días) |
| CU-19 Consultar y gestionar reserva | Sofía → Mis reservas. **Como participante:** pie de página o login → "Consultala con el N.º de reserva y tu DNI" → la reserva **señada** de Sofía en la Travesía (en 12 días) con el DNI de su acompañante **Mateo Ibarra: `45111222`** |
| CU-20 / 21 | Administrador → Reservas (con filtros); Sofía → Mis reservas |
| CU-22 Listar participantes | Cualquier guía → Ver participantes |
| CU-23 Cancelar reserva | Sofía u otro cliente, en una reserva futura confirmada o señada |
| CU-24 Cargar certificado | Sofía en la Travesía (su certificado está cargado, el de Mateo pendiente), o Mateo como participante |
| CU-25 Validar certificados | Administrador → Certificados médicos: hay varios **Cargados** para revisar, y uno ya rechazado una vez |
| CU-26 Realizar pago | Sofía paga el saldo de la Travesía, o Mateo lo paga como participante. La pasarela aprueba o rechaza al azar. |
| CU-27 Registrar devolución | Administrador → Devoluciones: hay **3 reservas pendientes** (Perito Moreno cancelado por viento) y el historial de las ya devueltas |
| CU-28 Registrar valoración | Sofía → Mis reservas → la Laguna de hace 4 días (todavía no la valoró) |
| CU-29 Consultar valoraciones | Administrador → Valoraciones: 17 valoraciones de 2 a 5 estrellas |
| CU-30 Cancelación automática | Administrador → Reservas → Ejecutar cancelación automática. Cancela **2 reservas** de salidas de dentro de 2 días: una con saldo impago (Bosque de Arrayanes) y una sin certificados (Glaciar). Las otras reservas de esas salidas cumplen las condiciones y no se cancelan. |
| Reprogramación (cliente) | Sofía → Mis reservas → Glaciar Torrecillas: aviso "Tu salida fue reprogramada" para aceptar o rechazar |
| Equipamiento | Administrador → Equipamiento → elegí una salida y tocá **Actualizar disponibilidad** |
