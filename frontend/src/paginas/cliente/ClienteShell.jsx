import { useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { PublicLayout } from '@/components/layouts/public-layout';
import { obtenerClienteActual } from '@/api/reservas';
import { useSesion } from '@/sesion/SesionProvider';

/**
 * Marco de las pantallas públicas (Visitante) y del Cliente.
 * Sin sesión: botones "Iniciar sesión" y "Registrarme".
 * Con sesión de cliente: "Mis reservas", su nombre y "Cerrar sesión" (CU-03).
 */
export default function ClienteShell() {
  const { usuario, salir } = useSesion();
  const navigate = useNavigate();
  const [cliente, setCliente] = useState(null);

  // Datos completos del cliente (los usa, por ejemplo, "Completar con mis datos" al reservar)
  useEffect(() => {
    if (usuario?.rol !== 'CLIENTE') {
      setCliente(null);
      return;
    }
    obtenerClienteActual()
      .then(setCliente)
      .catch(() => setCliente(null));
  }, [usuario]);

  return (
    <PublicLayout
      usuario={usuario}
      onIniciarSesion={() => navigate('/ingresar')}
      onRegistrarme={() => navigate('/registrarme')}
      onCerrarSesion={async () => {
        // Primero se sale de la página protegida y después se cierra la sesión
        navigate('/', { replace: true });
        await salir();
      }}
    >
      <Outlet context={{ cliente }} />
    </PublicLayout>
  );
}
