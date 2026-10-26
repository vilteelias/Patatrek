function Proximamente({ titulo = 'Próximamente', texto = 'Esta sección se implementa en un próximo módulo.' }) {
  return (
    <div className="estado-vacio">
      <h1 className="titulo">{titulo}</h1>
      <p>{texto}</p>
    </div>
  );
}

export default Proximamente;
