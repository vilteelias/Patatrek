import { useEffect, useState } from 'react';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { FormField } from '@/components/form-field';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { pesos } from './formato';

const MAXIMO = 400;

/**
 * CU-23 Cancelar reserva (prototipo pág. 15).
 * Paso 2: avisa que se cancelan todos los servicios, que NO se devuelve lo abonado y que es irreversible.
 * El motivo es opcional (flujo 3.b).
 * onConfirmar(motivo) → promesa; onMantener() → flujo 3.a "no se realizaron cambios".
 */
export function CancelarReservaDialog({ abierto, importeAbonado, onConfirmar, onMantener }) {
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (abierto) {
      setMotivo('');
      setError('');
    }
  }, [abierto]);

  const confirmar = async () => {
    setEnviando(true);
    setError('');
    try {
      await onConfirmar(motivo.trim());
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Dialog open={abierto} onOpenChange={(abrir) => !abrir && !enviando && onMantener()}>
      <DialogContent showCloseButton={false} className="rounded-xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Cancelar reserva</DialogTitle>
          <DialogDescription className="sr-only">Confirmá la cancelación de la reserva.</DialogDescription>
        </DialogHeader>

        <div className="flex gap-3 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-3 text-sm text-destructive">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p>
            Se cancelarán todos los servicios contratados de esta reserva.{' '}
            <strong>
              {Number(importeAbonado) > 0
                ? `No se te devolverá el monto abonado (${pesos(importeAbonado)})`
                : 'No se devuelve ningún importe'}
            </strong>
            , que queda retenido en concepto de gastos administrativos. Esta acción es <strong>irreversible</strong>.
          </p>
        </div>

        <FormField
          id="motivo-cancelacion"
          label="Motivo"
          optional
          error={error}
          hint={`${motivo.length}/${MAXIMO} caracteres`}
        >
          <Textarea
            id="motivo-cancelacion"
            value={motivo}
            maxLength={MAXIMO}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Contanos por qué cancelás (opcional)…"
            disabled={enviando}
            className="min-h-20 bg-card"
          />
        </FormField>

        <DialogFooter className="gap-2">
          <Button type="button" variant="outline" className="h-10 px-4" onClick={onMantener} disabled={enviando}>
            No, mantener reserva
          </Button>
          <Button
            type="button"
            className="h-10 bg-destructive px-4 text-white hover:bg-destructive/90"
            onClick={confirmar}
            disabled={enviando}
          >
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
            Sí, cancelar reserva
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
