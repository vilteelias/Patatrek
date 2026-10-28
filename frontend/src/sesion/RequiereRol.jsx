import { Navigate, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { INICIO_POR_ROL, useSesion } from './SesionProvider';

/**
 * Muestra el contenido solo si el usuario inició sesión con alguno de los roles indicados.
 * - Sin sesión → va a "Iniciar sesión" y, al ingresar, vuelve a la página que quería ver.
 * - Con otro rol → va a la página de inicio de su rol.
 */
export function RequiereRol({ roles, children }) {
  const { usuario, cargando } = useSesion();
  const location = useLocation();

  if (cargando) {
    return (
      <p className="flex items-center justify-center gap-2 py-16 text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Verificando sesión…
      </p>
    );
  }
  if (!usuario) {
    const volver = location.pathname + location.search;
    return <Navigate to={`/ingresar?volver=${encodeURIComponent(volver)}`} replace />;
  }
  if (!roles.includes(usuario.rol)) {
    return <Navigate to={INICIO_POR_ROL[usuario.rol] ?? '/'} replace />;
  }
  return children;
}
