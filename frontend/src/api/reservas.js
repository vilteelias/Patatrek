import { pedir } from './cliente.js';

// API del rol Cliente: CU-18 Realizar reserva y CU-26 Realizar pago.
// Mientras no esté el módulo de Autenticación, el backend usa la clienta de demostración (Sofía Ibarra).

export function obtenerClienteActual() {
  return pedir('/api/cliente/yo');
}

/** Arma "?clave=valor&..." solo con los filtros que tienen valor. */
function consulta(filtros = {}) {
  const params = new URLSearchParams();
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && String(valor).trim() !== '') params.append(clave, String(valor).trim());
  });
  const texto = params.toString();
  return texto ? `?${texto}` : '';
}

/** CU-21. filtros: { desde, hasta, estado } (todos opcionales) */
export function listarMisReservas(filtros) {
  return pedir(`/api/cliente/reservas${consulta(filtros)}`);
}

/** CU-23. El motivo es opcional. */
export function cancelarReserva(id, motivo) {
  return pedir(`/api/reservas/${id}/cancelacion`, { metodo: 'POST', cuerpo: { motivo: motivo || null } });
}

/** CU-20 (Administrador). filtros: { excursion, idSalida, desde, hasta, dni, estado } */
export function listarReservasAdmin(filtros) {
  return pedir(`/api/admin/reservas${consulta(filtros)}`);
}

export function obtenerReservaAdmin(id) {
  return pedir(`/api/admin/reservas/${id}`);
}

export function obtenerExcursion(id) {
  return pedir(`/api/excursiones/${id}`);
}

export function listarSalidasDeExcursion(idExcursion) {
  return pedir(`/api/excursiones/${idExcursion}/salidas`);
}

export function obtenerDatosParaReservar(idSalida) {
  return pedir(`/api/reservas/salidas/${idSalida}`);
}

/** datos: { idSalida, participantes: [...], modalidadAlojamiento, equipamiento: [{ idEquipamiento, cantidad }] } */
export function crearReserva(datos) {
  return pedir('/api/reservas', { metodo: 'POST', cuerpo: datos });
}

export function obtenerReserva(id) {
  return pedir(`/api/reservas/${id}`);
}

/**
 * CU-26. No se envía NINGÚN dato de tarjeta: solo el modo de pago y,
 * como la pasarela es simulada, la respuesta que queremos que devuelva.
 */
/** CU-26. La pasarela simulada decide sola si aprueba o rechaza el pago. */
export function pagarReserva(id, modoPago) {
  return pedir(`/api/reservas/${id}/pagos`, { metodo: 'POST', cuerpo: { modoPago } });
}

/** Solo para la demostración: vence la retención sin esperar los 10 minutos. */
export function simularVencimiento(id) {
  return pedir(`/api/reservas/${id}/simular-vencimiento`, { metodo: 'POST' });
}

/** CU-30: ejecuta ahora la cancelación automática que el temporizador hace todos los días. */
export function ejecutarCancelacionAutomatica() {
  return pedir('/api/admin/reservas/cancelacion-automatica', { metodo: 'POST' });
}
