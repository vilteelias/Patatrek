/**
 * Grupo de botones excluyentes (Baja / Media / Alta, Sí / No) como en el prototipo.
 * opciones: [{ valor, texto }]
 */
function SelectorOpciones({ id, opciones, valor, onChange, deshabilitado, invalido }) {
  return (
    <div
      id={id}
      className={`selector ${invalido ? 'selector--invalido' : ''}`}
      role="radiogroup"
      aria-invalid={invalido || undefined}
    >
      {opciones.map((op) => {
        const elegido = valor === op.valor;
        return (
          <button
            key={String(op.valor)}
            type="button"
            role="radio"
            aria-checked={elegido}
            className={`selector__opcion ${elegido ? 'selector__opcion--elegida' : ''}`}
            onClick={() => onChange(op.valor)}
            disabled={deshabilitado}
          >
            {op.texto}
          </button>
        );
      })}
    </div>
  );
}

export const OPCIONES_SI_NO = [
  { valor: true, texto: 'Sí' },
  { valor: false, texto: 'No' },
];

export default SelectorOpciones;
