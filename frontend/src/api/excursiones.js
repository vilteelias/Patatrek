import { pedir } from './cliente.js';

// ---------- Catálogo público (Visitante / Cliente) ----------

export function listarExcursiones() {
  return pedir('/api/excursiones');
}

// ---------- Administrador ----------

/** estado: undefined (todas), 'ACTIVA' o 'INACTIVA' */
export function listarExcursionesAdmin(estado) {
  const filtro = estado ? `?estado=${estado}` : '';
  return pedir(`/api/admin/excursiones${filtro}`);
}

export function obtenerExcursionAdmin(id) {
  return pedir(`/api/admin/excursiones/${id}`);
}

export function registrarExcursion(datos) {
  return pedir('/api/admin/excursiones', { metodo: 'POST', cuerpo: datos });
}

export function modificarExcursion(id, datos) {
  return pedir(`/api/admin/excursiones/${id}`, { metodo: 'PUT', cuerpo: datos });
}

export function darDeBajaExcursion(id, motivo) {
  return pedir(`/api/admin/excursiones/${id}/baja`, { metodo: 'PATCH', cuerpo: { motivo } });
}

export function subirImagenExcursion(id, archivo) {
  return pedir(`/api/admin/excursiones/${id}/imagen`, { metodo: 'POST', archivo });
}

export function listarEquipamiento() {
  return pedir('/api/admin/equipamiento');
}
