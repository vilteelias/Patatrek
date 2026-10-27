import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { PublicLayout } from '@/components/layouts/public-layout';
import { obtenerClienteActual } from '@/api/reservas';

/**
 * Marco de las pantallas públicas y del Cliente.
 * TEMPORAL (hasta el módulo de Autenticación): se muestra la clienta de demostración que informa el backend.
 * Si el backend no la tiene, se ve como Visitante (botones Iniciar sesión / Registrarme).
 */
export default function ClienteShell() {
  const [cliente, setCliente] = useState(null);

  useEffect(() => {
    obtenerClienteActual()
      .then(setCliente)
      .catch(() => setCliente(null));
  }, []);

  return (
    <PublicLayout usuario={cliente}>
      <Outlet context={{ cliente }} />
    </PublicLayout>
  );
}
