import { useEffect, useState } from 'react';
import { listarParticipantesSalida, obtenerSalidaGuia } from '@/api/guia';

// Carga juntos los datos de la salida y su nómina de participantes.
export function useSalidaConParticipantes(idSalida) {
  const [salida, setSalida] = useState(null);
  const [participantes, setParticipantes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError(null);
    Promise.all([obtenerSalidaGuia(idSalida), listarParticipantesSalida(idSalida)])
      .then(([s, p]) => {
        if (!vigente) return;
        setSalida(s);
        setParticipantes(p);
      })
      .catch((e) => vigente && setError(e.message))
      .finally(() => vigente && setCargando(false));
    return () => {
      vigente = false;
    };
  }, [idSalida]);

  return { salida, participantes, cargando, error };
}
