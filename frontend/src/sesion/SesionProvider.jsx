import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { cerrarSesion, iniciarSesion, obtenerSesion, registrarCliente } from '@/api/auth';

const SesionContexto = createContext(null);

/** Página de inicio de cada rol después de iniciar sesión. */
export const INICIO_POR_ROL = {
  ADMINISTRADOR: '/admin/excursiones',
  GUIA: '/guia/salidas',
  CLIENTE: '/',
};

/**
 * Guarda quién inició sesión (lo que informa el backend) y ofrece login, registro y logout.
 * cargando = true hasta que se conoce la sesión (para no redirigir antes de tiempo).
 */
export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  const recargar = useCallback(
    () =>
      obtenerSesion()
        .then((s) => setUsuario(s.usuario ?? null))
        .catch(() => setUsuario(null))
        .finally(() => setCargando(false)),
    [],
  );

  useEffect(() => {
    recargar();
  }, [recargar]);

  const valor = useMemo(
    () => ({
      usuario,
      cargando,
      recargar,
      async ingresar(correo, contrasenia) {
        const s = await iniciarSesion(correo, contrasenia);
        setUsuario(s.usuario);
        return s.usuario;
      },
      async registrarse(datos) {
        const s = await registrarCliente(datos);
        setUsuario(s.usuario);
        return s.usuario;
      },
      async salir() {
        try {
          await cerrarSesion();
        } finally {
          setUsuario(null);
        }
      },
    }),
    [usuario, cargando, recargar],
  );

  return <SesionContexto.Provider value={valor}>{children}</SesionContexto.Provider>;
}

export function useSesion() {
  return useContext(SesionContexto);
}
