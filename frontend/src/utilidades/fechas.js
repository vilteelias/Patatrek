const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

const dosDigitos = (n) => String(n).padStart(2, '0');

/** "11 de noviembre, 2026 a las 08:00" (recibe un LocalDateTime del backend, sin zona horaria) */
export function formatearFechaHora(iso) {
  const f = new Date(iso);
  return `${f.getDate()} de ${MESES[f.getMonth()]}, ${f.getFullYear()} a las ${dosDigitos(f.getHours())}:${dosDigitos(f.getMinutes())}`;
}

/** "09 sep 2026" */
export function formatearFechaCorta(iso) {
  const f = new Date(iso);
  return `${dosDigitos(f.getDate())} ${MESES[f.getMonth()].slice(0, 3)} ${f.getFullYear()}`;
}

/** true si el horario límite ya pasó. El backend vuelve a validarlo al recibir el archivo. */
export function plazoVencido(horarioLimite) {
  return horarioLimite ? Date.now() >= new Date(horarioLimite).getTime() : false;
}
