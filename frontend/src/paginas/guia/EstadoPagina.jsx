import { Link } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Loader2 } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

// Pantalla de "cargando" o de error mientras no hay datos de la salida.
export function CargandoSalida() {
  return (
    <p className="flex items-center gap-2 py-12 text-muted-foreground">
      <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando salida…
    </p>
  );
}

export function AvisoSalida({ titulo, mensaje, volverA = '/guia/salidas', textoVolver = 'Mis salidas' }) {
  return (
    <div className="flex flex-col gap-4">
      <Link
        to={volverA}
        className="inline-flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        {textoVolver}
      </Link>
      <Alert variant="destructive" className="rounded-xl">
        <AlertCircle aria-hidden="true" />
        <AlertTitle>{titulo}</AlertTitle>
        <AlertDescription>{mensaje}</AlertDescription>
      </Alert>
    </div>
  );
}
