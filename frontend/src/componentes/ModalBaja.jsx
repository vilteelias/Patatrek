import { useState } from 'react';
import Modal from './Modal.jsx';
import Campo, { LeyendaObligatorios } from './formulario/Campo.jsx';
import { darDeBajaExcursion } from '../api/excursiones.js';

/**
 * CU-08 Baja de excursión: baja lógica con motivo obligatorio.
 * Las salidas ya creadas y sus reservas no se modifican.
 */
function ModalBaja({ excursion, onCerrar, onBajaRealizada }) {
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function confirmar() {
    if (!motivo.trim()) {
      setError('Ingresá el motivo de la baja');
      return;
    }
    setEnviando(true);
    try {
      const actualizada = await darDeBajaExcursion(excursion.idExcursion, motivo.trim());
      onBajaRealizada(actualizada);
    } catch (e) {
      setError(e.errores?.motivo ?? e.message);
      setEnviando(false);
    }
  }

  return (
    <Modal
      titulo="Dar de baja excursión"
      descripcion={`"${excursion.nombre}" pasará a estado Inactiva y no se podrán planificar nuevas salidas. Las salidas y reservas existentes se mantienen.`}
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCerrar} disabled={enviando}>
            Cancelar
          </button>
          <button type="button" className="boton boton--peligro" onClick={confirmar} disabled={enviando}>
            {enviando ? 'Dando de baja…' : 'Confirmar baja'}
          </button>
        </>
      }
    >
      <LeyendaObligatorios />
      <Campo id="motivo-baja" etiqueta="Motivo de la baja" obligatorio error={error}>
        <textarea
          id="motivo-baja"
          className="control"
          rows={3}
          maxLength={500}
          value={motivo}
          onChange={(e) => {
            setMotivo(e.target.value);
            setError('');
          }}
          placeholder="Ej. Cierre del acceso al sendero por parte del parque."
          aria-invalid={!!error}
          aria-required="true"
          autoFocus
        />
      </Campo>
    </Modal>
  );
}

export default ModalBaja;
