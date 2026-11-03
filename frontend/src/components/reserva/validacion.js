// Validación de los datos de los participantes (los mismos mensajes que devuelve el backend).

export const PARTICIPANTE_VACIO = {
  nombre: '',
  apellido: '',
  dni: '',
  fechaNacimiento: '',
  correoElectronico: '',
  nroTelefono: '',
};

const DNI = /^\d{7,8}$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TELEFONO = /^[0-9+()\-\s]{6,20}$/;

function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Edad (en años cumplidos) que tendrá la persona en la fecha dada (ambas "AAAA-MM-DD"). */
export function edadEn(nacimiento, fecha) {
  const [an, mn, dn] = nacimiento.split('-').map(Number);
  const [af, mf, df] = fecha.slice(0, 10).split('-').map(Number);
  let edad = af - an;
  if (mf < mn || (mf === mn && df < dn)) edad -= 1;
  return edad;
}

/**
 * Devuelve { "participantes[i].campo": "mensaje" } con todos los problemas encontrados.
 * salida: { aceptaMenores, fechaInicio }
 */
export function validarParticipantes(participantes, salida) {
  const errores = {};
  const dnis = new Set();
  participantes.forEach((p, i) => {
    const pre = `participantes[${i}].`;
    const dni = p.dni.trim();
    if (!dni) errores[`${pre}dni`] = 'El DNI es obligatorio.';
    else if (!DNI.test(dni)) errores[`${pre}dni`] = 'El DNI debe tener 7 u 8 números, sin puntos.';
    else if (dnis.has(dni)) errores[`${pre}dni`] = 'Este DNI ya está cargado en otro participante de la reserva.';
    else dnis.add(dni);

    if (!p.nombre.trim()) errores[`${pre}nombre`] = 'El nombre es obligatorio.';
    if (!p.apellido.trim()) errores[`${pre}apellido`] = 'El apellido es obligatorio.';

    const correo = p.correoElectronico.trim();
    if (!correo) errores[`${pre}correoElectronico`] = 'El correo electrónico es obligatorio.';
    else if (!CORREO.test(correo))
      errores[`${pre}correoElectronico`] = 'Ingresá un correo válido, por ejemplo nombre@mail.com.';

    const tel = p.nroTelefono.trim();
    if (!tel) errores[`${pre}nroTelefono`] = 'El teléfono es obligatorio.';
    else if (!TELEFONO.test(tel)) errores[`${pre}nroTelefono`] = 'Ingresá solo números, espacios o guiones.';

    if (!p.fechaNacimiento) errores[`${pre}fechaNacimiento`] = 'La fecha de nacimiento es obligatoria.';
    else if (p.fechaNacimiento >= hoyIso())
      errores[`${pre}fechaNacimiento`] = 'La fecha de nacimiento debe ser anterior a hoy.';
    else if (!salida.aceptaMenores && edadEn(p.fechaNacimiento, salida.fechaInicio) < 18)
      errores[`${pre}fechaNacimiento`] =
        'Esta excursión no admite menores de edad: el participante tendría menos de 18 años el día de la salida.';
  });
  return errores;
}
