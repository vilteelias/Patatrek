import { useEffect, useState } from 'react';
import { listarExcursiones } from '../api/excursiones.js';
import TarjetaExcursion from '../componentes/TarjetaExcursion.jsx';

// CU-06 Listar excursiones (Visitante y Cliente): solo las activas.
function ListaExcursiones() {
  const [excursiones, setExcursiones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    listarExcursiones()
      .then(setExcursiones)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, []);

  return (
    <section>
      <h1 className="titulo">Excursiones</h1>
      <p className="subtitulo">Recorridos guiados por la Patagonia, para todos los niveles de experiencia.</p>

      {cargando && <p className="cargando">Cargando excursiones…</p>}
      {error && <div className="alerta alerta--error">{error}</div>}
      {!cargando && !error && excursiones.length === 0 && (
        <p className="estado-vacio">Todavía no hay excursiones disponibles.</p>
      )}

      <div className="grilla-tarjetas">
        {excursiones.map((excursion) => (
          <TarjetaExcursion key={excursion.idExcursion} excursion={excursion} />
        ))}
      </div>
    </section>
  );
}

export default ListaExcursiones;
