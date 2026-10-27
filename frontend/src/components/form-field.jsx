import { Lock } from 'lucide-react';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
export function FormField({ id, label, required, optional, locked, error, hint, className, children }) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <Label htmlFor={id} className="flex items-center gap-1 text-sm font-medium">
        <span>{label}</span>
        {required && (
          <span aria-hidden="true" className="text-destructive">
            *
          </span>
        )}
        {required && <span className="sr-only">(obligatorio)</span>}
        {optional && !required && <span className="font-normal text-muted-foreground">(opcional)</span>}
        {locked && <Lock className="size-3.5 text-muted-foreground" aria-label="Campo no editable" />}
      </Label>
      {children}
      {error ? (
        <p id={`${id}-error`} role="alert" className="text-xs text-destructive">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-xs text-muted-foreground">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
export function RequiredLegend({ className }) {
  return (
    <p className={cn('text-sm text-muted-foreground', className)}>
      Los campos marcados con <span className="text-destructive">*</span> son obligatorios.
    </p>
  );
}
