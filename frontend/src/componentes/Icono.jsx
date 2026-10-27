// Íconos de línea simples (estilo del prototipo). Uso: <Icono nombre="montaña" />
const TRAZOS = {
  montaña: <path d="m3 20 6.5-11 4 6.5L16 12l5 8H3Z" />,
  calendario: (
    <>
      <rect x="3.5" y="5" width="17" height="15" rx="2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </>
  ),
  reserva: (
    <>
      <path d="M4 6h16v4a2 2 0 0 0 0 4v4H4v-4a2 2 0 0 0 0-4V6Z" />
      <path d="M12 6v12" strokeDasharray="2 2.5" />
    </>
  ),
  certificado: (
    <>
      <path d="M6 3h9l4 4v14H6V3Z" />
      <path d="m9.5 13.5 2 2 4-4.5" />
    </>
  ),
  caja: (
    <>
      <path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z" />
      <path d="m4 7.5 8 4.5 8-4.5M12 12v9" />
    </>
  ),
  estrella: <path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9L12 3.5Z" />,
  devolucion: (
    <>
      <path d="M4 12a8 8 0 0 1 14-5.3M20 12a8 8 0 0 1-14 5.3" />
      <path d="M18 3v4h-4M6 21v-4h4" />
    </>
  ),
  mas: <path d="M12 5v14M5 12h14" />,
  cerrar: <path d="M6 6l12 12M18 6 6 18" />,
  candado: (
    <>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </>
  ),
  imagen: (
    <>
      <rect x="3.5" y="4.5" width="17" height="15" rx="2" />
      <circle cx="9" cy="10" r="1.6" />
      <path d="m20.5 16-5-5-9 8.5" />
    </>
  ),
  alerta: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5v5.5M12 16.5v.01" />
    </>
  ),
  check: <path d="m5 12.5 4.5 4.5L19 7.5" />,
  buscar: (
    <>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.2-4.2" />
    </>
  ),
  lapiz: <path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4Z" />,
  archivar: (
    <>
      <rect x="3.5" y="4" width="17" height="5" rx="1" />
      <path d="M5 9v10h14V9M10 13h4" />
    </>
  ),
};

function Icono({ nombre, tamaño = 18, className = '' }) {
  return (
    <svg
      className={`icono ${className}`}
      width={tamaño}
      height={tamaño}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {TRAZOS[nombre]}
    </svg>
  );
}

export default Icono;
