import { pedir } from './cliente.js';

// CU-27 Registrar devolución de reserva (Administrador).

/** { pendientes: [...], montoTotalPendiente, realizadas: [...] } */
export function listarDevoluciones() {
  return pedir('/api/admin/devoluciones');
}

/** Ejecuta el reembolso total de lo abonado a través de la pasarela simulada. */
export function ejecutarDevolucion(idReserva) {
  return pedir(`/api/admin/devoluciones/${idReserva}`, { metodo: 'POST' });
}
