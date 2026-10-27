import { pedir } from './cliente.js';

// API del rol Cliente: CU-18 Realizar reserva y CU-26 Realizar pago.
// Mientras no esté el módulo de Autenticación, el backend usa la clienta de demostración (Sofía Ibarra).

export function obtenerClienteActual() {
  return pedir('/api/cliente/yo');
}

export function listarMisReservas() {
  return pedir('/api/cliente/reservas');
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
export function pagarReserva(id, modoPago, aprobar) {
  return pedir(`/api/reservas/${id}/pagos`, { metodo: 'POST', cuerpo: { modoPago, aprobar } });
}

/** Solo para la demostración: vence la retención sin esperar los 10 minutos. */
export function simularVencimiento(id) {
  return pedir(`/api/reservas/${id}/simular-vencimiento`, { metodo: 'POST' });
}
