import { UserRound } from 'lucide-react';
import { FormField } from '@/components/form-field';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

const CAMPOS = [
  { campo: 'nombre', label: 'Nombre', autoComplete: 'given-name' },
  { campo: 'apellido', label: 'Apellido', autoComplete: 'family-name' },
  { campo: 'dni', label: 'DNI', inputMode: 'numeric', placeholder: 'Sin puntos, ej. 38555777', maxLength: 8 },
  { campo: 'fechaNacimiento', label: 'Fecha de nacimiento', type: 'date' },
  { campo: 'correoElectronico', label: 'Correo electrónico', type: 'email', placeholder: 'nombre@mail.com' },
  { campo: 'nroTelefono', label: 'Teléfono', type: 'tel', placeholder: 'Ej. 2804-667788' },
];

/**
 * Datos de un participante (CU-18 paso 6).
 * CORRECCIÓN DE LA CÁTEDRA: todos los campos se muestran como obligatorios (asterisco rojo)
 * y cada error aparece debajo de su campo.
 */
export function FormularioParticipante({ indice, valor, errores, onChange, onUsarMisDatos, notaEdad }) {
  const error = (campo) => errores[`participantes[${indice}].${campo}`];

  return (
    <fieldset className="rounded-2xl border border-border bg-card px-5 py-5 sm:px-7 sm:py-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <legend className="text-base font-semibold text-foreground">Participante {indice + 1}</legend>
        {onUsarMisDatos && (
          <Button type="button" variant="ghost" size="sm" onClick={onUsarMisDatos}>
            <UserRound aria-hidden="true" /> Completar con mis datos
          </Button>
        )}
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        {CAMPOS.map(({ campo, label, ...resto }) => {
          const id = `p${indice}-${campo}`;
          const mensaje = error(campo);
          return (
            <FormField
              key={campo}
              id={id}
              label={label}
              required
              error={mensaje}
              hint={campo === 'fechaNacimiento' ? notaEdad : undefined}
            >
              <Input
                id={id}
                name={campo}
                type={resto.type ?? 'text'}
                value={valor[campo]}
                onChange={(e) => onChange(campo, e.target.value)}
                aria-invalid={mensaje ? true : undefined}
                aria-describedby={mensaje ? `${id}-error` : undefined}
                aria-required="true"
                className="h-10 bg-card"
                {...resto}
              />
            </FormField>
          );
        })}
      </div>
    </fieldset>
  );
}
