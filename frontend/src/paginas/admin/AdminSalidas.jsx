import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function AdminSalidas() {
  const [salidas, setSalidas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetch('http://localhost:8080/api/salidas')
      .then((res) => {
        if (!res.ok) throw new Error('Error al cargar las salidas programadas');
        return res.json();
      })
      .then((data) => {
        setSalidas(data);
        setCargando(false);
      })
      .catch((err) => {
        setError(err.message);
        setCargando(false);
      });
  }, []);

  const formatearFecha = (fecha) => {
    if (!fecha) return 'N/D';
    if (Array.isArray(fecha)) {
      return `${fecha[2].toString().padStart(2, '0')}/${fecha[1].toString().padStart(2, '0')}/${fecha[0]}`;
    }
    return fecha;
  };

  if (cargando) {
    return <div className="p-8 text-center text-stone-600 font-sans">Cargando salidas...</div>;
  }

  return (
    <div className="p-8 max-w-7xl mx-auto font-sans">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-stone-900">Salidas programadas</h1>
          <p className="text-sm text-stone-600">Visualiza y administra las realizaciones programadas de las excursiones.</p>
        </div>
        <button
          onClick={() => navigate('/admin/salidas/nueva')}
          className="bg-[#1C4D38] hover:bg-[#153B2B] text-white px-4 py-2 rounded-lg text-sm font-semibold shadow-sm transition-colors"
        >
          + Planificar salida
        </button>
      </div>

      {error && (
        <div className="p-4 mb-4 bg-red-50 border border-red-200 text-red-800 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div className="bg-white rounded-xl border border-stone-200 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-stone-50 border-b border-stone-200 text-stone-500 text-xs uppercase tracking-wider">
              <th className="p-4 font-semibold">Excursión</th>
              <th className="p-4 font-semibold">Fecha inicio</th>
              <th className="p-4 font-semibold">Cupos Ocupados / Máx</th>
              <th className="p-4 font-semibold">Estado</th>
              <th className="p-4 font-semibold text-right">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-200 text-sm text-stone-800">
            {salidas.length === 0 ? (
              <tr>
                <td colSpan="5" className="p-6 text-center text-stone-500">
                  No hay salidas registradas en el sistema.
                </td>
              </tr>
            ) : (
              salidas.map((salida) => (
                <tr key={salida.idSalida} className="hover:bg-stone-50/50 transition-colors">
                  <td className="p-4 font-medium text-stone-900">
                    {salida.excursion?.nombre || 'Excursión sin nombre'}
                  </td>
                  <td className="p-4 text-stone-600">{formatearFecha(salida.fechaInicio)}</td>
                  <td className="p-4 text-stone-600">
                    {salida.cuposOcupados || 0} / {salida.cupoMaximo || '-'}
                  </td>
                  <td className="p-4">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                      salida.estadoSalida === 'DISPONIBLE' ? 'bg-emerald-100 text-emerald-800' :
                      salida.estadoSalida === 'CANCELADA' ? 'bg-red-100 text-red-800' :
                      'bg-stone-100 text-stone-700'
                    }`}>
                      {salida.estadoSalida || 'DISPONIBLE'}
                    </span>
                  </td>
                  <td className="p-4 text-right space-x-2">
                    <button
                      onClick={() => navigate(`/admin/salidas/${salida.idSalida}/asignar-cupos`)}
                      className="text-stone-700 hover:text-stone-950 font-medium text-xs border border-stone-300 px-3 py-1.5 rounded-lg hover:bg-stone-50 transition-colors"
                    >
                      Asignar cupos
                    </button>
                    <button
                      onClick={() => navigate(`/admin/salidas/${salida.idSalida}/contingencia`)}
                      className="text-amber-800 hover:text-amber-950 font-medium text-xs border border-amber-300 bg-amber-50 px-3 py-1.5 rounded-lg hover:bg-amber-100 transition-colors"
                    >
                      Registrar contingencia
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}