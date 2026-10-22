import type { EstadoSalida } from "../../lib/salidas/types"

const ESTADO_CONFIG: Record<EstadoSalida, { label: string; className: string }> = {
  DISPONIBLE: { label: "Disponible", className: "bg-status-available text-status-available-foreground" },
  EN_CURSO: { label: "En Curso", className: "bg-status-progress text-status-progress-foreground" },
  FINALIZADA: { label: "Finalizada", className: "bg-status-finished text-status-finished-foreground" },
}

export function StatusBadge({ estado }: { estado: EstadoSalida }) {
  const { label, className } = ESTADO_CONFIG[estado]
  return (
    <span className={`inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${className}`}>
      {label}
    </span>
  )
}
