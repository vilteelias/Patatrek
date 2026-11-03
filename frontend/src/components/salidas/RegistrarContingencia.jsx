import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

export function RegistrarContingencia() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [salidaInfo, setSalidaInfo] = useState(null);
  const [mensaje, setMensaje] = useState(null);

  // Estados del formulario
  const [tipoContingencia, setTipoContingencia] = useState('Climática');
  const [motivo, setMotivo] = useState('');
  const [resolucion, setResolucion] = useState('Cancelación');
  
  // Campos dinámicos
  const [nuevaFecha, setNuevaFecha] = useState('');
  const [nuevoHorario, setNuevoHorario] = useState('');
  const [nuevoRecorrido, setNuevoRecorrido] = useState('');

  useEffect(() => {
    fetch(`http://localhost:8080/api/salidas/${id}`)
      .then((res) => (res.ok ? res.json() : Promise.reject('Error al cargar la salida')))
      .then((data) => setSalidaInfo(data))
      .catch((err) => setMensaje({ tipo: 'error', texto: err }));
  }, [id]);

  const handleGuardar = (e) => {
    e.preventDefault();
    if (!motivo.trim()) {
      setMensaje({ tipo: 'error', texto: 'El motivo es obligatorio.' });
      return;
    }

    const dtoEnvio = {
      tipoContingencia,
      motivo,
      resolucion: resolucion.toUpperCase().replace('Ó', 'O'), // Normaliza a CANCELACION, REPROGRAMACION, MODIFICACION
      nuevaFecha: resolucion === 'Reprogramación' ? nuevaFecha : null,
      nuevoHorario: resolucion === 'Reprogramación' ? nuevoHorario : null,
      nuevoRecorrido: (resolucion === 'Reprogramación' || resolucion === 'Modificación') ? nuevoRecorrido : null,
    };

    fetch(`http://localhost:8080/api/contingencias/${id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dtoEnvio),
    })
      .then((res) => {
        if (!res.ok) throw new Error('Error al registrar la contingencia');
        setMensaje({ tipo: 'exito', texto: 'Contingencia registrada y notificada con éxito.' });
        setTimeout(() => navigate('/salidas'), 2500);
      })
      .catch((err) => setMensaje({ tipo: 'error', texto: err.message }));
  };

  const formatearFecha = (fecha) => {
    if (!fecha) return 'N/D';
    if (Array.isArray(fecha)) {
      return `${fecha[2].toString().padStart(2, '0')}/${fecha[1].toString().padStart(2, '0')}/${fecha[0]}`;
    }
    return fecha;
  };

  if (!salidaInfo) {
    return <div className="max-w-4xl mx-auto p-6 text-center text-stone-600">Cargando datos...</div>;
  }

  return (
    <div className="max-w-3xl mx-auto p-6 bg-[#FAF8F5] rounded-xl border border-stone-200 mt-6 shadow-sm font-sans">
      <div className="text-xs text-stone-500 mb-2">
        Salidas / <span className="text-stone-700 font-medium">Salida #{id}</span> / Registrar contingencia
      </div>

      <h2 className="text-2xl font-bold text-stone-900 mb-1">Registrar contingencia de salida</h2>
      <p className="text-sm text-stone-600 mb-6">
        Registrá un evento imprevisto que afecte la salida y definí su resolución. El sistema actualiza automáticamente la salida, las reservas afectadas y notifica a los participantes.
      </p>

      {mensaje && (
        <div className={`p-3 rounded-lg mb-4 text-sm font-medium ${mensaje.tipo === 'exito' ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-red-100 text-red-900 border border-red-300'}`}>
          {mensaje.texto}
        </div>
      )}

      <form onSubmit={handleGuardar} className="space-y-6">
        
        {/* Tarjeta de Información de la Salida */}
        <div className="bg-white p-4 rounded-xl border border-stone-200 shadow-sm flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-stone-800">{salidaInfo.excursion?.nombre || 'Excursión N/D'}</h3>
            <p className="text-sm text-stone-500 mt-1">
              {formatearFecha(salidaInfo.fechaInicio)} • {salidaInfo.cuposOcupados || 0} reservas
            </p>
          </div>
          <span className="px-3 py-1 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-full">
            {salidaInfo.estadoSalida || 'DISPONIBLE'}
          </span>
        </div>

        {/* Tipo de Contingencia y Motivo */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-5">
          <div>
            <label className="block text-sm font-bold text-stone-800 mb-2">Tipo de contingencia</label>
            <div className="flex gap-3">
              {['Climática', 'Operativa'].map((tipo) => (
                <button
                  key={tipo}
                  type="button"
                  onClick={() => setTipoContingencia(tipo)}
                  className={`px-4 py-2 rounded-lg text-sm font-medium border transition-colors ${
                    tipoContingencia === tipo
                      ? 'bg-emerald-50 border-emerald-600 text-emerald-800'
                      : 'bg-white border-stone-300 text-stone-600 hover:bg-stone-50'
                  }`}
                >
                  {tipo}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold text-stone-800 mb-2">Motivo</label>
            <textarea
              rows="3"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ej: Alerta de temporal..."
              className="w-full p-3 bg-stone-50 border border-stone-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-700 outline-none"
              required
            />
          </div>
        </div>

        {/* Resolución */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-5">
          <div>
            <label className="block text-sm font-bold text-stone-800 mb-2">Resolución</label>
            <div className="flex flex-wrap gap-3">
              {['Cancelación', 'Reprogramación', 'Modificación'].map((res) => (
                <button
                  key={res}
                  type="button"
                  onClick={() => setResolucion(res)}
                  className={`px-4 py-2 rounded-lg text-sm font-medium border transition-colors ${
                    resolucion === res
                      ? 'bg-stone-800 border-stone-900 text-white'
                      : 'bg-white border-stone-300 text-stone-600 hover:bg-stone-50'
                  }`}
                >
                  {res}
                </button>
              ))}
            </div>
          </div>

          {/* Bloques dinámicos según resolución */}
          {resolucion === 'Cancelación' && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-800">
              <strong className="block mb-1">Esta acción cancela la salida por completo.</strong>
              Las reservas pasan a Pendiente de devolución, se notificará a cada titular y participante informando la cancelación y el inicio del trámite de devolución. Se liberan los guías asignados, plazas de refugio y equipamiento.
            </div>
          )}

          {resolucion === 'Reprogramación' && (
            <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg space-y-4">
              <div className="text-sm text-orange-800 mb-2">
                Se actualizará el cronograma y se notificará a los clientes para que acepten o rechacen la nueva fecha.
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Nueva Fecha</label>
                  <input type="date" value={nuevaFecha} onChange={(e) => setNuevaFecha(e.target.value)} className="w-full p-2 border border-stone-300 rounded-md text-sm" required />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Nuevo Horario</label>
                  <input type="time" value={nuevoHorario} onChange={(e) => setNuevoHorario(e.target.value)} className="w-full p-2 border border-stone-300 rounded-md text-sm" required />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">Recorrido Alternativo (Opcional)</label>
                <input type="text" value={nuevoRecorrido} onChange={(e) => setNuevoRecorrido(e.target.value)} className="w-full p-2 border border-stone-300 rounded-md text-sm" />
              </div>
            </div>
          )}

          {resolucion === 'Modificación' && (
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg space-y-4">
              <div className="text-sm text-blue-800 mb-2">
                El cronograma y las reservas se mantienen intactas. Solo se modifica el recorrido y se notifica del cambio.
              </div>
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">Nuevo Recorrido</label>
                <input type="text" value={nuevoRecorrido} onChange={(e) => setNuevoRecorrido(e.target.value)} className="w-full p-2 border border-stone-300 rounded-md text-sm" required />
              </div>
            </div>
          )}
        </div>

        {/* Botones de acción */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-5 py-2.5 border border-stone-300 text-stone-700 text-sm font-semibold rounded-lg hover:bg-stone-50 transition-colors"
          >
            Cancelar
          </button>
          <button
            type="submit"
            className={`px-6 py-2.5 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors ${
              resolucion === 'Cancelación' ? 'bg-red-700 hover:bg-red-800' : 'bg-[#1C4D38] hover:bg-[#153B2B]'
            }`}
          >
            Confirmar {resolucion.toLowerCase()}
          </button>
        </div>

      </form>
    </div>
  );
}