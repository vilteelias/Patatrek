import { useState } from 'react';
import { useLocation, useParams, useSearchParams } from 'react-router-dom';
import { AlertCircle, ShieldCheck } from 'lucide-react';
import { AccesoLayout } from '@/components/layouts/acceso-layout';
import { AvisoPlazo } from '@/components/certificados/aviso-plazo';
import { FilaCertificado } from '@/components/certificados/fila-certificado';
import { ModalCargaCertificado } from '@/components/certificados/modal-carga-certificado';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { FormField } from '@/components/form-field';
import { plazoVencido } from '@/utilidades/fechas';
import { useCertificadosReserva } from './useCertificadosReserva.js';

// CU-24 Cargar certificado médico (vista del Participante): solo puede cargar el suyo.
// El DNI llega desde "Consultar y gestionar reserva" (CU-19) en location.state, o por ?dni= en la URL.
// Si no llega, se lo pedimos acá.
export default function MiCertificadoPagina() {
  const { idReserva } = useParams();
  const { state } = useLocation();
  const [params, setParams] = useSearchParams();
  const dni = state?.dni ?? params.get('dni') ?? '';

  const { datos, cargando, error, recargar } = useCertificadosReserva(idReserva, dni, Boolean(dni));
  const [seleccionado, setSeleccionado] = useState(null);
  const [dniIngresado, setDniIngresado] = useState('');

  const miCertificado = datos?.certificados[0];
  const vencido = plazoVencido(datos?.horarioLimite);

  return (
    <AccesoLayout
      derecha={
        <span className="flex items-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck className="size-4 shrink-0" aria-hidden="true" />
          <span className="truncate">
            Acceso por ID de reserva + DNI
            {miCertificado && ` · ${miCertificado.apellido}, ${miCertificado.nombre}`}
          </span>
        </span>
      }
    >
      <section aria-labelledby="titulo-mi-certificado" className="flex flex-col gap-5">
        <header>
          <p className="text-sm text-muted-foreground">
            Reserva #{idReserva} <span className="text-warning">/</span> Mi certificado médico
          </p>
          <h1 id="titulo-mi-certificado" className="mt-1 font-serif text-3xl font-semibold text-foreground">
            Mi certificado médico
          </h1>
          <p className="mt-2 max-w-[500px] text-pretty text-muted-foreground">
            Esta excursión requiere certificado médico. Como participante, solo podés cargar tu propio certificado —
            no el de los demás integrantes de la reserva.
          </p>
        </header>

        {!dni && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (dniIngresado.trim()) setParams({ dni: dniIngresado.trim() });
            }}
            className="flex flex-col gap-4 rounded-xl border border-border bg-card p-6"
          >
            <FormField id="dni" label="Tu DNI" required hint="Es el que figura en la reserva, sin puntos.">
              <Input
                id="dni"
                inputMode="numeric"
                autoComplete="off"
                value={dniIngresado}
                onChange={(e) => setDniIngresado(e.target.value.replace(/\D/g, ''))}
              />
            </FormField>
            <Button type="submit" size="lg" className="self-start" disabled={!dniIngresado.trim()}>
              Continuar
            </Button>
          </form>
        )}

        {error && (
          <Alert variant="destructive" className="border-destructive/30">
            <AlertCircle aria-hidden="true" />
            <AlertTitle>No pudimos cargar tu certificado</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {dni && cargando && (
          <div className="flex flex-col gap-3" aria-busy="true" aria-live="polite">
            <span className="sr-only">Cargando tu certificado…</span>
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-28 w-full" />
          </div>
        )}

        {datos && (
          <>
            <AvisoPlazo horarioLimite={datos.horarioLimite} vencido={vencido} propio />
            {miCertificado ? (
              <div className="overflow-hidden rounded-xl border border-border bg-card">
                <FilaCertificado certificado={miCertificado} plazoVencido={vencido} grande onCargar={setSeleccionado} />
              </div>
            ) : (
              <p className="rounded-xl border border-dashed border-border bg-card px-6 py-10 text-center text-muted-foreground">
                No encontramos un certificado asociado a tu DNI en esta reserva.
              </p>
            )}
          </>
        )}
      </section>

      {seleccionado && (
        <ModalCargaCertificado
          key={seleccionado.idCertificadoMedico}
          certificado={seleccionado}
          dni={dni}
          onSubido={recargar}
          onCerrar={() => setSeleccionado(null)}
        />
      )}
    </AccesoLayout>
  );
}
