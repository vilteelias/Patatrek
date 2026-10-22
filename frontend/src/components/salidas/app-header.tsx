import { getIniciales } from "../../lib/salidas/format"
import type { UsuarioSesionDTO } from "../../lib/salidas/types"

const ROL_USUARIO_LABEL: Record<UsuarioSesionDTO["rol"], string> = {
  GUIA: "Guía",
  ADMIN: "Administrador",
  PARTICIPANTE: "Participante",
}

export function AppHeader({ usuario }: { usuario: UsuarioSesionDTO }) {
  return (
    <header className="border-b border-border bg-card">
      <div className="flex h-16 items-center justify-between px-4 md:px-6">
        <a href="/" className="flex items-center" aria-label="Patatrek, inicio">
          <img
            src="/assets/patatrek-logo.png"
            alt="Patatrek"
            width={40}
            height={40}
            className="size-10 rounded-full object-cover"
          />
        </a>

        <div className="flex items-center gap-2.5">
          <span
            aria-hidden="true"
            className="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground"
          >
            {getIniciales(usuario.nombre, usuario.apellido)}
          </span>
          <div className="leading-tight">
            <p className="text-sm font-semibold text-foreground">
              {usuario.apellido}, {usuario.nombre}
            </p>
            <p className="text-xs text-muted-foreground">{ROL_USUARIO_LABEL[usuario.rol]}</p>
          </div>
        </div>
      </div>
    </header>
  )
}
