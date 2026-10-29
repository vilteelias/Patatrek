import { useEffect, useState } from 'react';
import { AlertCircle, FileCheck } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { ModalRevision } from '@/components/certificados/modal-revision';
import { TablaValidacion } from '@/components/certificados/tabla-validacion';
import { listarCertificadosAValidar } from '@/api/certificados';

function Resumen({ etiqueta, valor, destacado }) {
  return (
    <div className="rounded-xl border border-border bg-card px-5 py-4">
      <p className="text-xs text-muted-foreground">{etiqueta}</p>
      <p className={`mt-0.5 text-2xl font-semibold tabular-nums ${destacado ? 'text-destructive' : 'text-foreground'}`}>
        {valor}
      </p>
    </div>
  );
}

// CU-25 Validar certificados médicos de participantes (Administrador).
export default function AdminCertificados() {
  const [certificados, setCertificados] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resueltos, setResueltos] = useState({}); // revisados en esta sesión: se quedan en la tabla con su resultado
  const [seleccionado, setSeleccionado] = useState(null);

  useEffect(() => {
    listarCertificadosAValidar()
      .then(setCertificados)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, []);

  const pendientes = certificados.filter((c) => !resueltos[c.idCertificadoMedico]);
  const reenvios = pendientes.filter((c) => c.intentosEnvio >= 2);

  return (
    <section aria-labelledby="titulo-validar" className="mx-auto flex w-full max-w-[1020px] flex-col gap-7">
      <header>
        <p className="text-sm text-muted-foreground">Certificados médicos</p>
        <h1 id="titulo-validar" className="mt-1 font-serif text-3xl font-semibold text-foreground sm:text-4xl">
          Validar certificados médicos
        </h1>
        <p className="mt-2 max-w-[680px] text-pretty text-muted-foreground">
          Revisá la documentación cargada por cada participante y definí su aprobación o rechazo. El cliente recibe la
          decisión automáticamente por correo. El listado prioriza las salidas más próximas.
        </p>
      </header>

      {error && (
        <Alert variant="destructive" className="border-destructive/30">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>No pudimos cargar los certificados</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {cargando && (
        <div className="flex flex-col gap-4" aria-busy="true" aria-live="polite">
          <span className="sr-only">Cargando certificados…</span>
          <div className="grid gap-4 sm:grid-cols-3">
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
          </div>
          <Skeleton className="h-64 w-full" />
        </div>
      )}

      {!cargando && !error && (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <Resumen etiqueta="Pendientes de revisión" valor={pendientes.length} />
            <Resumen etiqueta="Reenvíos (2do intento)" valor={reenvios.length} destacado />
            <Resumen etiqueta="Revisados en esta sesión" valor={Object.keys(resueltos).length} />
          </div>

          {certificados.length === 0 ? (
            <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-border bg-card px-6 py-14 text-center">
              <FileCheck className="size-8 text-muted-foreground" aria-hidden="true" />
              <p className="text-muted-foreground">No hay certificados pendientes de revisión.</p>
            </div>
          ) : (
            <TablaValidacion certificados={certificados} resueltos={resueltos} onRevisar={setSeleccionado} />
          )}
        </>
      )}

      {seleccionado && (
        <ModalRevision
          key={seleccionado.idCertificadoMedico}
          certificado={seleccionado}
          onResuelto={(id, estado) => setResueltos((previos) => ({ ...previos, [id]: estado }))}
          onCerrar={() => setSeleccionado(null)}
        />
      )}
    </section>
  );
}
