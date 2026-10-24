export async function listarExcursiones() {
    const respuesta = await fetch('/api/excursiones');

    if (!respuesta.ok) {
        throw new Error('No se pudieron cargar las excursiones.');
    }
    return respuesta.json();
}