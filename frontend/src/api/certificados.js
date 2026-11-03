import { pedir } from './cliente.js';

// API de certificados médicos (CU-24 cargar, CU-25 validar).

export const MAX_INTENTOS = 2;
export const TAMANIO_MAXIMO = 5 * 1024 * 1024; // 5 MB
export const TIPOS_PERMITIDOS = ['application/pdf', 'image/jpeg', 'image/png'];

// ---------- Cliente / Participante (CU-24) ----------

/**
 * Certificados de los participantes de una reserva.
 * Cliente: todos los participantes. Participante: pasar su dni y devuelve solo el suyo.
 * Respuesta:
 * {
 *   horarioLimite: "2026-11-11T08:00:00",   // 72 hs antes de la salida
 *   certificados: [{
 *     idCertificadoMedico, nombre, apellido, esTitular,
 *     estadoCertificado,  // PENDIENTE | CARGADO | APROBADO | RECHAZADO | RECHAZADO_DEFINITIVO
 *     fechaSubida, motivoRechazo, intentosEnvio
 *   }]
 * }
 */
export function obtenerCertificadosReserva(idReserva, dni) {
  const filtro = dni ? `?dni=${encodeURIComponent(dni)}` : '';
  return pedir(`/api/reservas/${idReserva}/certificados${filtro}`);
}

/** Sube el archivo (multipart, campo "archivo"). El dni es opcional: lo usa el backend para validar al participante. */
export function subirCertificado(idCertificado, archivo, dni) {
  const filtro = dni ? `?dni=${encodeURIComponent(dni)}` : '';
  return pedir(`/api/certificados/${idCertificado}/archivo${filtro}`, { metodo: 'POST', archivo });
}

// ---------- Administrador (CU-25) ----------

/**
 * Certificados en estado CARGADO, de reservas Señadas/Confirmadas y salidas Disponibles/Completas,
 * ordenados por la salida más próxima. Cada elemento:
 * {
 *   idCertificadoMedico, nombre, apellido, dni, idReserva, nombreExcursion,
 *   fechaInicioSalida, fechaFinalizacionSalida,   // "2026-11-14"
 *   fechaSubida, nombreArchivo, intentosEnvio     // 1 = primer envío, 2 = segundo envío
 * }
 */
export function listarCertificadosAValidar() {
  return pedir('/api/admin/certificados/pendientes');
}

/** URL para abrir el archivo adjunto (se abre en una pestaña nueva). */
export function urlArchivoCertificado(idCertificado) {
  return `/api/admin/certificados/${idCertificado}/archivo`;
}

export function aprobarCertificado(idCertificado) {
  return pedir(`/api/admin/certificados/${idCertificado}/aprobar`, { metodo: 'PUT' });
}

/** Devuelve el certificado actualizado (estadoCertificado: RECHAZADO o RECHAZADO_DEFINITIVO). */
export function rechazarCertificado(idCertificado, motivo) {
  return pedir(`/api/admin/certificados/${idCertificado}/rechazar`, { metodo: 'PUT', cuerpo: { motivo } });
}
