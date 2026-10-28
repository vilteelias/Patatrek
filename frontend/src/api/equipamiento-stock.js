import { pedir } from './cliente.js';

// Consulta de disponibilidad del equipamiento (Administrador, solo lectura).

/** Sin idSalida: la disponibilidad de hoy. Con idSalida: la de las fechas de esa salida. */
export function consultarStockEquipamiento(idSalida) {
  return pedir(`/api/admin/equipamiento/stock${idSalida ? `?idSalida=${idSalida}` : ''}`);
}

export function listarSalidasParaEquipamiento() {
  return pedir('/api/admin/equipamiento/salidas');
}
