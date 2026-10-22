import { formatGuias, formatRangoFechas, ROL_LABEL } from "../../lib/salidas/format"
import type { SalidaDTO } from "../../lib/salidas/types"
import { StatusBadge } from "./status-badge"

interface SalidaCardProps {
  salida: SalidaDTO
  onVerParticipantes: (salida: SalidaDTO) => void
}

function DetailItem({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm font-semibold text-foreground">{value}</dd>
    </div>
  )
}

export function SalidaCard({ salida, onVerParticipantes }: SalidaCardProps) {
  const finalizada = salida.estado === "FINALIZADA"
  const refugios = salida.refugios.length > 0 ? salida.refugios.map((r) => r.nombre).join(", ") : "—"

  return (
    <article
      aria-labelledby={`salida-${salida.id}-titulo`}
      className={`rounded-2xl border border-border p-4 md:p-5 ${finalizada ? "bg-card/50" : "bg-card"}`}
    >
      <div className="flex items-start justify-between gap-4">
        <div className={finalizada ? "opacity-60" : undefined}>
          <h2 id={`salida-${salida.id}-titulo`} className="text-base font-semibold text-foreground">
            {salida.nombre}
          </h2>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {formatRangoFechas(salida.fechaInicio, salida.fechaFin)} · Rol: {ROL_LABEL[salida.rol]}
          </p>
        </div>
        <StatusBadge estado={salida.estado} />
      </div>

      {!finalizada && (
        <dl className="mt-3 flex flex-wrap gap-x-8 gap-y-3 border-t border-border pt-3">
          <DetailItem label="Cupo máximo" value={salida.cupoMaximo} />
          <DetailItem label="Guías ayudantes" value={formatGuias(salida.guiasAyudantes)} />
          <DetailItem label="Refugio/s" value={refugios} />
        </dl>
      )}

      <div className="mt-4 flex justify-end">
        <button
          type="button"
          onClick={() => onVerParticipantes(salida)}
          className={
            finalizada
              ? "rounded-lg border border-border bg-card px-4 py-2 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
              : "rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
          }
        >
          Ver detalle
        </button>
      </div>
    </article>
  )
}
