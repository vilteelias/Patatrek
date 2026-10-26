import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  listarEquipamiento,
  modificarExcursion,
  obtenerExcursionAdmin,
  registrarExcursion,
  subirImagenExcursion,
} from '../../api/excursiones.js';
import Campo, { LeyendaObligatorios } from '../../componentes/formulario/Campo.jsx';
import SelectorOpciones, { OPCIONES_SI_NO } from '../../componentes/formulario/SelectorOpciones.jsx';
import Interruptor from '../../componentes/formulario/Interruptor.jsx';
import ModalEquipamiento from '../../componentes/ModalEquipamiento.jsx';
import Icono from '../../componentes/Icono.jsx';

const OPCIONES_DIFICULTAD = [
  { valor: 'BAJA', texto: 'Baja' },
  { valor: 'MEDIA', texto: 'Media' },
  { valor: 'ALTA', texto: 'Alta' },
];

const VACIO = {
  nombre: '',
  descripcionGeneral: '',
  descripcionTecnica: '',
  dificultad: null,
  duracion: '',
  precio: '',
  recorridoBase: '',
  requiereCertificado: null,
  aceptaMenores: null,
  requiereEquipamiento: false,
  equipamiento: [], // [{ idEquipamiento, nombre }]
};

const TIPOS_IMAGEN = ['image/jpeg', 'image/png'];
const MAX_IMAGEN = 5 * 1024 * 1024; // RNF-05

// Mismas reglas que valida el backend, para avisar antes de enviar (flujo 4a del CU-04)
function validar(d) {
  const e = {};
  if (!d.nombre.trim()) e.nombre = 'Ingresá el nombre de la excursión';
  if (!d.descripcionGeneral.trim()) e.descripcionGeneral = 'Ingresá la descripción general';
  if (!d.descripcionTecnica.trim()) e.descripcionTecnica = 'Ingresá la descripción técnica';
  if (d.duracion === '') e.duracion = 'Ingresá la duración en días';
  else if (!Number.isInteger(Number(d.duracion)) || d.duracion < 1 || d.duracion > 30)
    e.duracion = 'La duración debe ser un número entero entre 1 y 30 días';
  if (d.precio === '') e.precio = 'Ingresá el precio base';
  else if (Number(d.precio) <= 0) e.precio = 'El precio debe ser mayor a cero';
  if (!d.recorridoBase.trim()) e.recorridoBase = 'Ingresá el recorrido base';
  if (!d.dificultad) e.dificultad = 'Seleccioná el nivel de dificultad';
  if (d.aceptaMenores === null) e.aceptaMenores = 'Indicá si admite menores de edad';
  if (d.requiereCertificado === null) e.requiereCertificado = 'Indicá si requiere certificado médico';
  if (d.requiereEquipamiento && d.equipamiento.length === 0)
    e.equipamientoIds = 'Marcaste equipamiento obligatorio: agregá al menos un ítem.';
  return e;
}

