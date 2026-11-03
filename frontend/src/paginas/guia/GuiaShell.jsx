import { useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { GuiaLayout } from '@/components/guia/guia-layout';
import { obtenerGuiaActual } from '@/api/guia';
import { RequiereRol } from '@/sesion/RequiereRol';
import { useSesion } from '@/sesion/SesionProvider';

// Marco de todas las pantallas del guía: exige sesión con rol Guía, carga quién es y muestra el encabezado.
export default function GuiaShell() {
  return (
    <RequiereRol roles={['GUIA']}>
      <PanelGuia />
    </RequiereRol>
  );
}

function PanelGuia() {
  const navigate = useNavigate();
  const { usuario, salir } = useSesion();
  const [guia, setGuia] = useState({ nombre: usuario?.nombre ?? '', apellido: usuario?.apellido ?? '' });

  useEffect(() => {
    obtenerGuiaActual()
      .then(setGuia)
      .catch(() => {});
  }, [usuario?.id]);

  return (
    <GuiaLayout
      guia={guia}
      onCerrarSesion={async () => {
        navigate('/', { replace: true });
        await salir(); // CU-03
      }}
    >
      <Outlet />
    </GuiaLayout>
  );
}
