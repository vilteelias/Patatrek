import { useState } from 'react';
import { AlertCircle, CalendarX } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { SalidaCard, SalidaCardSkeleton } from './salida-card';
const PESTANIAS = [
  { valor: 'proximas', etiqueta: 'Próximas', estados: ['DISPONIBLE', 'COMPLETA'] },
  { valor: 'en-curso', etiqueta: 'En curso', estados: ['EN_CURSO'] },
  { valor: 'finalizadas', etiqueta: 'Finalizadas', estados: ['FINALIZADA', 'CANCELADA'] },
];
export function MisSalidas({ salidas, cargando, error, onVerParticipantes }) {
  const [pestaniaElegida, setPestaniaElegida] = useState(null);
  const grupos = PESTANIAS.map((p) => ({
    ...p,
    salidas: salidas.filter((s) => p.estados.includes(s.estadoSalida)),
  }));
  const hayEnCurso = grupos.some((g) => g.valor === 'en-curso' && g.salidas.length > 0);
  const pestaniaActiva = pestaniaElegida ?? (hayEnCurso ? 'en-curso' : 'proximas');
  return (
    <section aria-labelledby="titulo-mis-salidas" className="flex flex-col gap-6">
      <header>
        <p className="text-sm text-muted-foreground">Módulo guía</p>
        <h1 id="titulo-mis-salidas" className="mt-1 font-serif text-3xl font-semibold text-foreground sm:text-4xl">
          Mis salidas
        </h1>
        <p className="mt-2 text-pretty text-muted-foreground">
          Solo se muestran las salidas en las que estás asignado como guía responsable o ayudante.
        </p>
      </header>

      {error && (
        <Alert variant="destructive" className="border-destructive/30">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>No pudimos cargar tus salidas</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {cargando ? (
        <div className="flex flex-col gap-4" aria-busy="true" aria-live="polite">
          <span className="sr-only">Cargando salidas…</span>
          <SalidaCardSkeleton />
          <SalidaCardSkeleton />
          <SalidaCardSkeleton />
        </div>
      ) : (
        !error && (
          <Tabs value={pestaniaActiva} onValueChange={(valor) => setPestaniaElegida(valor)} className="gap-5">
            <TabsList className="h-auto! w-full bg-secondary/70 p-1 sm:w-fit">
              {grupos.map((g) => (
                <TabsTrigger key={g.valor} value={g.valor} className="h-9 gap-2 px-2 text-sm sm:px-4">
                  {g.etiqueta}
                  <span className="rounded-full bg-muted px-1.5 py-0.5 text-xs font-semibold text-muted-foreground tabular-nums">
                    {g.salidas.length}
                  </span>
                </TabsTrigger>
              ))}
            </TabsList>

            {grupos.map((g) => (
              <TabsContent key={g.valor} value={g.valor}>
                {g.salidas.length === 0 ? (
                  <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-border bg-card px-6 py-14 text-center">
                    <CalendarX className="size-8 text-muted-foreground" aria-hidden="true" />
                    <p className="text-muted-foreground">No tenés salidas en esta categoría.</p>
                  </div>
                ) : (
                  <ul className="flex flex-col gap-4">
                    {g.salidas.map((salida) => (
                      <li key={salida.idSalida}>
                        <SalidaCard salida={salida} onVerParticipantes={onVerParticipantes} />
                      </li>
                    ))}
                  </ul>
                )}
              </TabsContent>
            ))}
          </Tabs>
        )
      )}
    </section>
  );
}
