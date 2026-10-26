import { useMemo, useState } from 'react';
import Modal from './Modal.jsx';
import Icono from './Icono.jsx';

/**
 * CU-05 Agregar equipamiento necesario de excursión.
 * Muestra el catálogo precargado excluyendo los ítems ya asignados.
 * Cancelar descarta la selección sin modificar el listado (flujo alternativo 4a).
 */
function ModalEquipamiento({ catalogo, idsAsignados, onConfirmar, onCerrar }) {
  const [busqueda, setBusqueda] = useState('');
  const [elegidos, setElegidos] = useState(new Set());

  const disponibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return catalogo
      .filter((item) => !idsAsignados.includes(item.idEquipamiento))
      .filter((item) => item.nombre.toLowerCase().includes(texto));
  }, [catalogo, idsAsignados, busqueda]);

  function alternar(id) {
    setElegidos((previos) => {
      const nuevos = new Set(previos);
      nuevos.has(id) ? nuevos.delete(id) : nuevos.add(id);
      return nuevos;
    });
  }

  return (
    <Modal
      titulo="Agregar equipamiento"
      descripcion="Seleccioná del catálogo precargado los ítems obligatorios para esta excursión."
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCerrar}>
            Cancelar
          </button>
          <button
            type="button"
            className="boton boton--primario"
            disabled={elegidos.size === 0}
            onClick={() => onConfirmar([...elegidos])}
          >
            Agregar seleccionados ({elegidos.size})
          </button>
        </>
      }
    >
      <div className="buscador">
        <Icono nombre="buscar" />
        <input
          type="search"
          placeholder="Buscar ítem…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          aria-label="Buscar ítem de equipamiento"
          autoFocus
        />
      </div>

      {disponibles.length === 0 ? (
        <p className="modal__vacio">
          {busqueda ? 'Ningún ítem coincide con la búsqueda.' : 'Todos los ítems del catálogo ya están asignados.'}
        </p>
      ) : (
        <ul className="lista-chequeo">
          {disponibles.map((item) => (
            <li key={item.idEquipamiento}>
              <label>
                <input
                  type="checkbox"
                  checked={elegidos.has(item.idEquipamiento)}
                  onChange={() => alternar(item.idEquipamiento)}
                />
                <span>
                  {item.nombre}
                  {item.descripcion && <small>{item.descripcion}</small>}
                </span>
              </label>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}

export default ModalEquipamiento;
