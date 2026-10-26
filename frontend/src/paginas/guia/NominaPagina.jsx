import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { NominaSalida } from '@/components/guia/nomina-salida';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useSalidaConParticipantes } from './useSalidaConParticipantes';
import { AvisoSalida, CargandoSalida } from './EstadoPagina';

// CU-22 Listar participantes de una salida (nómina).
export default function NominaPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { salida, participantes, cargando, error } = useSalidaConParticipantes(id);

  // Aviso que llega después del control de asistencia
  const [aviso] = useState(location.state?.aviso ?? null);
  useEffect(() => {
    if (location.state?.aviso) navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!salida) {
    return error ? <AvisoSalida titulo="No pudimos abrir la salida" mensaje={error} /> : <CargandoSalida />;
  }

  return (
    <div className="flex flex-col gap-6">
      {aviso && (
        <Alert className="rounded-xl border-primary/25 bg-accent text-accent-foreground">
          <CheckCircle2 aria-hidden="true" />
          <AlertDescription className="text-accent-foreground">{aviso}</AlertDescription>
        </Alert>
      )}
      <NominaSalida
        salida={salida}
        participantes={participantes}
        cargando={cargando}
        error={error}
        onVolver={() => navigate('/guia/salidas')}
        onControlAsistencia={() => navigate(`/guia/salidas/${id}/control`)}
        onMarcarFinalizacion={() => navigate(`/guia/salidas/${id}/finalizar`)}
      />
    </div>
  );
}
