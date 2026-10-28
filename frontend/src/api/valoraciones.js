import { pedir } from './cliente.js';

// CU-28 Registrar valoración de excursión y CU-29 Consultar valoraciones.

export function listarValoracionesPendientes() {
  return pedir('/api/cliente/valoraciones-pendientes');
}

/** El participante se identifica con el N.º de reserva y su DNI (enlace del correo). */
export function obtenerFormularioValoracion(idReserva, dni) {
  const q = new URLSearchParams({ reserva: String(idReserva), dni: String(dni) });
  return pedir(`/api/valoraciones/formulario?${q}`);
}

export function registrarValoracion({ idReserva, dni, calificacion, comentario }) {
  return pedir('/api/valoraciones', {
    metodo: 'POST',
    cuerpo: { idReserva: Number(idReserva), dni, calificacion, comentario: comentario || null },
  });
}

export function resumenValoraciones() {
  return pedir('/api/admin/valoraciones');
}

export function detalleValoraciones(idExcursion) {
  return pedir(`/api/admin/valoraciones/excursiones/${idExcursion}`);
}
