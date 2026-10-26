function Interruptor({ id, activo, onChange, deshabilitado, etiqueta }) {
  return (
    <button
      id={id}
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      className={`interruptor ${activo ? 'interruptor--activo' : ''}`}
      onClick={() => onChange(!activo)}
      disabled={deshabilitado}
    >
      <span className="interruptor__bola" />
    </button>
  );
}

export default Interruptor;
