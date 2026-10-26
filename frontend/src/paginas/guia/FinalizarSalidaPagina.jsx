import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FinalizarSalida } from '@/components/guia/finalizar-salida';
import { SalidaFinalizadaExito } from '@/components/guia/salida-finalizada-exito';
import { marcarFinalizacionSalida } from '@/api/guia';
import { useSalidaConParticipantes } from './useSalidaConParticipantes';
import { AvisoSalida, CargandoSalida } from './EstadoPagina';

// CU-16 Marcar finalización de salida (solo guía responsable, salida En curso).
export default function FinalizarSalidaPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { salida, participantes, error } = useSalidaConParticipantes(id);
  const [enviando, setEnviando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [resultado, setResultado] = useState(null); // { cantidadNotificados, salida }

  if (resultado) {
    return (
      <SalidaFinalizadaExito
        salida={resultado.salida}
        cantidadNotificados={resultado.cantidadNotificados}
        onVolverAMisSalidas={() => navigate('/guia/salidas')}
      />
    );
  }

  if (!salida) {
    return error ? <AvisoSalida titulo="No pudimos abrir la salida" mensaje={error} /> : <CargandoSalida />;
  }

  const volverANomina = `/guia/salidas/${id}`;

  if (salida.rolGuia !== 'RESPONSABLE') {
    return (
      <AvisoSalida
        titulo="No podés finalizar esta salida"
        mensaje="Solo el guía responsable puede marcar la finalización de la salida."
        volverA={volverANomina}
        textoVolver={salida.nombreExcursion}
      />
    );
  }
  if (salida.estadoSalida !== 'EN_CURSO') {
    return (
      <AvisoSalida
        titulo="La salida no está En curso"
        mensaje="Solo se puede finalizar una salida que esté En curso (después del control de asistencia)."
        volverA={volverANomina}
        textoVolver={salida.nombreExcursion}
      />
    );
  }

  async function confirmar(observaciones) {
    setErrorGeneral(null);
    setEnviando(true);
    try {
      setResultado(await marcarFinalizacionSalida(id, observaciones));
    } catch (e) {
      setErrorGeneral(e.message);
      setEnviando(false);
    }
  }

  return (
    <FinalizarSalida
      salida={salida}
      participantes={participantes}
      enviando={enviando}
      errorGeneral={errorGeneral}
      onVolver={() => navigate(volverANomina)}
      onConfirmar={confirmar}
    />
  );
}
