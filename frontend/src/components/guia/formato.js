const MESES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];
function partesFecha(iso) {
  const [anio, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  return { anio, mes: MESES[mes - 1], dia };
}
export function formatearRangoFechas(inicio, fin) {
  const a = partesFecha(inicio);
  if (!fin || inicio.slice(0, 10) === fin.slice(0, 10)) {
    return `${a.dia} de ${a.mes}, ${a.anio}`;
  }
  const b = partesFecha(fin);
  if (a.anio !== b.anio) {
    return `${a.dia} de ${a.mes}, ${a.anio} al ${b.dia} de ${b.mes}, ${b.anio}`;
  }
  if (a.mes !== b.mes) {
    return `${a.dia} de ${a.mes} al ${b.dia} de ${b.mes}, ${b.anio}`;
  }
  return `${a.dia} al ${b.dia} de ${b.mes}, ${b.anio}`;
}
export function formatearHorario(inicio, fin) {
  return `${inicio} a ${fin} h`;
}
export function formatearDni(dni) {
  const digitos = dni.replace(/\D/g, '');
  return digitos.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
export const formatoPrecio = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});
