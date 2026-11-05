import { pedir } from './cliente.js';

// Gestión de salidas (Administrador): CU-09 a CU-14.

function consulta(params = {}) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && String(v).trim() !== '') q.append(k, String(v).trim());
  });
  const texto = q.toString();
  return texto ? `?${texto}` : '';
}

/** CU-10. filtros: { excursion, estado } */
export function listarSalidasAdmin(filtros) {
  return pedir(`/api/admin/salidas${consulta(filtros)}`);
}

export function obtenerSalidaAdmin(id) {
  return pedir(`/api/admin/salidas/${id}`);
}

/** Excursiones, guías (con disponibilidad) y refugios (con plazas libres) para un período. */
export function opcionesSalida({ fechaInicio, horaInicio, fechaFinalizacion, horaFinalizacion, excluirSalida } = {}) {
  return pedir(
    `/api/admin/salidas/opciones${consulta({ fechaInicio, horaInicio, fechaFinalizacion, horaFinalizacion, excluirSalida })}`,
  );
}

/** CU-09 */
export function planificarSalida(datos) {
  return pedir('/api/admin/salidas', { metodo: 'POST', cuerpo: datos });
}

/** CU-11: { idGuiaResponsable, plazasPorRefugio, refugiosPorNoche } */
export function modificarSalida(id, datos) {
  return pedir(`/api/admin/salidas/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** CU-12: la lista completa de ayudantes que tiene que quedar. */
export function asignarAyudantes(id, idsGuias) {
  return pedir(`/api/admin/salidas/${id}/ayudantes`, { metodo: 'PUT', cuerpo: { idsGuias } });
}

/** CU-13 */
export function darDeBajaSalida(id, motivo) {
  return pedir(`/api/admin/salidas/${id}/baja`, { metodo: 'POST', cuerpo: { motivo } });
}

/** CU-14: { tipoContingencia, resolucion, motivo, nuevaFechaInicio, nuevaHoraInicio, nuevaHoraFinalizacion, nuevoRecorrido } */
export function registrarContingencia(id, datos) {
  return pedir(`/api/admin/salidas/${id}/contingencias`, { metodo: 'POST', cuerpo: datos });
}
