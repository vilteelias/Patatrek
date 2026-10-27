export const DIFICULTAD = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta' };

const formatoPesos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

export function pesos(valor) {
  return formatoPesos.format(Number(valor ?? 0));
}

export function dias(cantidad) {
  return `${cantidad} ${cantidad === 1 ? 'día' : 'días'}`;
}

export function siNo(valor) {
  return valor ? 'Sí' : 'No';
}
