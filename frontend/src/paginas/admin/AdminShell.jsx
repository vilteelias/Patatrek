import { Outlet, useNavigate } from 'react-router-dom';
import { AdminLayout } from '@/components/layouts/admin-layout';
import { RequiereRol } from '@/sesion/RequiereRol';
import { useSesion } from '@/sesion/SesionProvider';

/** Marco del Administrador: exige sesión con rol Administrador y muestra quién está conectado. */
export default function AdminShell({ modulosDisponibles }) {
  const { usuario, salir } = useSesion();
  const navigate = useNavigate();
  return (
    <RequiereRol roles={['ADMINISTRADOR']}>
      <AdminLayout
        modulosDisponibles={modulosDisponibles}
        usuario={usuario}
        onCerrarSesion={async () => {
          navigate('/', { replace: true });
          await salir(); // CU-03
        }}
      >
        <Outlet />
      </AdminLayout>
    </RequiereRol>
  );
}
