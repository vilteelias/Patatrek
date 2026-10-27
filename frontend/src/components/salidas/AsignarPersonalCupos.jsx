import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

export function AsignarPersonalCupos() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [salidaInfo, setSalidaInfo] = useState(null);
  const [guiasCatalog, setGuiasCatalog] = useState([]);
  const [ayudantesSeleccionados, setAyudantesSeleccionados] = useState([]);
  const [mensaje, setMensaje] = useState(null);

  useEffect(() => {
    fetch(`http://localhost:8080/api/salidas/${id}`)
      .then((res) => (res.ok ? res.json() : Promise.reject('No se pudo cargar la salida')))
      .then((data) => {
        setSalidaInfo(data);
        if (data.asignacionGuias) {
          const ayudantesPrevios = data.asignacionGuias
            .filter((ag) => ag.tipoGuia === 'AYUDANTE' || ag.tipoGuia === 1 || ag.tipoGuia === '1')
            .map((ag) => ({
              idGuia: ag.guia?.idUsuario || ag.guia?.idGuia || ag.guia?.id,
              nombre: ag.guia ? `${ag.guia.apellido}, ${ag.guia.nombre}` : 'Guía',
            }));
          setAyudantesSeleccionados(ayudantesPrevios);
        }
      })
      .catch((err) => setMensaje({ tipo: 'error', texto: err.message || 'Error al obtener la salida' }));

    fetch('http://localhost:8080/api/guias')
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => setGuiasCatalog(Array.isArray(data) ? data : []))
      .catch(() => setGuiasCatalog([]));
  }, [id]);

  if (mensaje && mensaje.tipo === 'error' && !salidaInfo) {
    return (
      <div className="max-w-4xl mx-auto p-6 bg-[#FAF8F5] rounded-xl border border-stone-200 mt-6 shadow-sm font-sans">
        <div className="p-3 rounded-lg mb-4 text-sm font-medium bg-red-100 text-red-900 border border-red-300">
          {mensaje.texto}
        </div>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="px-5 py-2.5 border border-stone-300 text-stone-700 text-sm font-semibold rounded-lg hover:bg-stone-50"
        >
          ← Volver
        </button>
      </div>
    );
  }

  if (!salidaInfo) {
    return (
      <div className="max-w-4xl mx-auto p-6 bg-[#FAF8F5] rounded-xl border border-stone-200 mt-6 shadow-sm font-sans text-center text-stone-600">
        Cargando datos de salida...
      </div>
    );
  }

  const asignacionResp = salidaInfo.asignacionGuias?.find(
    (ag) => ag.tipoGuia === 'RESPONSABLE' || ag.tipoGuia === 0 || ag.tipoGuia === '0'
  );
  const idGuiaResponsable = asignacionResp?.guia?.idUsuario || asignacionResp?.guia?.idGuia || asignacionResp?.guia?.id;

  const idsAyudantesSeleccionados = ayudantesSeleccionados.map((a) => a.idGuia);

  const guiasDisponiblesParaAgregar = guiasCatalog.filter((g) => {
    const gId = g.idUsuario || g.idGuia || g.id;
    return gId !== idGuiaResponsable && !idsAyudantesSeleccionados.includes(gId);
  });

  const agregarAyudante = () => {
    if (guiasDisponiblesParaAgregar.length === 0) return;
    const primerGuia = guiasDisponiblesParaAgregar[0];
    const gId = primerGuia.idUsuario || primerGuia.idGuia || primerGuia.id;
    const nombreG = `${primerGuia.apellido}, ${primerGuia.nombre}`;

    setAyudantesSeleccionados([...ayudantesSeleccionados, { idGuia: gId, nombre: nombreG }]);
  };

  const cambiarGuiaAyudante = (index, nuevoId) => {
    const guiaObj = guiasCatalog.find((g) => (g.idUsuario || g.idGuia || g.id) === Number(nuevoId));
    if (!guiaObj) return;

    const nuevos = [...ayudantesSeleccionados];
    nuevos[index] = {
      idGuia: Number(nuevoId),
      nombre: `${guiaObj.apellido}, ${guiaObj.nombre}`,
    };
    setAyudantesSeleccionados(nuevos);
  };

  const eliminarAyudante = (index) => {
    setAyudantesSeleccionados(ayudantesSeleccionados.filter((_, i) => i !== index));
  };

  const cupoBase = salidaInfo.cupoBase || 10;
  const cupoMaximo = salidaInfo.cupoMaximo || 20;
  const cuposOcupados = salidaInfo.cuposOcupados || 0;
  const cantidadAyudantes = ayudantesSeleccionados.length;
  
  const variacionCalculada = Math.floor(cupoBase * 0.25 * cantidadAyudantes);
  const cupoNuevo = cupoBase + variacionCalculada;

  const superaMaximo = cupoNuevo > cupoMaximo;
  const estadoResultante = superaMaximo ? 'Cupo excedido' : (salidaInfo.estadoSalida || 'DISPONIBLE');

  const handleGuardar = (e) => {
    e.preventDefault();
    if (superaMaximo) {
      setMensaje({ tipo: 'error', texto: 'El cupo nuevo calculado supera el cupo máximo definido para la excursión.' });
      return;
    }

    const dtoEnvio = {
      idsGuiaAyudantes: ayudantesSeleccionados.map((a) => a.idGuia),
    };

    fetch(`http://localhost:8080/api/salidas/${id}/ayudantes`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dtoEnvio),
    })
      .then((res) => {
        if (!res.ok) throw new Error('Error al actualizar los ayudantes y cupos en la base de datos');
        return res.json();
      })
      .then(() => {
        setMensaje({ tipo: 'exito', texto: '¡Personal y cupos actualizados y guardados con éxito!' });
      })
      .catch((err) => {
        setMensaje({ tipo: 'error', texto: err.message });
      });
  };

  const formatearFecha = (fecha) => {
    if (!fecha) return 'N/D';
    if (Array.isArray(fecha)) {
      return `${fecha[2].toString().padStart(2, '0')}/${fecha[1].toString().padStart(2, '0')}/${fecha[0]}`;
    }
    return fecha;
  };

  return (
    <div className="max-w-4xl mx-auto p-6 bg-[#FAF8F5] rounded-xl border border-stone-200 mt-6 shadow-sm font-sans">
      
      <div className="text-xs text-stone-500 mb-2">
        Salidas / <span className="text-stone-700 font-medium">Salida #{id}</span> / Asignar personal y cupos
      </div>

      <h2 className="text-2xl font-bold text-stone-900 mb-1">Asignar personal y cupos</h2>
      <p className="text-sm text-stone-600 mb-6">
        Modificá los guías ayudantes de esta salida programada. El sistema recalcula el cupo total y el estado operacional automáticamente.
      </p>

      {mensaje && (
        <div
          className={`p-3 rounded-lg mb-4 text-sm font-medium ${
            mensaje.tipo === 'exito'
              ? 'bg-emerald-100 text-emerald-900 border border-emerald-300'
              : 'bg-red-100 text-red-900 border border-red-300'
          }`}
        >
          {mensaje.texto}
        </div>
      )}

      <form onSubmit={handleGuardar} className="space-y-6">

        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm grid grid-cols-2 md:grid-cols-4 gap-4 items-center">
          <div>
            <span className="block text-[11px] font-semibold text-stone-400 uppercase tracking-wider">Excursión ID</span>
            <span className="text-sm font-bold text-stone-800">
              {salidaInfo.excursion?.nombre || salidaInfo.idExcursion || 'N/D'}
            </span>
          </div>
          <div>
            <span className="block text-[11px] font-semibold text-stone-400 uppercase tracking-wider">Fecha inicio</span>
            <span className="text-sm font-semibold text-stone-800">{formatearFecha(salidaInfo.fechaInicio)}</span>
          </div>
          <div>
            <span className="block text-[11px] font-semibold text-stone-400 uppercase tracking-wider">Guía responsable</span>
            <span className="text-sm font-semibold text-stone-800">
              {asignacionResp?.guia ? `${asignacionResp.guia.apellido}, ${asignacionResp.guia.nombre}` : 'No asignado'}
            </span>
          </div>
          <div>
            <span className="block text-[11px] font-semibold text-stone-400 uppercase tracking-wider mb-1">Estado actual</span>
            <span className="inline-block px-2.5 py-1 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-full">
              {salidaInfo.estadoSalida || 'DISPONIBLE'}
            </span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-bold text-stone-800">Guías ayudantes</h3>
            <p className="text-xs text-stone-500">
              Cada ayudante agregado incrementa el cupo total de la salida; cada uno removido lo reduce.
            </p>
          </div>

          <div className="space-y-3">
            {ayudantesSeleccionados.map((item, index) => {
              const guiasDisponiblesParaEsteSelect = guiasCatalog.filter((g) => {
                const gId = g.idUsuario || g.idGuia || g.id;
                if (gId === idGuiaResponsable) return false;
                const yaSeleccionadoEnOtro = ayudantesSeleccionados.some((a, idx) => idx !== index && a.idGuia === gId);
                return !yaSeleccionadoEnOtro;
              });

              return (
                <div key={index} className="flex items-center gap-3">
                  <select
                    value={item.idGuia}
                    onChange={(e) => cambiarGuiaAyudante(index, e.target.value)}
                    className="flex-1 p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none text-sm"
                  >
                    {guiasDisponiblesParaEsteSelect.map((g) => {
                      const gId = g.idUsuario || g.idGuia || g.id;
                      return (
                        <option key={gId} value={gId}>
                          {g.apellido}, {g.nombre} (Capacidad: {g.capacidadEstandar})
                        </option>
                      );
                    })}
                  </select>
                  <button
                    type="button"
                    onClick={() => eliminarAyudante(index)}
                    className="text-stone-400 hover:text-red-600 font-bold px-3 py-2 bg-stone-100 hover:bg-red-50 rounded-lg border border-stone-200 transition-colors"
                  >
                    ✕
                  </button>
                </div>
              );
            })}
          </div>

          <div>
            <button
              type="button"
              onClick={agregarAyudante}
              disabled={guiasDisponiblesParaAgregar.length === 0}
              className={`text-xs font-bold px-4 py-2 rounded-lg transition-colors border ${
                guiasDisponiblesParaAgregar.length === 0
                  ? 'bg-stone-100 text-stone-400 border-stone-200 cursor-not-allowed'
                  : 'text-emerald-800 border-emerald-300 bg-emerald-50/50 hover:bg-emerald-100/60'
              }`}
            >
              + Agregar guía ayudante
            </button>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-bold text-stone-800">Recálculo de cupos</h3>
            <p className="text-xs text-stone-500">
              Variación = Cupo base × 0.25 × cantidad de guías ayudantes (redondeado hacia abajo).
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-2">
            <div className="p-3 bg-stone-50 border border-stone-200 rounded-lg">
              <span className="block text-[11px] font-semibold text-stone-500 mb-1">Cupo base</span>
              <span className="text-lg font-bold text-stone-900">{cupoBase}</span>
            </div>
            <div className="p-3 bg-stone-50 border border-stone-200 rounded-lg">
              <span className="block text-[11px] font-semibold text-stone-500 mb-1">Ayudantes asignados</span>
              <span className="text-lg font-bold text-stone-900">{cantidadAyudantes}</span>
            </div>
            <div className="p-3 bg-stone-50 border border-stone-200 rounded-lg">
              <span className="block text-[11px] font-semibold text-stone-500 mb-1">Variación</span>
              <span className="text-lg font-bold text-emerald-700">+{variacionCalculada}</span>
            </div>
            <div className="p-3 bg-emerald-50 border border-emerald-300 rounded-lg">
              <span className="block text-[11px] font-semibold text-emerald-800 mb-1">Cupo nuevo</span>
              <span className="text-lg font-bold text-emerald-900">{cupoNuevo}</span>
            </div>
          </div>

          <div className="flex flex-col md:flex-row justify-between items-start md:items-center text-xs text-stone-600 pt-2 gap-2">
            <div>
              Cupo máximo definido: <span className="font-bold text-stone-800">{cupoMaximo}</span> | Cupos ocupados actualmente: <span className="font-bold text-stone-800">{cuposOcupados}</span>
            </div>
          </div>

          <div className="p-3 bg-stone-50 border border-stone-200 rounded-lg flex items-center justify-between text-xs">
            <span className="text-stone-700 font-medium">
              {superaMaximo ? '⚠️ El cupo nuevo excede el límite máximo permitido.' : 'Dentro de los límites permitidos. Estado operacional resultante:'}
            </span>
            <span className={`px-2.5 py-1 font-bold rounded-full ${superaMaximo ? 'bg-red-100 text-red-800' : 'bg-emerald-100 text-emerald-800'}`}>
              {estadoResultante}
            </span>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-5 py-2.5 border border-stone-300 text-stone-700 text-sm font-semibold rounded-lg hover:bg-stone-50 transition-colors"
          >
            Volver
          </button>
          <button
            type="submit"
            disabled={superaMaximo}
            className={`px-6 py-2.5 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors ${
              superaMaximo ? 'bg-stone-400 cursor-not-allowed opacity-60' : 'bg-[#1C4D38] hover:bg-[#153B2B]'
            }`}
          >
            Guardar cambios
          </button>
        </div>

      </form>
    </div>
  );
}