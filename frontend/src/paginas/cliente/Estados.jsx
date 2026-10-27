import { Link } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Loader2 } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

export function Cargando({ texto = 'Cargando…' }) {
  return (
    <p className="flex items-center gap-2 py-12 text-muted-foreground">
      <Loader2 className="size-4 animate-spin" aria-hidden="true" /> {texto}
    </p>
  );
}

export function Volver({ to, children }) {
  return (
    <Link to={to} className="inline-flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
      <ArrowLeft className="size-4" aria-hidden="true" />
      {children}
    </Link>
  );
}

export function Aviso({ titulo, mensaje, volverA = '/', textoVolver = 'Volver a las excursiones' }) {
  return (
    <div className="mx-auto flex max-w-[660px] flex-col gap-4">
      <Volver to={volverA}>{textoVolver}</Volver>
      <Alert variant="destructive" className="rounded-xl">
        <AlertCircle aria-hidden="true" />
        <AlertTitle>{titulo}</AlertTitle>
        <AlertDescription>{mensaje}</AlertDescription>
      </Alert>
    </div>
  );
}
