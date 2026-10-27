import { pedir } from './cliente.js';

// API del rol Guía (CU-10, CU-22, CU-15, CU-16).
// Mientras no esté el módulo de Autenticación, el backend usa un guía de demostración.

export function obtenerGuiaActual() {
  return pedir('/api/guia/yo');
}

export function listarMisSalidas() {
  return pedir('/api/guia/salidas');
}

export function obtenerSalidaGuia(idSalida) {
  return pedir(`/api/guia/salidas/${idSalida}`);
}

export function listarParticipantesSalida(idSalida) {
  return pedir(`/api/guia/salidas/${idSalida}/participantes`);
}

/** registros: [{ idParticipante, estadoAsistencia, equipamientoPresentado: [ids] }] */
export function registrarControlAsistencia(idSalida, registros) {
  return pedir(`/api/guia/salidas/${idSalida}/control-asistencia`, { metodo: 'POST', cuerpo: { registros } });
}

/** Devuelve { cantidadNotificados, salida } */
export function marcarFinalizacionSalida(idSalida, observaciones) {
  return pedir(`/api/guia/salidas/${idSalida}/finalizacion`, { metodo: 'POST', cuerpo: { observaciones } });
}
