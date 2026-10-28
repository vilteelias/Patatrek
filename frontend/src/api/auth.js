import { pedir } from './cliente.js';

// CU-01 Registrar cliente, CU-02 Iniciar sesión, CU-03 Cerrar sesión.
// La sesión vive en el servidor (cookie). El frontend nunca guarda la contraseña.

/** { usuario: { id, rol, nombre, apellido, correoElectronico, dni } | null, exigeLogin } */
export function obtenerSesion() {
  return pedir('/api/auth/sesion');
}

export function iniciarSesion(correoElectronico, contrasenia) {
  return pedir('/api/auth/login', { metodo: 'POST', cuerpo: { correoElectronico, contrasenia } });
}

export function registrarCliente(datos) {
  return pedir('/api/auth/registro', { metodo: 'POST', cuerpo: datos });
}

export function cerrarSesion() {
  return pedir('/api/auth/logout', { metodo: 'POST' });
}
