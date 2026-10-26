import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MisSalidas } from '@/components/guia/mis-salidas';
import { listarMisSalidas } from '@/api/guia';

// CU-10 Listar salidas (vista del guía): solo las salidas donde está asignado.
export default function MisSalidasPagina() {
  const navigate = useNavigate();
  const [salidas, setSalidas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    listarMisSalidas()
      .then(setSalidas)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, []);

  return (
    <MisSalidas
      salidas={salidas}
      cargando={cargando}
      error={error}
      onVerParticipantes={(idSalida) => navigate(`/guia/salidas/${idSalida}`)}
    />
  );
}
