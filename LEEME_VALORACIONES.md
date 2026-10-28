# Valoraciones: registrar (CU-28) y consultar (CU-29)

## Qué agrega

**Cliente / Participante** (prototipo pág. 7 y 18)
- En **Mis reservas** aparece un recuadro **"Valoraciones pendientes"** con las salidas **finalizadas** en las que el cliente estuvo **Presente y Apto** y todavía no valoró. Cada una tiene el botón **Valorar**.
- El formulario **"¿Cómo fue tu experiencia?"** muestra la excursión, la fecha de la salida, el participante y la acreditación *Presente y Apto*. Tiene:
  - **calificación de 1 a 5 estrellas**, obligatoria. Se puede elegir con el mouse o con las flechas del teclado;
  - **comentario opcional**, de hasta 1000 caracteres.
- Si se envía sin estrellas, se resalta el campo y aparece el mensaje del flujo 5a: *"Debe seleccionar una calificación entre 1 y 5 estrellas para continuar"*.
- Al enviar se muestra *"¡Gracias por tu valoración!"* y se manda el correo de confirmación (simulado en la consola). El formulario se cierra: **una sola valoración por participante**.
- **Participante sin cuenta:** al finalizar una salida (CU-16), el correo que reciben los participantes *Presente y Apto* ahora trae el enlace `http://localhost:5173/valorar?reserva=ID&dni=DNI`. Si alguien entra a `/valorar` sin el enlace, ingresa su **N.º de reserva** y su **DNI**. Esos dos campos también llevan la marca de obligatorio.
- Reglas que se validan en el backend:
  - la salida tiene que estar **Finalizada**;
  - el participante tiene que estar **Presente y Apto**;
  - **no puede haber una valoración previa**.

**Administrador** (`/admin/valoraciones`, se habilita *Valoraciones* en el menú)
- A la izquierda, **todas las excursiones** con su **promedio** en estrellas y el **total de valoraciones**.
- Al elegir una excursión, se ven:
  - el promedio grande;
  - las barras con la cantidad por estrellas (5 a 1);
  - la lista de **calificaciones y comentarios**, con participante, salida y fecha.

## Datos de ejemplo (se cargan solos, sin resetear la base)

Si todavía no hay valoraciones en la base, al arrancar el backend se crea:
- Una salida **Finalizada** del *Sendero Laguna del Bosque* (hace 20 días):
  - con la reserva de **Sofía Ibarra**: ella y Mateo Ibarra, los dos *Presente y Apto* y **sin valorar**, para mostrarlo en la demo;
  - con la reserva de *Marcos Quiroga*, que **ya valoró** (5 ★ él y 4 ★ Lara), más *Nicolás Paz*, que estuvo **Ausente** y no puede valorar.
- Una valoración de 4 ★ de *Elena Castro* en la salida finalizada del *Perito Moreno*.

## Cómo agregarlo

Ya mergeaste `reserva-pago` a `main`, así que:
1. En IntelliJ: cambiá a `main` y hacé **Git → Pull**.
2. Creá una rama nueva:
   ```powershell
   git checkout -b valoraciones
   ```
3. Descomprimí el zip en la raíz del repo:
   ```powershell
   Expand-Archive -Path "$HOME\Downloads\patatrek-valoraciones.zip" -DestinationPath "E:\Patatrek" -Force
   ```
4. Levantá backend y frontend y probalo.
5. Hacé commit (con los Unversioned Files) y push, y después abrí y mergeá el pull request `valoraciones` → `main`.

No hace falta tocar `application.properties` ni la base: la tabla `valoracion` ya estaba en el script.

Archivos que ya existían y se reemplazan:
- `ManejadorErrores.java`: se agrega el mensaje para un dato faltante en la URL.
- `ParticipanteRepository.java`: dos búsquedas nuevas.
- `OperacionTerrenoService.java`: el enlace en el correo de fin de salida.
- `App.jsx`: rutas `/valorar` y `/admin/valoraciones`.
- `MisReservasPagina.jsx`: el recuadro de valoraciones pendientes.
- `ReservaApiTest.java`: dos filtros ajustados por las nuevas reservas de ejemplo.

## Para mostrarlo en la demo (unos 2 minutos)

1. **Mis reservas** → recuadro *Valoraciones pendientes* → **Valorar**.
2. Tocá *Enviar valoración* sin estrellas: aparece el mensaje del flujo 5a.
3. Elegí 4 estrellas, escribí un comentario y enviá. Aparece *¡Gracias por tu valoración!* y el recuadro desaparece de *Mis reservas*.
4. Abrí `http://localhost:5173/valorar` y entrá como participante sin cuenta con la reserva de Sofía y el DNI de Mateo, `45111222`. También puede valorar.
5. **Administrador** → *Valoraciones* → *Sendero Laguna del Bosque*: el promedio y los comentarios ya incluyen los nuevos.

## Endpoints nuevos

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/cliente/valoraciones-pendientes` | Panel del cliente |
| GET | `/api/valoraciones/formulario?reserva=&dni=` | Formulario (cliente o participante con el enlace) |
| POST | `/api/valoraciones` | `{ idReserva, dni, calificacion, comentario }` |
| GET | `/api/admin/valoraciones` | Excursiones con promedio y total |
| GET | `/api/admin/valoraciones/excursiones/{id}` | Detalle: promedio, cantidad por estrellas y comentarios |
