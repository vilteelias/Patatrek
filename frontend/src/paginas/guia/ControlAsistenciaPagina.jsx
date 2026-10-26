import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ControlAsistencia } from '@/components/guia/control-asistencia';
import { registrarControlAsistencia } from '@/api/guia';
import { useSalidaConParticipantes } from './useSalidaConParticipantes';
import { AvisoSalida, CargandoSalida } from './EstadoPagina';

const ESTADOS_PARA_CONTROL = ['DISPONIBLE', 'COMPLETA'];

// CU-15 Control de asistencia, equipamiento y certificados de participantes.
export default function ControlAsistenciaPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { salida, participantes, error } = useSalidaConParticipantes(id);
  const [enviando, setEnviando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState(null);

  if (!salida) {
    return error ? <AvisoSalida titulo="No pudimos abrir la salida" mensaje={error} /> : <CargandoSalida />;
  }

  const volverANomina = `/guia/salidas/${id}`;

  if (!ESTADOS_PARA_CONTROL.includes(salida.estadoSalida)) {
    return (
      <AvisoSalida
        titulo="El control de asistencia ya se realizó"
        mensaje="El control solo se hace antes de partir, cuando la salida está Disponible o Completa."
        volverA={volverANomina}
        textoVolver={salida.nombreExcursion}
      />
    );
  }

  async function finalizarControl(registros) {
    setErrorGeneral(null);
    setEnviando(true);
    try {
      await registrarControlAsistencia(id, registros);
      navigate(volverANomina, {
        state: { aviso: 'Control de asistencia registrado. La salida pasó a estado "En curso".' },
      });
    } catch (e) {
      setErrorGeneral(e.message);
      setEnviando(false);
    }
  }

  return (
    <ControlAsistencia
      salida={salida}
      participantes={participantes}
      enviando={enviando}
      errorGeneral={errorGeneral}
      onVolver={() => navigate(volverANomina)}
      onFinalizarControl={finalizarControl}
    />
  );
}
