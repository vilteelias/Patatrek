import { useState, useEffect } from 'react';

export function PlanificarSalida({ onSalidaCreada }) {
  // Estado para los catálogos del Backend
  const [excursiones, setExcursiones] = useState([]);
  const [guias, setGuias] = useState([]);
  const [refugiosCatalog, setRefugiosCatalog] = useState([]);

  // Estado del formulario
  const [formData, setFormData] = useState({
    idExcursion: '',
    fechaInicio: '2026-11-14',
    horaInicio: '08:00',
    fechaFinalizacion: '2026-11-14',
    horaFinalizacion: '17:30',
    idGuiaResponsable: '',
    cupoBase: 0,
    cupoMaximo: '',
    capacidadRefugio: 10,
  });

  // Estado para la asignación por noches
  const [refugiosNoches, setRefugiosNoches] = useState([]);
  const [mensaje, setMensaje] = useState(null);

  // Cargar datos iniciales
  useEffect(() => {
    fetch('http://localhost:8080/api/excursiones')
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => {
        const lista = Array.isArray(data) ? data : [];
        setExcursiones(lista);
        if (lista.length > 0) setFormData((prev) => ({ ...prev, idExcursion: lista[0].idExcursion || lista[0].id }));
      })
      .catch(() => setExcursiones([]));

    fetch('http://localhost:8080/api/guias')
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => setGuias(Array.isArray(data) ? data : []))
      .catch(() => setGuias([]));

    fetch('http://localhost:8080/api/refugios')
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => {
        const lista = Array.isArray(data) ? data : [];
        setRefugiosCatalog(lista);
        if (lista.length > 0) {
          setRefugiosNoches([
            { idRefugio: lista[0].idRefugio || lista[0].id, ordenVisita: 1 },
            { idRefugio: lista[1]?.idRefugio || lista[0].idRefugio || lista[0].id, ordenVisita: 2 },
          ]);
        }
      })
      .catch(() => setRefugiosCatalog([]));
  }, []);

  // -------------------------------------------------------------
  // CONDICIONES DE HABILITACIÓN PROGRESIVA
  // -------------------------------------------------------------
  // Bloque 1 (Excursión) -> Habilita Bloque 2 (Cronograma)
  const paso1Completo = Boolean(formData.idExcursion);

  // Bloque 2 (Cronograma) -> Habilita Bloque 3 (Guía y Cupos)
  const cronogramaValido =
    Boolean(formData.fechaInicio && formData.horaInicio && formData.fechaFinalizacion && formData.horaFinalizacion) &&
    formData.fechaFinalizacion >= formData.fechaInicio &&
    !(formData.fechaInicio === formData.fechaFinalizacion && formData.horaFinalizacion <= formData.horaInicio);

  const paso2Completo = paso1Completo && cronogramaValido;

  // Bloque 3 (Guía y Cupos) -> Habilita Bloque 4 (Refugios/Submit)
  const paso3Completo =
    paso2Completo &&
    Boolean(formData.idGuiaResponsable) &&
    Boolean(formData.cupoMaximo) &&
    Number(formData.cupoMaximo) > 0 &&
    formData.cupoBase < Number(formData.cupoMaximo);

  const esMasDeUnDia = formData.fechaInicio && formData.fechaFinalizacion && formData.fechaInicio !== formData.fechaFinalizacion;

  // Manejador general
  const handleChange = (e) => {
    const { name, value } = e.target;
    const updatedForm = { ...formData, [name]: value };

    // Validaciones de fechas y horas
    if (updatedForm.fechaFinalizacion && updatedForm.fechaInicio && updatedForm.fechaFinalizacion < updatedForm.fechaInicio) {
      setMensaje({ tipo: 'error', texto: 'La fecha de finalización no puede ser anterior a la fecha de inicio.' });
    } else if (
      updatedForm.fechaInicio &&
      updatedForm.fechaFinalizacion &&
      updatedForm.fechaInicio === updatedForm.fechaFinalizacion &&
      updatedForm.horaInicio &&
      updatedForm.horaFinalizacion &&
      updatedForm.horaFinalizacion <= updatedForm.horaInicio
    ) {
      setMensaje({
        tipo: 'error',
        texto: 'Si la salida finaliza el mismo día, la hora de finalización debe ser posterior a la hora de inicio.',
      });
    } else {
      setMensaje(null);
    }

    if (name === 'idGuiaResponsable') {
      const guiaSel = guias.find((g) => (g.idUsuario || g.idGuia || g.id) === Number(value));
      setFormData((prev) => ({
        ...prev,
        idGuiaResponsable: Number(value),
        cupoBase: guiaSel ? guiaSel.capacidadEstandar : 0,
      }));
      return;
    }

    setFormData((prev) => ({
      ...prev,
      [name]: name === 'idExcursion' || name === 'cupoMaximo' || name === 'capacidadRefugio' ? Number(value) : value,
    }));
  };

  const handleRefugioChange = (index, idRefugio) => {
    const nuevos = [...refugiosNoches];
    nuevos[index].idRefugio = Number(idRefugio);
    setRefugiosNoches(nuevos);
  };

  const agregarRefugio = () => {
    const primerRefugioId = refugiosCatalog.length > 0 ? refugiosCatalog[0].idRefugio || refugiosCatalog[0].id : 1;
    setRefugiosNoches((prev) => [...prev, { idRefugio: primerRefugioId, ordenVisita: prev.length + 1 }]);
  };

  const eliminarRefugio = (index) => {
    setRefugiosNoches((prev) =>
      prev.filter((_, i) => i !== index).map((item, idx) => ({ ...item, ordenVisita: idx + 1 }))
    );
  };

  const totalPlazasReservadas = refugiosNoches.length * (formData.capacidadRefugio || 0);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!paso3Completo) return;

    const dtoEnvio = {
      idExcursion: Number(formData.idExcursion),
      fechaInicio: formData.fechaInicio,
      horaInicio: formData.horaInicio,
      fechaFinalizacion: formData.fechaFinalizacion,
      horaFinalizacion: formData.horaFinalizacion,
      idGuiaResponsable: Number(formData.idGuiaResponsable),
      cupoMaximo: Number(formData.cupoMaximo),
      refugiosNoches: esMasDeUnDia
        ? refugiosNoches.map((item) => ({
            idRefugio: item.idRefugio,
            ordenVisita: item.ordenVisita,
            plazasReservadas: Number(formData.capacidadRefugio),
          }))
        : [],
    };

    fetch('http://localhost:8080/api/salidas', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dtoEnvio),
    })
      .then((res) => {
        if (!res.ok) throw new Error('Error al guardar la salida en la base de datos');
        return res.json();
      })
      .then((data) => {
        setMensaje({ tipo: 'exito', texto: '¡Salida planificada y registrada con éxito!' });
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
        Completá los datos requeridos. Cada tarjeta se habilitará una vez completada la sección anterior.
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

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* BLOQUE 1: Excursión base */}
        <div className="bg-white p-5 rounded-xl border border-stone-200 shadow-sm">
          <label className="block text-sm font-bold text-stone-800 mb-2">1. Excursión base</label>
          <select
            name="idExcursion"
            value={formData.idExcursion}
            onChange={handleChange}
            required
            className="w-full p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 focus:ring-2 focus:ring-emerald-700 outline-none"
          >
            {excursiones.length === 0 && <option value="">Cargando excursiones...</option>}
            {excursiones.map((e) => (
              <option key={e.idExcursion || e.id} value={e.idExcursion || e.id}>
                {e.nombre}
              </option>
            ))}
          </select>
        </div>

        {/* BLOQUE 2: Cronograma */}
        <div
          className={`bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4 transition-all duration-300 ${
            !paso1Completo ? 'opacity-40 pointer-events-none bg-stone-50' : ''
          }`}
        >
          <div>
            <h3 className="text-base font-bold text-stone-800">2. Cronograma</h3>
            <p className="text-xs text-stone-500">
              Si la fecha de finalización es distinta a la de inicio, requerirá asignación de refugios.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Fecha de inicio</label>
              <input
                type="date"
                name="fechaInicio"
                disabled={!paso1Completo}
                value={formData.fechaInicio}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Hora de inicio</label>
              <input
                type="time"
                name="horaInicio"
                disabled={!paso1Completo}
                value={formData.horaInicio}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Fecha de finalización</label>
              <input
                type="date"
                name="fechaFinalizacion"
                min={formData.fechaInicio}
                disabled={!paso1Completo}
                value={formData.fechaFinalizacion}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Hora de finalización</label>
              <input
                type="time"
                name="horaFinalizacion"
                disabled={!paso1Completo}
                value={formData.horaFinalizacion}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
            </div>
          </div>
        </div>

        {/* BLOQUE 3: Guía responsable y cupos */}
        <div
          className={`bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4 transition-all duration-300 ${
            !paso2Completo ? 'opacity-40 pointer-events-none bg-stone-50' : ''
          }`}
        >
          <div>
            <h3 className="text-base font-bold text-stone-800">3. Guía responsable y cupos</h3>
            <p className="text-xs text-stone-500">
              El cupo base se asigna automáticamente desde la capacidad estándar del guía.
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Guía responsable</label>
              <select
                name="idGuiaResponsable"
                disabled={!paso2Completo}
                value={formData.idGuiaResponsable}
                onChange={handleChange}
                required
                className="w-full p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 outline-none"
              >
                <option value="">Seleccioná un guía</option>
                {guias.map((g) => (
                  <option key={g.idUsuario || g.idGuia || g.id} value={g.idUsuario || g.idGuia || g.id}>
                    {g.apellido}, {g.nombre} (Capacidad: {g.capacidadEstandar})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Cupo máximo de la salida</label>
              <input
                type="number"
                name="cupoMaximo"
                disabled={!paso2Completo}
                placeholder="Ej. 18"
                value={formData.cupoMaximo}
                onChange={handleChange}
                required
                className="w-1/3 p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
              <p className="text-[11px] text-stone-400 mt-1">
                Cupo base del guía seleccionado: {formData.cupoBase}. Debe ser menor al cupo máximo.
              </p>
            </div>
          </div>
        </div>

        {/* BLOQUE 4: Refugios (Sólo si es de más de un día) */}
        {esMasDeUnDia && (
          <div
            className={`bg-white p-5 rounded-xl border border-stone-200 shadow-sm space-y-4 transition-all duration-300 ${
              !paso3Completo ? 'opacity-40 pointer-events-none bg-stone-50' : ''
            }`}
          >
            <div>
              <h3 className="text-base font-bold text-stone-800">4. Refugios y plazas de alojamiento</h3>
              <p className="text-xs text-stone-500">
                Seleccioná los refugios correspondientes a cada noche de la salida.
              </p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-600 mb-1">Capacidad estándar por refugio</label>
              <input
                type="number"
                name="capacidadRefugio"
                disabled={!paso3Completo}
                value={formData.capacidadRefugio}
                onChange={handleChange}
                className="w-1/3 p-2.5 bg-white border border-stone-300 rounded-lg text-stone-800 outline-none"
              />
            </div>

            <div className="space-y-3 pt-2">
              {refugiosNoches.map((item, index) => (
                <div key={index} className="flex items-center gap-3">
                  <span className="text-xs font-semibold text-stone-500 bg-stone-100 px-3 py-2 rounded-lg border border-stone-200">
                    Noche {item.ordenVisita}
                  </span>
                  <select
                    disabled={!paso3Completo}
                    value={item.idRefugio}
                    onChange={(e) => handleRefugioChange(index, e.target.value)}
                    className="flex-1 p-2.5 bg-stone-50/50 border border-stone-300 rounded-lg text-stone-800 outline-none text-sm"
                  >
                    {refugiosCatalog.map((r) => (
                      <option key={r.idRefugio || r.id} value={r.idRefugio || r.id}>
                        {r.nombre}
                      </option>
                    ))}
                  </select>
                  <span className="text-xs font-semibold text-stone-500">{formData.capacidadRefugio || 0} plazas</span>
                  {refugiosNoches.length > 1 && (
                    <button
                      type="button"
                      disabled={!paso3Completo}
                      onClick={() => eliminarRefugio(index)}
                      className="text-stone-400 hover:text-red-600 font-bold px-2"
                    >
                      ✕
                    </button>
                  )}
                </div>
              ))}
            </div>

            <div className="flex justify-between items-center pt-2">
              <button
                type="button"
                disabled={!paso3Completo}
                onClick={agregarRefugio}
                className="text-xs font-bold text-stone-700 border border-stone-300 px-3 py-2 rounded-lg hover:bg-stone-50"
              >
                + Agregar refugio
              </button>
              <div className="text-xs text-stone-500 font-semibold">
                Total plazas: <span className="font-bold text-stone-800">{totalPlazasReservadas}</span>
              </div>
            </div>
          </div>
        )}

        {/* Botón final */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="submit"
            disabled={!paso3Completo || mensaje?.tipo === 'error'}
            className={`px-6 py-2.5 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors ${
              !paso3Completo || mensaje?.tipo === 'error'
                ? 'bg-stone-400 cursor-not-allowed opacity-50'
                : 'bg-[#1C4D38] hover:bg-[#153B2B]'
            }`}
          >
            Planificar salida
          </button>
        </div>
      </form>
    </div>
  );
}