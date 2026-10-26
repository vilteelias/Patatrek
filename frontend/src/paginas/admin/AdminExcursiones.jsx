import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { listarExcursionesAdmin } from '../../api/excursiones.js';
import { DIFICULTAD, dias, pesos, siNo } from '../../utilidades/formato.js';
import Icono from '../../componentes/Icono.jsx';
import ModalBaja from '../../componentes/ModalBaja.jsx';

const FILTROS = [
  { clave: 'TODAS', texto: 'Todas' },
  { clave: 'ACTIVA', texto: 'Activas' },
  { clave: 'INACTIVA', texto: 'Inactivas' },
];

// CU-06 Listar excursiones (Administrador): catálogo completo, activas e inactivas.
function AdminExcursiones() {
  const [excursiones, setExcursiones] = useState([]);
  const [filtro, setFiltro] = useState('TODAS');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aBajar, setABajar] = useState(null);

  // Mensaje de éxito que llega desde el formulario (registrar / modificar)
  const location = useLocation();
  const navigate = useNavigate();
  const [aviso, setAviso] = useState(location.state?.aviso ?? '');

  useEffect(() => {
    if (location.state?.aviso) {
      navigate(location.pathname, { replace: true, state: null }); // que no reaparezca al recargar
    }
    listarExcursionesAdmin()
      .then(setExcursiones)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!aviso) return;
    const t = setTimeout(() => setAviso(''), 5000);
    return () => clearTimeout(t);
  }, [aviso]);

  const cantidad = (clave) =>
    clave === 'TODAS' ? excursiones.length : excursiones.filter((e) => e.estadoExcursion === clave).length;

  const visibles = filtro === 'TODAS' ? excursiones : excursiones.filter((e) => e.estadoExcursion === filtro);

  function bajaRealizada(actualizada) {
    setExcursiones((lista) => lista.map((e) => (e.idExcursion === actualizada.idExcursion ? actualizada : e)));
    setABajar(null);
    setAviso(`"${actualizada.nombre}" se dio de baja correctamente.`);
  }

  return (
    <section className="admin-pagina">
      <div className="admin-pagina__cabecera">
        <div>
          <p className="miga">Catálogo comercial</p>
          <h1 className="titulo">Excursiones</h1>
        </div>
        <Link to="nueva" className="boton boton--primario">
          <Icono nombre="mas" tamaño={16} /> Nueva excursión
        </Link>
      </div>

      {aviso && (
        <div className="alerta alerta--exito" role="status">
          <Icono nombre="check" /> {aviso}
        </div>
      )}
      {error && <div className="alerta alerta--error">{error}</div>}

      <div className="pestanias" role="tablist">
        {FILTROS.map((f) => (
          <button
            key={f.clave}
            type="button"
            role="tab"
            aria-selected={filtro === f.clave}
            className={`pestania ${filtro === f.clave ? 'pestania--activa' : ''}`}
            onClick={() => setFiltro(f.clave)}
          >
            {f.texto} ({cantidad(f.clave)})
          </button>
        ))}
      </div>

      <div className="tabla-contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Duración</th>
              <th>Dificultad</th>
              <th>Cert. médico</th>
              <th>Admite menores</th>
              <th>Precio base</th>
              <th>Estado</th>
              <th><span className="solo-lector">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={8} className="tabla__vacio">Cargando…</td>
              </tr>
            )}
            {!cargando && visibles.length === 0 && (
              <tr>
                <td colSpan={8} className="tabla__vacio">No hay excursiones para mostrar.</td>
              </tr>
            )}
            {visibles.map((e) => (
              <tr key={e.idExcursion} className={e.estadoExcursion === 'INACTIVA' ? 'tabla__fila--inactiva' : ''}>
                <td>
                  <strong>{e.nombre}</strong>
                  {e.motivoBaja && <small className="tabla__nota">Baja: {e.motivoBaja}</small>}
                </td>
                <td>{dias(e.duracion)}</td>
                <td>{DIFICULTAD[e.dificultad]}</td>
                <td>{siNo(e.requiereCertificado)}</td>
                <td>{siNo(e.aceptaMenores)}</td>
                <td className="tabla__numero">{pesos(e.precio)}</td>
                <td>
                  <span className={`estado estado--${e.estadoExcursion.toLowerCase()}`}>
                    {e.estadoExcursion === 'ACTIVA' ? 'Activa' : 'Inactiva'}
                  </span>
                </td>
                <td className="tabla__acciones">
                  <Link to={`${e.idExcursion}/editar`} className="boton-icono" title="Modificar">
                    <Icono nombre="lapiz" tamaño={16} />
                    <span className="solo-lector">Modificar {e.nombre}</span>
                  </Link>
                  {e.estadoExcursion === 'ACTIVA' && (
                    <button type="button" className="boton-icono boton-icono--peligro" title="Dar de baja" onClick={() => setABajar(e)}>
                      <Icono nombre="archivar" tamaño={16} />
                      <span className="solo-lector">Dar de baja {e.nombre}</span>
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {aBajar && <ModalBaja excursion={aBajar} onCerrar={() => setABajar(null)} onBajaRealizada={bajaRealizada} />}
    </section>
  );
}

export default AdminExcursiones;
