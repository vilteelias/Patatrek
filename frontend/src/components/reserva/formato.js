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

/** Estados de reserva para los filtros (CU-20 y CU-21), en el orden del documento. */
export const ESTADOS_RESERVA = [
  { valor: 'SENADA', etiqueta: 'Señada' },
  { valor: 'CONFIRMADA', etiqueta: 'Confirmada' },
  { valor: 'CANCELADA', etiqueta: 'Cancelada' },
  { valor: 'PENDIENTE_PAGO', etiqueta: 'Pendiente de pago' },
  { valor: 'PENDIENTE_DEVOLUCION', etiqueta: 'Pendiente de devolución' },
  { valor: 'DEVUELTA', etiqueta: 'Devuelta' },
];

/** Clases de un <select> nativo con el mismo aspecto que los Input. */
export const CLASE_SELECT =
  'h-10 w-full rounded-lg border border-input bg-card px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';
