import { useState } from 'react';

export function PlanificarSalida({ onSalidaCreada }) {
  const [formData, setFormData] = useState({
    idExcursion: 1,
    fechaInicio: '2026-11-14',
    horaInicio: '08:00',
    fechaFin: '2026-11-16',
    horaFin: '17:30',
    guiaResponsable: '',
    cupoMaximo: '',
    capacidadRefugio: 10,
  });

  const [refugiosNoches, setRefugiosNoches] = useState([
    { noche: 1, nombre: 'Refugio Cerro Alto' },
    { noche: 2, nombre: 'Refugio Lago Escondido' },
  ]);

  const [mensaje, setMensaje] = useState(null);

  const esMasDeUnDia = formData.fechaInicio && formData.fechaFin && formData.fechaInicio !== formData.fechaFin;

  const handleChange = (e) => {
    const { name, value } = e.target;

    // Validación de fechas
    if (name === 'fechaFin' && formData.fechaInicio && value < formData.fechaInicio) {
      setMensaje({ tipo: 'error', texto: 'La fecha de finalización no puede ser anterior a la fecha de inicio.' });
      return;
    }

    if (name === 'fechaInicio' && formData.fechaFin && value > formData.fechaFin) {
      setFormData((prev) => ({ ...prev, fechaInicio: value, fechaFin: value }));
      setMensaje(null);
      return;
    }

    setMensaje(null);
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'idExcursion' || name === 'cupoMaximo' || name === 'capacidadRefugio' ? Number(value) : value,
    }));
  };

  const handleRefugioChange = (index, value) => {
    const nuevos = [...refugiosNoches];
    nuevos[index].nombre = value;
    setRefugiosNoches(nuevos);
  };

  const agregarRefugio = () => {
    setRefugiosNoches((prev) => [
      ...prev,
      { noche: prev.length + 1, nombre: 'Refugio Cerro Alto' },
    ]);
  };

  const eliminarRefugio = (index) => {
    setRefugiosNoches((prev) => prev.filter((_, i) => i !== index).map((item, idx) => ({ ...item, noche: idx + 1 })));
  };

  const totalPlazasReservadas = refugiosNoches.length * (formData.capacidadRefugio || 0);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formData.fechaFin < formData.fechaInicio) {
      setMensaje({ tipo: 'error', texto: 'Revisá el rango de fechas antes de continuar.' });
      return;
    }

    fetch('http://localhost:8080/api/salidas', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData),
    })
      .then((res) => {
        if (!res.ok) throw new Error('Error al planificar la salida');
        return res.json();
      })
      .then((data) => {
        setMensaje({ tipo: 'exito', texto: '¡Salida planificada correctamente!' });
        if (onSalidaCreada) onSalidaCreada(data);
      })
      .catch((err) => {
        setMensaje({ tipo: 'error', texto: err.message });
      });
  };

  return (
    <div className="max-w-4xl mx-auto p-6 bg-[#FAF8F5] rounded-xl border border-stone-200 mt-6 shadow-sm font-sans">
      <h2 className="text-2xl font-bold text-stone-900 mb-1">Planificar salida</h2>
      <p className="text-sm text-stone-600 mb-6">
        Definí el cronograma, el guía responsable y los recursos de esta nueva realización de una excursión ya registrada en el catálogo.
      </p>

      {mensaje && (
        <div className={`p-3 rounded-lg mb-4 text-sm font-medium ${mensaje.tipo === 'exito' ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-red-100 text-red-900 border border-red-300'}`}>
          {mensaje.texto}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Excursión base */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm">
          <label className="block text-sm font-bold text-stone-800 mb-2">Excursión base</label>
          <select
            name="idExcursion"
            value={formData.idExcursion}
            onChange={handleChange}
            className="w-full p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
          >
            <option value={1}>Travesía Cerro Alto — Río Senguer</option>
            <option value={2}>Circuito Lagos Patagónicos</option>
            <option value={3}>Ascenso Cerro Perito Moreno</option>
          </select>
        </div>

        {/* Cronograma */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-bold text-stone-800">Cronograma</h3>
            <p className="text-xs text-stone-500">
              Si la fecha de finalización es distinta a la de inicio, la salida se considera de más de un día y requerirá refugios.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Fecha de inicio</label>
              <input
                type="date"
                name="fechaInicio"
                value={formData.fechaInicio}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Hora de inicio</label>
              <input
                type="time"
                name="horaInicio"
                value={formData.horaInicio}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Fecha de finalización</label>
              <input
                type="date"
                name="fechaFin"
                min={formData.fechaInicio}
                value={formData.fechaFin}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Hora de finalización</label>
              <input
                type="time"
                name="horaFin"
                value={formData.horaFin}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              />
            </div>
          </div>

          {esMasDeUnDia && (
            <div className="p-3 bg-amber-50/70 border border-amber-200 text-amber-900 rounded-lg text-xs flex items-center gap-2">
              <span>🕒</span>
              <span>Salida de más de un día: se habilita la asignación de refugios más abajo.</span>
            </div>
          )}
        </div>

        {/* Guía responsable y cupos */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-bold text-stone-800">Guía responsable y cupos</h3>
            <p className="text-xs text-stone-500">
              El cupo base de la salida se toma automáticamente de la capacidad estándar del guía asignado.
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Guía responsable</label>
              <select
                name="guiaResponsable"
                value={formData.guiaResponsable}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              >
                <option value="">Seleccioná un guía</option>
                <option value="Almeida, Martín">Almeida, Martín</option>
                <option value="Coronel, Brenda">Coronel, Brenda</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Cupo máximo de la salida</label>
              <input
                type="number"
                name="cupoMaximo"
                placeholder="Ej. 18"
                value={formData.cupoMaximo}
                onChange={handleChange}
                required
                className="w-1/3 p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
              />
              <p className="text-[11px] text-stone-400 mt-1">
                El cupo base del guía seleccionado debe ser menor que el cupo máximo.
              </p>
            </div>
          </div>
        </div>

        {/* Refugios y plazas de alojamiento */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-base font-bold text-stone-800">Refugios y plazas de alojamiento</h3>
            <p className="text-xs text-stone-500">
              Obligatorio para salidas de más de un día. Las plazas reservadas quedan disponibles para que los clientes elijan la modalidad "Refugio" al reservar.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-stone-600 mb-1">Capacidad estándar por refugio</label>
            <input
              type="number"
              name="capacidadRefugio"
              value={formData.capacidadRefugio}
              onChange={handleChange}
              className="w-1/3 p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
            />
            <p className="text-[11px] text-stone-400 mt-1">Se aplica por igual a cada refugio que agregues a esta salida.</p>
          </div>

          {/* Listado dinámico de Noches */}
          <div className="space-y-3 pt-2">
            {refugiosNoches.map((item, index) => (
              <div key={index} className="flex items-center gap-3">
                <span className="text-xs font-semibold text-stone-500 bg-stone-100 px-3 py-2 rounded-lg border border-stone-200">
                  Noche {item.noche}
                </span>
                <select
                  value={item.nombre}
                  onChange={(e) => handleRefugioChange(index, e.target.value)}
                  className="flex-1 p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none text-sm"
                >
                  <option value="Refugio Cerro Alto">Refugio Cerro Alto</option>
                  <option value="Refugio Lago Escondido">Refugio Lago Escondido</option>
                </select>
                <span className="text-xs font-semibold text-stone-500">{formData.capacidadRefugio || 0} plazas</span>
                {refugiosNoches.length > 1 && (
                  <button type="button" onClick={() => eliminarRefugio(index)} className="text-stone-400 hover:text-red-600 font-bold px-2">
                    ✕
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className="flex justify-between items-center pt-2">
            <button
              type="button"
              onClick={agregarRefugio}
              className="text-xs font-bold text-stone-700 border border-stone-300 px-3 py-2 rounded-lg hover:bg-stone-50 transition-colors"
            >
              + Agregar refugio
            </button>
            <div className="text-xs text-stone-500 font-semibold">
              Total de plazas reservadas: <span className="font-bold text-stone-800">{totalPlazasReservadas}</span>
            </div>
          </div>
        </div>

        {/* Acciones */}
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" className="px-5 py-2.5 border border-stone-300 text-stone-700 text-sm font-semibold rounded-lg hover:bg-stone-50 transition-colors">
            Cancelar
          </button>
          <button type="submit" className="px-6 py-2.5 bg-[#1C4D38] hover:bg-[#153B2B] text-white text-sm font-semibold rounded-lg shadow-sm transition-colors">
            Planificar salida
          </button>
        </div>
      </form>
    </div>
  );
}