// Layout del Administrador. Generado con v0 y adaptado a React + Vite (react-router-dom).
import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { CalendarDays, FileCheck, Menu, Mountain, Package, RefreshCcw, Star, Ticket, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import logo from '@/assets/logo.png';

const ITEMS_MENU = [
  { etiqueta: 'Excursiones', ruta: '/admin/excursiones', icono: Mountain },
  { etiqueta: 'Salidas', ruta: '/admin/salidas', icono: CalendarDays },
  { etiqueta: 'Reservas', ruta: '/admin/reservas', icono: Ticket },
  { etiqueta: 'Certificados médicos', ruta: '/admin/certificados', icono: FileCheck },
  { etiqueta: 'Equipamiento', ruta: '/admin/equipamiento', icono: Package },
  { etiqueta: 'Valoraciones', ruta: '/admin/valoraciones', icono: Star },
  { etiqueta: 'Devoluciones', ruta: '/admin/devoluciones', icono: RefreshCcw },
];

/**
 * modulosDisponibles: rutas de los módulos ya implementados (los demás se ven atenuados con "Próximamente").
 * children: contenido de la página (en App.jsx se pasa <Outlet />).
 */
export function AdminLayout({ modulosDisponibles, children }) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const { pathname } = useLocation();

  // Al navegar se cierra el menú del celular
  useEffect(() => setMenuAbierto(false), [pathname]);

  useEffect(() => {
    if (!menuAbierto) return;
    const cerrarConEscape = (e) => e.key === 'Escape' && setMenuAbierto(false);
    document.addEventListener('keydown', cerrarConEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', cerrarConEscape);
      document.body.style.overflow = '';
    };
  }, [menuAbierto]);

  return (
    <div className="min-h-dvh bg-background">
      {/* Barra superior en celular */}
      <header className="sticky top-0 z-30 flex items-center justify-between bg-sidebar px-4 py-3 text-sidebar-foreground lg:hidden">
        <Link to="/admin/excursiones" className="flex items-center gap-3">
          <img src={logo} alt="PataTrek" className="size-10 rounded-full" />
          <span className="font-serif text-lg font-semibold text-sidebar-accent-foreground">PataTrek</span>
        </Link>
        <button
          type="button"
          onClick={() => setMenuAbierto(true)}
          className="inline-flex size-10 items-center justify-center rounded-lg hover:bg-sidebar-accent focus-visible:outline-2 focus-visible:outline-sidebar-accent-foreground"
          aria-label="Abrir menú"
          aria-expanded={menuAbierto}
          aria-controls="menu-admin-movil"
        >
          <Menu className="size-6" aria-hidden="true" />
        </button>
      </header>

      {/* Menú lateral fijo en escritorio */}
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-[250px] lg:block">
        <PanelMenu modulosDisponibles={modulosDisponibles} />
      </aside>

      {/* Panel lateral en celular */}
      {menuAbierto && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-foreground/40"
            aria-label="Cerrar menú"
            onClick={() => setMenuAbierto(false)}
          />
          <div
            id="menu-admin-movil"
            role="dialog"
            aria-modal="true"
            aria-label="Menú de administración"
            className="absolute inset-y-0 left-0 w-[min(280px,85vw)] animate-in slide-in-from-left duration-200"
          >
            <PanelMenu modulosDisponibles={modulosDisponibles} onCerrar={() => setMenuAbierto(false)} />
          </div>
        </div>
      )}

      <main className="min-w-0 px-4 py-6 sm:px-8 sm:py-10 lg:ml-[250px] lg:px-12">{children}</main>
    </div>
  );
}

function PanelMenu({ modulosDisponibles, onCerrar }) {
  return (
    <div className="relative flex h-full flex-col bg-sidebar text-sidebar-foreground">
      {onCerrar && (
        <button
          type="button"
          onClick={onCerrar}
          className="absolute top-3 right-3 inline-flex size-9 items-center justify-center rounded-lg hover:bg-sidebar-accent"
          aria-label="Cerrar menú"
        >
          <X className="size-5" aria-hidden="true" />
        </button>
      )}

      <div className="flex justify-center px-6 pt-8 pb-6">
        <Link to="/admin/excursiones" aria-label="PataTrek, ir al inicio">
          <img src={logo} alt="PataTrek" className="size-[72px] rounded-full" />
        </Link>
      </div>

      <nav aria-label="Menú principal" className="flex-1 overflow-y-auto border-t border-sidebar-border px-3 pt-4">
        <ul className="flex flex-col gap-1">
          {ITEMS_MENU.map((item) => (
            <li key={item.ruta}>
              <ItemNavegacion item={item} disponible={modulosDisponibles.includes(item.ruta)} onNavegar={onCerrar} />
            </li>
          ))}
        </ul>
      </nav>

      <div className="flex items-center gap-3 border-t border-sidebar-border px-5 py-5">
        <div
          className="flex size-10 shrink-0 items-center justify-center rounded-full bg-warning text-sm font-semibold text-white"
          aria-hidden="true"
        >
          AD
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-sidebar-accent-foreground">Administrador</p>
          <p className="truncate text-xs text-sidebar-foreground/80">admin@patatrek.com</p>
        </div>
      </div>
    </div>
  );
}

function ItemNavegacion({ item, disponible, onNavegar }) {
  const Icono = item.icono;
  const claseBase = 'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors';

  if (!disponible) {
    return (
      <span className={cn(claseBase, 'group relative cursor-not-allowed opacity-45')} aria-disabled="true" title="Próximamente">
        <Icono className="size-[18px] shrink-0" aria-hidden="true" />
        <span>{item.etiqueta}</span>
        <span className="sr-only">(Próximamente)</span>
      </span>
    );
  }

  // NavLink marca como activo también las subrutas (ej. /admin/excursiones/nueva)
  return (
    <NavLink
      to={item.ruta}
      onClick={onNavegar}
      className={({ isActive }) =>
        cn(
          claseBase,
          'focus-visible:outline-2 focus-visible:outline-sidebar-accent-foreground',
          isActive
            ? 'bg-sidebar-accent font-semibold text-sidebar-accent-foreground'
            : 'hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground',
        )
      }
    >
      <Icono className="size-[18px] shrink-0" aria-hidden="true" />
      <span>{item.etiqueta}</span>
    </NavLink>
  );
}
