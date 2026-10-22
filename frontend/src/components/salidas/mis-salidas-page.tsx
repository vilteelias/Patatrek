"use client"

import { useEffect, useState } from "react"
import { getSalidasDelGuia } from "../../lib/salidas/api"
import { MOCK_USUARIO } from "../../lib/salidas/mock-data"
import type { SalidaDTO, UsuarioSesionDTO } from "../../lib/salidas/types"
import { AppHeader } from "./app-header"
import { SalidaCard } from "./salida-card"

function SalidaCardSkeleton() {
  return (
    <div className="h-36 animate-pulse rounded-2xl border border-border bg-card" aria-hidden="true" />
  )
}

export function MisSalidasPage() {
  const [usuario] = useState<UsuarioSesionDTO>(MOCK_USUARIO)
  const [salidas, setSalidas] = useState<SalidaDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    getSalidasDelGuia(usuario.id)
      .then((data) => {
        if (!cancelled) setSalidas(data)
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [usuario.id])

  const handleVerParticipantes = (salida: SalidaDTO) => {
    // Hook up navigation here, e.g. navigate(`/guia/salidas/${salida.id}/participantes`)
    window.alert(`Participantes de: ${salida.nombre}`)
  }

  return (
    <div className="min-h-screen bg-background">
      <AppHeader usuario={usuario} />

      <main className="mx-auto max-w-[600px] px-4 py-8 md:py-10">
        <p className="text-xs text-muted-foreground">Módulo guía</p>
        <h1 className="mt-1 font-serif text-3xl font-semibold text-foreground text-balance">Mis salidas</h1>
        <p className="mt-2 text-sm text-muted-foreground text-pretty">
          Solo se muestran las salidas en las que estás asignado como guía responsable o ayudante.
        </p>

        <section aria-label="Salidas asignadas" aria-busy={loading} className="mt-5 flex flex-col gap-3">
          {loading && (
            <>
              <SalidaCardSkeleton />
              <SalidaCardSkeleton />
              <SalidaCardSkeleton />
            </>
          )}

          {!loading && error && (
            <p role="alert" className="rounded-2xl border border-border bg-card p-5 text-sm text-destructive">
              No pudimos cargar tus salidas. {error}
            </p>
          )}

          {!loading && !error && salidas.length === 0 && (
            <p className="rounded-2xl border border-border bg-card p-5 text-sm text-muted-foreground">
              Todavía no tenés salidas asignadas.
            </p>
          )}

          {!loading &&
            !error &&
            salidas.map((salida) => (
              <SalidaCard key={salida.id} salida={salida} onVerParticipantes={handleVerParticipantes} />
            ))}
        </section>
      </main>
    </div>
  )
}
