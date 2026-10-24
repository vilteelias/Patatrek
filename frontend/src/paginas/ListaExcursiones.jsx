import { useEffect, useState } from 'react';
import { listarExcursiones } from '../api/excursiones';
import TarjetaExcursion from '../componentes/TarjetaExcursion';

function ListaExcursiones() {
    // (1) Estado: los datos que pueden cambiar
    const [excursiones, setExcursiones] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState(null);

    // (2) Efecto: se ejecuta una vez cuando el componente aparece
    useEffect(() => {
        listarExcursiones()
            .then((datos) => setExcursiones(datos))
            .catch((err) => setError(err.message))
            .finally(() => setCargando(false));
    }, []);

    // (3) Qué mostrar según el estado
    if (cargando) return <p>Cargando excursiones...</p>;
    if (error) return <p className="error">{error}</p>;
    if (excursiones.length === 0) return <p>Todavía no hay excursiones registradas.</p>;

    return (
        <section>
            <h1>Excursiones</h1>
            <div className="grilla">
                {excursiones.map((exc) => (                           // (4)
                    <TarjetaExcursion key={exc.idExcursion} excursion={exc} />
                ))}
            </div>
        </section>
    );
}

export default ListaExcursiones;