// CU-04 Registrar excursión  /  CU-07 Modificar excursión (misma pantalla)
function FormularioExcursion() {
  const { id } = useParams();
  const esEdicion = Boolean(id);
  const navigate = useNavigate();

  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [catalogo, setCatalogo] = useState([]);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [cargando, setCargando] = useState(esEdicion);
  const [enviando, setEnviando] = useState(false);

  // En edición: si hay salidas activas, los parámetros operativos quedan bloqueados (RF-06)
  const [bloqueado, setBloqueado] = useState(false);
  const [estado, setEstado] = useState('ACTIVA');

  // Imagen promocional
  const [imagenActual, setImagenActual] = useState(null);
  const [imagenNueva, setImagenNueva] = useState(null); // { archivo, vistaPrevia }
  const inputImagen = useRef(null);

  useEffect(() => {
    listarEquipamiento()
      .then(setCatalogo)
      .catch(() => setErrorGeneral('No se pudo cargar el catálogo de equipamiento.'));

    if (esEdicion) {
      obtenerExcursionAdmin(id)
        .then((e) => {
          setDatos({
            nombre: e.nombre,
            descripcionGeneral: e.descripcionGeneral,
            descripcionTecnica: e.descripcionTecnica,
            dificultad: e.dificultad,
            duracion: e.duracion,
            precio: e.precio,
            recorridoBase: e.recorridoBase,
            requiereCertificado: e.requiereCertificado,
            aceptaMenores: e.aceptaMenores,
            requiereEquipamiento: e.requiereEquipamiento,
            equipamiento: e.equipamientoObligatorio,
          });
          setBloqueado(e.tieneSalidasActivas);
          setEstado(e.estadoExcursion);
          setImagenActual(e.imagenPromocional);
        })
        .catch((err) => setErrorGeneral(err.message))
        .finally(() => setCargando(false));
    }
  }, [id, esEdicion]);

  // Libera la vista previa al cambiar de imagen o salir de la pantalla
  useEffect(() => () => imagenNueva && URL.revokeObjectURL(imagenNueva.vistaPrevia), [imagenNueva]);

  function cambiar(campo, valor) {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrores((e) => ({ ...e, [campo]: undefined }));
  }

  function elegirImagen(archivo) {
    if (!archivo) return;
    if (!TIPOS_IMAGEN.includes(archivo.type)) {
      setErrores((e) => ({ ...e, imagen: 'La imagen debe ser JPG o PNG.' }));
      return;
    }
    if (archivo.size > MAX_IMAGEN) {
      setErrores((e) => ({ ...e, imagen: 'La imagen no puede superar los 5 MB.' }));
      return;
    }
    setErrores((e) => ({ ...e, imagen: undefined }));
    setImagenNueva({ archivo, vistaPrevia: URL.createObjectURL(archivo) });
  }

  function alternarEquipamiento(activo) {
    setDatos((d) => ({ ...d, requiereEquipamiento: activo, equipamiento: activo ? d.equipamiento : [] }));
    setErrores((e) => ({ ...e, requiereEquipamiento: undefined, equipamientoIds: undefined }));
  }

  function agregarItems(ids) {
    const nuevos = catalogo.filter((item) => ids.includes(item.idEquipamiento));
    setDatos((d) => ({ ...d, equipamiento: [...d.equipamiento, ...nuevos] }));
    setErrores((e) => ({ ...e, equipamientoIds: undefined }));
    setModalAbierto(false);
  }

  function quitarItem(idEquipamiento) {
    setDatos((d) => ({ ...d, equipamiento: d.equipamiento.filter((i) => i.idEquipamiento !== idEquipamiento) }));
  }

  function enfocarPrimerError(errs) {
    const primero = Object.keys(errs).find((k) => errs[k]);
    if (!primero) return;
    const elemento = document.getElementById(primero === 'equipamientoIds' ? 'seccion-equipamiento' : primero);
    elemento?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    elemento?.focus?.({ preventScroll: true });
  }

  async function guardar(evento) {
    evento.preventDefault();
    setErrorGeneral('');

    const errs = validar(datos);
    if (Object.keys(errs).length > 0) {
      setErrores(errs);
      setErrorGeneral('Revisá los datos marcados antes de continuar.');
      enfocarPrimerError(errs);
      return;
    }

    const cuerpo = {
      nombre: datos.nombre.trim(),
      descripcionGeneral: datos.descripcionGeneral.trim(),
      descripcionTecnica: datos.descripcionTecnica.trim(),
      dificultad: datos.dificultad,
      duracion: Number(datos.duracion),
      precio: Number(datos.precio),
      recorridoBase: datos.recorridoBase.trim(),
      requiereCertificado: datos.requiereCertificado,
      aceptaMenores: datos.aceptaMenores,
      requiereEquipamiento: datos.requiereEquipamiento,
      equipamientoIds: datos.equipamiento.map((i) => i.idEquipamiento),
    };

    setEnviando(true);
    let guardada;
    try {
      guardada = esEdicion ? await modificarExcursion(id, cuerpo) : await registrarExcursion(cuerpo);
    } catch (e) {
      setErrores(e.errores ?? {});
      setErrorGeneral(e.message);
      enfocarPrimerError(e.errores ?? {});
      setEnviando(false);
      return;
    }

    let aviso = esEdicion
      ? `Los cambios en "${guardada.nombre}" se guardaron correctamente.`
      : `La excursión "${guardada.nombre}" se registró correctamente y quedó Activa.`;

    if (imagenNueva) {
      try {
        await subirImagenExcursion(guardada.idExcursion, imagenNueva.archivo);
      } catch (e) {
        aviso += ` Atención: la imagen no se pudo subir (${e.message}). Podés intentarlo de nuevo desde Modificar.`;
      }
    }
    navigate('/admin/excursiones', { state: { aviso } });
  }

  if (cargando) return <p className="cargando">Cargando excursión…</p>;

  const imagenVisible = imagenNueva?.vistaPrevia ?? imagenActual;
  const idsAsignados = datos.equipamiento.map((i) => i.idEquipamiento);

  return (
    <section className="admin-pagina admin-pagina--formulario">
      <p className="miga">
        <Link to="/admin/excursiones">Excursiones</Link> / {esEdicion ? 'Modificar excursión' : 'Nueva excursión'}
      </p>
      <h1 className="titulo">
        {esEdicion ? 'Modificar excursión' : 'Registrar excursión'}
        {esEdicion && estado === 'INACTIVA' && <span className="estado estado--inactiva">Inactiva</span>}
      </h1>
      <p className="subtitulo">
        {esEdicion
          ? 'Actualizá la información de la excursión.'
          : 'Completá los datos generales, técnicos y operativos de la nueva excursión para publicarla en el catálogo comercial.'}
      </p>

      {bloqueado && (
        <div className="alerta alerta--info">
          <Icono nombre="candado" />
          <div>
            <strong>Esta excursión tiene salidas programadas activas.</strong> Solo podés modificar el nombre, las
            descripciones y la imagen. Los parámetros operativos quedan bloqueados para preservar las condiciones de las
            reservas existentes.
          </div>
        </div>
      )}

      {errorGeneral && (
        <div className="alerta alerta--error" role="alert">
          <Icono nombre="alerta" /> {errorGeneral}
        </div>
      )}

      <form onSubmit={guardar} noValidate>
        <LeyendaObligatorios />

        {/* ---------------- Datos generales ---------------- */}
        <section className="seccion">
          <h2 className="seccion__titulo">Datos generales</h2>
          <p className="seccion__ayuda">Información visible para clientes y visitantes en el catálogo.</p>

          <Campo
            id="nombre"
            etiqueta="Nombre de la excursión"
            obligatorio
            error={errores.nombre}
            ayuda="Debe ser único en el catálogo, no puede repetirse con excursiones activas ni inactivas."
          >
            <input
              id="nombre"
              className="control"
              maxLength={100}
              value={datos.nombre}
              onChange={(e) => cambiar('nombre', e.target.value)}
              placeholder="Ej. Travesía Cerro Alto — Río Senguer"
              aria-required="true"
              aria-invalid={!!errores.nombre}
            />
          </Campo>

          <Campo id="descripcionGeneral" etiqueta="Descripción general" obligatorio error={errores.descripcionGeneral}>
            <textarea
              id="descripcionGeneral"
              className="control"
              rows={3}
              maxLength={1000}
              value={datos.descripcionGeneral}
              onChange={(e) => cambiar('descripcionGeneral', e.target.value)}
              placeholder="Descripción orientada al cliente: qué va a vivir, paisajes, atractivos."
              aria-required="true"
              aria-invalid={!!errores.descripcionGeneral}
            />
          </Campo>

          <Campo id="descripcionTecnica" etiqueta="Descripción técnica" obligatorio error={errores.descripcionTecnica}>
            <textarea
              id="descripcionTecnica"
              className="control"
              rows={3}
              maxLength={1000}
              value={datos.descripcionTecnica}
              onChange={(e) => cambiar('descripcionTecnica', e.target.value)}
              placeholder="Detalle técnico: tipo de terreno, exigencia física, altimetría."
              aria-required="true"
              aria-invalid={!!errores.descripcionTecnica}
            />
          </Campo>

          <Campo id="imagen" etiqueta="Imagen promocional" error={errores.imagen} ayuda="Opcional. Formato JPG o PNG, hasta 5 MB.">
            <div
              className={`zona-imagen ${imagenVisible ? 'zona-imagen--con-imagen' : ''}`}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                elegirImagen(e.dataTransfer.files[0]);
              }}
            >
              {imagenVisible && <img src={imagenVisible} alt="Vista previa de la imagen promocional" />}
              <button id="imagen" type="button" className="zona-imagen__boton" onClick={() => inputImagen.current.click()}>
                <Icono nombre="imagen" tamaño={22} />
                <span>{imagenVisible ? 'Cambiar imagen' : 'Arrastrá una imagen o hacé clic para subir'}</span>
              </button>
              <input
                ref={inputImagen}
                type="file"
                accept="image/png,image/jpeg"
                hidden
                onChange={(e) => {
                  elegirImagen(e.target.files[0]);
                  e.target.value = '';
                }}
              />
            </div>
          </Campo>
        </section>

        {/* ---------------- Datos técnicos y operativos ---------------- */}
        <section className="seccion">
          <h2 className="seccion__titulo">Datos técnicos y operativos</h2>
          <p className="seccion__ayuda">Definen las condiciones bajo las cuales se podrán planificar salidas.</p>

          <div className="fila-campos">
            <Campo id="duracion" etiqueta="Duración estimada" obligatorio bloqueado={bloqueado} error={errores.duracion}>
              <div className="control-sufijo">
                <input
                  id="duracion"
                  className="control"
                  type="number"
                  min={1}
                  max={30}
                  step={1}
                  value={datos.duracion}
                  onChange={(e) => cambiar('duracion', e.target.value)}
                  placeholder="Ej. 3"
                  disabled={bloqueado}
                  aria-required="true"
                  aria-invalid={!!errores.duracion}
                />
                <span>días</span>
              </div>
            </Campo>

            <Campo id="precio" etiqueta="Precio base de referencia" obligatorio bloqueado={bloqueado} error={errores.precio}>
              <div className="control-prefijo">
                <span>$</span>
                <input
                  id="precio"
                  className="control"
                  type="number"
                  min={0}
                  step="0.01"
                  value={datos.precio}
                  onChange={(e) => cambiar('precio', e.target.value)}
                  placeholder="0,00"
                  disabled={bloqueado}
                  aria-required="true"
                  aria-invalid={!!errores.precio}
                />
              </div>
            </Campo>
          </div>

          <Campo id="recorridoBase" etiqueta="Recorrido base" obligatorio bloqueado={bloqueado} error={errores.recorridoBase}>
            <textarea
              id="recorridoBase"
              className="control"
              rows={2}
              maxLength={1000}
              value={datos.recorridoBase}
              onChange={(e) => cambiar('recorridoBase', e.target.value)}
              placeholder="Trayecto de referencia de la excursión, puntos clave del recorrido."
              disabled={bloqueado}
              aria-required="true"
              aria-invalid={!!errores.recorridoBase}
            />
          </Campo>

          <Campo id="dificultad" etiqueta="Nivel de dificultad" obligatorio bloqueado={bloqueado} error={errores.dificultad}>
            <SelectorOpciones
              id="dificultad"
              opciones={OPCIONES_DIFICULTAD}
              valor={datos.dificultad}
              onChange={(v) => cambiar('dificultad', v)}
              deshabilitado={bloqueado}
              invalido={!!errores.dificultad}
            />
          </Campo>

          <div className="fila-campos">
            <Campo id="aceptaMenores" etiqueta="¿Admite menores de edad?" obligatorio bloqueado={bloqueado} error={errores.aceptaMenores}>
              <SelectorOpciones
                id="aceptaMenores"
                opciones={OPCIONES_SI_NO}
                valor={datos.aceptaMenores}
                onChange={(v) => cambiar('aceptaMenores', v)}
                deshabilitado={bloqueado}
                invalido={!!errores.aceptaMenores}
              />
            </Campo>

            <Campo
              id="requiereCertificado"
              etiqueta="¿Requiere certificado médico?"
              obligatorio
              bloqueado={bloqueado}
              error={errores.requiereCertificado}
            >
              <SelectorOpciones
                id="requiereCertificado"
                opciones={OPCIONES_SI_NO}
                valor={datos.requiereCertificado}
                onChange={(v) => cambiar('requiereCertificado', v)}
                deshabilitado={bloqueado}
                invalido={!!errores.requiereCertificado}
              />
            </Campo>
          </div>
        </section>

        {/* ---------------- Equipamiento obligatorio (CU-05) ---------------- */}
        <section className={`seccion ${errores.equipamientoIds ? 'seccion--error' : ''}`} id="seccion-equipamiento" tabIndex={-1}>
          <div className="seccion__cabecera">
            <div>
              <h2 className="seccion__titulo">
                Equipamiento obligatorio
                {bloqueado && <Icono nombre="candado" tamaño={14} className="seccion__candado" />}
              </h2>
              <p className="seccion__ayuda">
                Ítems mínimos que el participante debe presentar; se verifican en el control de asistencia el día de la salida.
              </p>
            </div>
            <Interruptor
              id="requiereEquipamiento"
              etiqueta="Requiere equipamiento obligatorio"
              activo={datos.requiereEquipamiento}
              onChange={alternarEquipamiento}
              deshabilitado={bloqueado}
            />
          </div>

          {datos.requiereEquipamiento ? (
            <>
              <ul className="lista-items">
                {datos.equipamiento.map((item) => (
                  <li key={item.idEquipamiento}>
                    {item.nombre}
                    {!bloqueado && (
                      <button type="button" className="boton-icono boton-icono--peligro" onClick={() => quitarItem(item.idEquipamiento)}>
                        <Icono nombre="cerrar" tamaño={16} />
                        <span className="solo-lector">Quitar {item.nombre}</span>
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              {!bloqueado && (
                <button type="button" className="boton boton--punteado" onClick={() => setModalAbierto(true)}>
                  <Icono nombre="mas" tamaño={16} /> Agregar ítem
                </button>
              )}
              {errores.equipamientoIds && (
                <p className="campo__mensaje campo__mensaje--error" role="alert">
                  {errores.equipamientoIds}
                </p>
              )}
            </>
          ) : (
            <p className="seccion__vacio">La excursión no requiere equipamiento obligatorio.</p>
          )}
        </section>

        <div className="acciones-formulario">
          <Link to="/admin/excursiones" className="boton boton--secundario">
            Cancelar
          </Link>
          <button type="submit" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Registrar excursión'}
          </button>
        </div>
      </form>

      {modalAbierto && (
        <ModalEquipamiento
          catalogo={catalogo}
          idsAsignados={idsAsignados}
          onConfirmar={agregarItems}
          onCerrar={() => setModalAbierto(false)}
        />
      )}
    </section>
  );
}

export default FormularioExcursion;
