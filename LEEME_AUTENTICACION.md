# Autenticación: registrarse (CU-01), iniciar sesión (CU-02) y cerrar sesión (CU-03)

## Qué agrega

- **Iniciar sesión** (`/ingresar`, como el prototipo pág. 3):
  - Se ingresa con correo y contraseña.
  - Si los datos están mal, aparece *"El correo electrónico o la contraseña son incorrectos"* (RF-02).
  - Cada rol va a su pantalla: el **Cliente** al catálogo (o a donde estaba, por ejemplo *Reservar*), el **Guía** a *Mis salidas* y el **Administrador** al panel.
- **Registrarme** (`/registrarme`):
  - Pide nombre, apellido, DNI, fecha de nacimiento, correo, teléfono y contraseña (dos veces). **Todos con asterisco de obligatorio**, igual que en los participantes.
  - Valida que el **DNI y el correo no estén registrados** (RF-01) y que la contraseña tenga al menos 8 caracteres con letras y números.
  - Al terminar, el cliente queda con la sesión iniciada.
- **Cerrar sesión:** está en el encabezado del cliente, en el menú del administrador y en el encabezado del guía. Vuelve al catálogo.
- **Pantallas según el rol:**
  - El **Visitante** ve el catálogo y las salidas. Si toca *Reservar*, se le pide iniciar sesión y después vuelve a la reserva.
  - *Mis reservas*, *Reservar* y *Pagar* son solo para el **Cliente**.
  - `/admin/...` es solo para el **Administrador**.
  - `/guia/...` es solo para el **Guía**.
  - `/valorar` sigue abierto, porque lo usa el participante sin cuenta con el enlace del correo.
- **Cada uno ve lo suyo:** el cliente que inicia sesión ve **sus** reservas, y el guía ve **sus** salidas (antes siempre eran Sofía y Martín).
- **Seguridad:**
  - Las contraseñas se guardan con **BCrypt**, nunca en texto plano.
  - La sesión vive en el servidor, con una cookie. El frontend no guarda contraseñas ni tokens.

## Cuentas de demostración

Todas tienen la contraseña **`Patatrek2026`**. En la pantalla de ingreso hay un desplegable *"Cuentas de demostración"* que completa los datos con un clic.

| Rol | Correo |
|---|---|
| Administrador | `admin@patatrek.com` (se crea solo) |
| Guía | `martin.almeida@patatrek.com`, `brenda.coronel@patatrek.com`, `lucas.ibarra@patatrek.com` |
| Cliente | `sofia.ibarra@mail.com` (y los demás clientes de ejemplo) |

Al arrancar, el backend les pone esa contraseña a las cuentas de ejemplo que todavía no tenían (las que decían `SIN_DEFINIR`). **No hace falta resetear la base.**

## Importante: las rutas del backend todavía NO se bloquean

Por ahora `patatrek.seguridad.exigir-login=false`, que es el valor por defecto:
- El backend **no rechaza** pedidos sin sesión, así las pantallas de tus compañeros (Salidas, Certificados) siguen funcionando aunque no manden la sesión.
- Si alguien usa una pantalla de cliente o de guía sin iniciar sesión, el backend sigue usando a Sofía y a Martín como antes.

Cuando todo el equipo esté integrado, alcanza con agregar esta línea en `application.properties`:
```properties
patatrek.seguridad.exigir-login=true
```
Con eso, cada rol solo puede usar sus rutas: `/api/admin/**` el Administrador, `/api/guia/**` el Guía, y `/api/cliente/**` y `/api/reservas/**` el Cliente. El catálogo y `/api/valoraciones` quedan públicos. Esto ya está probado en `AuthEstrictoApiTest`.

**Para tu compañera de Salidas:** como `/admin` ahora pide sesión de administrador en el frontend, para entrar a sus pantallas tiene que iniciar sesión con `admin@patatrek.com` / `Patatrek2026`. Su código no cambia.

## Cómo agregarlo

Este zip va **después del de valoraciones**: usa sus archivos `App.jsx`, `OperacionTerrenoService.java` y `ManejadorErrores.java`.

- **Si todavía no subiste valoraciones:** en la misma rama `valoraciones`, descomprimí primero `patatrek-valoraciones.zip` y después este, reemplazando. Probá todo y subí los dos juntos en un solo pull request.
- **Si ya mergeaste valoraciones:** Pull de `main` en IntelliJ, `git checkout -b autenticacion`, descomprimí este zip, probá y subí.

```powershell
Expand-Archive -Path "$HOME\Downloads\patatrek-autenticacion.zip" -DestinationPath "E:\Patatrek" -Force
```

No hace falta tocar `application.properties` ni la base: la tabla `administrador` ya estaba en el script.

**Opcional:** si tienen la foto original del Fitz Roy del prototipo, guardenla como `frontend/public/fondo-login.jpg` y se usa de fondo en el ingreso y el registro. Si no está, se ve un degradé.

## Para mostrarlo en la demo (unos 2 minutos)

1. Como **visitante**, abrí el catálogo: arriba aparecen *Iniciar sesión* y *Registrarme*. Elegí una salida y tocá **Reservar**: te pide iniciar sesión.
2. Probá con una contraseña incorrecta y mostrá el mensaje de error.
3. Tocá **Registrate**:
   - Mostrá los asteriscos y los errores al enviar vacío.
   - Probá con el DNI `38555777`, que ya existe: aparece el error de duplicado.
   - Corregilo y creá la cuenta. Volvés directo a la reserva, con tu nombre arriba.
4. **Cerrar sesión**. Entrá como **guía** (`martin.almeida@...`): va a *Mis salidas*.
5. **Cerrar sesión**. Entrá como **administrador** (`admin@...`): va al panel, con su nombre abajo en el menú.

## Endpoints nuevos

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/auth/sesion` | `{ usuario: {id, rol, nombre, apellido, correoElectronico, dni} \| null }` |
| POST | `/api/auth/login` | `{ correoElectronico, contrasenia }` → 200, o 401 si los datos son incorrectos |
| POST | `/api/auth/registro` | CU-01 → 201 y queda con sesión. 400 si faltan datos, 409 si el DNI o el correo ya están registrados |
| POST | `/api/auth/logout` | CU-03 |
