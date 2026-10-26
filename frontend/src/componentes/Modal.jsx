import { useEffect } from 'react';

// Ventana modal simple. Se cierra con Escape o haciendo clic afuera.
function Modal({ titulo, descripcion, onCerrar, children, pie }) {
  useEffect(() => {
    const alPresionar = (e) => e.key === 'Escape' && onCerrar();
    document.addEventListener('keydown', alPresionar);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', alPresionar);
      document.body.style.overflow = '';
    };
  }, [onCerrar]);

  return (
    <div className="modal__fondo" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
        <header className="modal__encabezado">
          <h2 id="modal-titulo">{titulo}</h2>
          {descripcion && <p>{descripcion}</p>}
        </header>
        <div className="modal__cuerpo">{children}</div>
        {pie && <footer className="modal__pie">{pie}</footer>}
      </div>
    </div>
  );
}

export default Modal;
