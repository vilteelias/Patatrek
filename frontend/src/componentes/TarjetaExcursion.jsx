const DIFICULTAD = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta' };

const pesos = new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0,
});

// Recibe una excursión por "props" y la muestra
function TarjetaExcursion({ excursion }) {
    return (
        <article className="tarjeta">
            <h2>{excursion.nombre}</h2>
            <p>{excursion.descripcionGeneral}</p>

            <ul className="etiquetas">
                <li>Dificultad: {DIFICULTAD[excursion.dificultad]}</li>
                <li>{excursion.duracion} {excursion.duracion === 1 ? 'día' : 'días'}</li>
                {!excursion.aceptaMenores && <li>Solo mayores de 18</li>}
                {excursion.requiereCertificado && <li>Requiere certificado médico</li>}
            </ul>

            <p className="precio">{pesos.format(excursion.precio)} por persona</p>
        </article>
    );
}

export default TarjetaExcursion;