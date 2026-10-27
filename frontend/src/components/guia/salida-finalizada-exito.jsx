import { CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
function textoNotificados(n) {
  if (n === 0) return 'No se enviaron solicitudes de valoración.';
  if (n === 1) return 'Se envió 1 solicitud de valoración.';
  return `Se enviaron ${n} solicitudes de valoración.`;
}
export function SalidaFinalizadaExito({ salida, cantidadNotificados, onVolverAMisSalidas }) {
  return (
    <div className="mx-auto flex w-full max-w-[620px] flex-col">
      <Card className="items-center gap-4 rounded-xl px-6 py-12 text-center shadow-none ring-border">
        <CheckCircle2 className="size-16 text-primary" strokeWidth={1.5} aria-hidden="true" />
        <div className="flex flex-col gap-2" role="status">
          <h1 className="font-serif text-3xl font-semibold text-balance text-foreground">Salida finalizada</h1>
          <p className="text-sm text-muted-foreground">{salida.nombreExcursion}</p>
          <p className="text-base text-pretty text-foreground">{textoNotificados(cantidadNotificados)}</p>
        </div>
        <Button
          type="button"
          onClick={onVolverAMisSalidas}
          className="mt-4 h-12 w-full px-6 text-base font-semibold sm:w-auto"
        >
          Volver a mis salidas
        </Button>
      </Card>
    </div>
  );
}
