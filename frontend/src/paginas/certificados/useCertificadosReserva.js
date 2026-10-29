import { useCallback, useEffect, useState } from 'react';
import { obtenerCertificadosReserva } from '@/api/certificados';

// Carga los certificados de una reserva. Con dni (participante) trae solo el suyo.
// habilitado=false evita la consulta (por ejemplo, mientras el participante todavía no ingresó su DNI).
export function useCertificadosReserva(idReserva, dni, habilitado = true) {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(habilitado);
  const [error, setError] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!habilitado) return undefined;
    let vigente = true;
    setError(null);
    obtenerCertificadosReserva(idReserva, dni)
      .then((r) => vigente && setDatos(r))
      .catch((e) => vigente && setError(e.message))
      .finally(() => vigente && setCargando(false));
    return () => {
      vigente = false;
    };
  }, [idReserva, dni, habilitado, version]);

  // Vuelve a consultar sin mostrar el esqueleto de carga (se usa tras subir un archivo)
  const recargar = useCallback(() => setVersion((v) => v + 1), []);

  return { datos, cargando, error, recargar };
}
