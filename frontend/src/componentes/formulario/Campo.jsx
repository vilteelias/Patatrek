import Icono from '../Icono.jsx';

/**
 * Envoltorio de un campo de formulario.
 * - obligatorio: muestra el asterisco (*). Es la convención de todo el sistema:
 *   cada formulario lleva arriba la leyenda "Los campos marcados con * son obligatorios".
 * - bloqueado: el campo no se puede editar (muestra un candado).
 * - error / ayuda: mensaje debajo del campo.
 */
function Campo({ id, etiqueta, obligatorio, bloqueado, error, ayuda, children }) {
  return (
    <div className={`campo ${error ? 'campo--error' : ''}`}>
      <label htmlFor={id} className="campo__etiqueta">
        {etiqueta}
        {obligatorio && (
          <span className="campo__obligatorio" aria-hidden="true">*</span>
        )}
        {obligatorio && <span className="solo-lector"> (obligatorio)</span>}
        {bloqueado && (
          <span className="campo__bloqueado" title="Bloqueado: la excursión tiene salidas activas">
            <Icono nombre="candado" tamaño={14} />
          </span>
        )}
      </label>
      {children}
      {error ? (
        <p className="campo__mensaje campo__mensaje--error" id={`${id}-error`} role="alert">
          {error}
        </p>
      ) : (
        ayuda && <p className="campo__mensaje">{ayuda}</p>
      )}
    </div>
  );
}

export function LeyendaObligatorios() {
  return (
    <p className="leyenda-obligatorios">
      Los campos marcados con <span className="campo__obligatorio">*</span> son obligatorios.
    </p>
  );
}

export default Campo;
