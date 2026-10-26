import { DIFICULTAD, dias, pesos } from '../utilidades/formato.js';

// Degradés de respaldo cuando la excursión todavía no tiene imagen (como en el prototipo)
const FONDOS = [
  'linear-gradient(135deg, #2c6a54, #1d4a3c)',
  'linear-gradient(135deg, #3d7a64, #163a2f)',
  'linear-gradient(135deg, #c77446, #8f3f22)',
  'linear-gradient(135deg, #7d8a5a, #444f2f)',
];

// Recibe una excursión por "props" y la muestra
function TarjetaExcursion({ excursion }) {
  const fondo = FONDOS[excursion.idExcursion % FONDOS.length];

  return (
    <article className="tarjeta">
      <div className="tarjeta__imagen" style={{ background: fondo }}>
        {excursion.imagenPromocional && (
          <img src={excursion.imagenPromocional} alt={`Imagen de ${excursion.nombre}`} loading="lazy" />
        )}
      </div>

      <div className="tarjeta__cuerpo">
        <div className="tarjeta__titulo">
          <h2>{excursion.nombre}</h2>
          <span className={`insignia insignia--${excursion.dificultad.toLowerCase()}`}>
            {DIFICULTAD[excursion.dificultad]}
          </span>
        </div>

        <p className="tarjeta__descripcion">{excursion.descripcionGeneral}</p>

        <ul className="etiquetas">
          <li>{dias(excursion.duracion)}</li>
          {excursion.requiereCertificado && <li>Certificado médico requerido</li>}
          {!excursion.aceptaMenores && <li>Solo mayores de 18</li>}
        </ul>

        <p className="tarjeta__precio">
          {pesos(excursion.precio)} <span>por persona</span>
        </p>
      </div>
    </article>
  );
}

export default TarjetaExcursion;
