import { pedir } from './cliente.js';

// CU-19 del lado del Participante (sin cuenta): acceso con N.º de reserva + DNI.

export function obtenerReservaParticipante(idReserva, dni) {
  return pedir(`/api/participante/reservas/${idReserva}?dni=${encodeURIComponent(dni)}`);
}

/** CU-26 4b: el participante paga el saldo restante. */
export function pagarSaldoComoParticipante(idReserva, dni) {
  return pedir(`/api/participante/reservas/${idReserva}/pagos`, { metodo: 'POST', cuerpo: { dni } });
}
