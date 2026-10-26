// Cliente HTTP común para hablar con la API REST de Spring Boot.
// Las rutas son relativas (/api/...) y Vite las redirige al backend (ver vite.config.js).

/**
 * Error de la API. El backend responde siempre con:
 * { mensaje: "texto general", errores: { campo: "mensaje del campo" } }
 */
export class ApiError extends Error {
  constructor(mensaje, estado, errores = {}) {
    super(mensaje);
    this.estado = estado;
    this.errores = errores;
  }
}

export async function pedir(ruta, { metodo = 'GET', cuerpo, archivo } = {}) {
  const opciones = { method: metodo, headers: {} };

  if (archivo) {
    // Subida de archivos: multipart/form-data (el navegador arma la cabecera)
    const datos = new FormData();
    datos.append('archivo', archivo);
    opciones.body = datos;
  } else if (cuerpo !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(ruta, opciones);
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Verificá que el backend esté levantado.', 0);
  }

  const texto = await respuesta.text();
  const json = texto ? intentarJson(texto) : null;

  if (!respuesta.ok) {
    throw new ApiError(
      json?.mensaje ?? `Error inesperado del servidor (${respuesta.status}).`,
      respuesta.status,
      json?.errores ?? {},
    );
  }
  return json;
}

function intentarJson(texto) {
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}
