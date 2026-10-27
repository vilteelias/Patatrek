import { useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { GuiaLayout } from '@/components/guia/guia-layout';
import { obtenerGuiaActual } from '@/api/guia';

// Marco de todas las pantallas del guía: carga quién es el guía y muestra el encabezado.
export default function GuiaShell() {
  const navigate = useNavigate();
  const [guia, setGuia] = useState({ nombre: '', apellido: '' });

  useEffect(() => {
    obtenerGuiaActual()
      .then(setGuia)
      .catch(() => setGuia({ nombre: 'Guía', apellido: 'PataTrek' }));
  }, []);

  return (
    // "Cerrar sesión" vuelve al inicio hasta que esté el módulo de Autenticación
    <GuiaLayout guia={guia} onCerrarSesion={() => navigate('/')}>
      <Outlet />
    </GuiaLayout>
  );
}
