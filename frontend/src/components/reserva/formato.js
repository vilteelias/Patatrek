// Formatos de la parte de reservas (reutiliza los del guía).
export { formatearRangoFechas } from '@/components/guia/formato';

const pesosAR = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });

export function pesos(valor) {
  return pesosAR.format(Number(valor ?? 0));
}

/** "08:00:00" → "08:00" */
export function hora(valor) {
  return valor ? String(valor).slice(0, 5) : '';
}

/** "2026-10-26T14:05:00" → "26/10/2026 14:05" */
export function fechaHora(valor) {
  if (!valor) return '';
  const [f, h = ''] = String(valor).split('T');
  const [a, m, d] = f.split('-');
  return `${d}/${m}/${a} ${h.slice(0, 5)}`.trim();
}

export const MODO_PAGO = { SENA: 'Seña (50%)', TOTAL: 'Pago completo', SALDO: 'Saldo restante' };
export const ESTADO_PAGO = { PENDIENTE: 'Pendiente', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado', DEVUELTO: 'Devuelto' };
export const ALOJAMIENTO = { REFUGIO: 'Refugio', CARPA_PROPIA: 'Carpa propia' };
