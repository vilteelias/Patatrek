import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import { AvisoPlazo } from '@/components/certificados/aviso-plazo';
import { FilaCertificado } from '@/components/certificados/fila-certificado';
import { ModalCargaCertificado } from '@/components/certificados/modal-carga-certificado';
import { iniciales } from '@/components/certificados/estado-certificado';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { plazoVencido } from '@/utilidades/fechas';
import { Volver } from '../cliente/Estados';
import { useCertificadosReserva } from './useCertificadosReserva.js';

// CU-24 Cargar certificado médico (vista del Cliente): carga el de cualquier participante de su reserva.
export default function CargarCertificadosPagina() {
    const { idReserva } = useParams();
    const { datos, cargando, error, recargar } = useCertificadosReserva(idReserva);
    const [seleccionado, setSeleccionado] = useState(null);

    const titular = datos?.certificados.find((c) => c.esTitular);
    const vencido = plazoVencido(datos?.horarioLimite);

    return (
        <div className="mx-auto flex max-w-[720px] flex-col gap-5 px-4 py-8">
            <Volver to={`/reservas/${idReserva}`}>Detalle de la reserva</Volver>

            <section aria-labelledby="titulo-certificados" className="flex flex-col gap-5">
                <header className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm text-muted-foreground">
                            Mi reserva #{idReserva} <span className="text-warning">/</span> Certificados médicos
                        </p>
                        <h1 id="titulo-certificados" className="mt-1 font-serif text-3xl font-semibold text-foreground">
                            Certificados médicos
                        </h1>
                        <p className="mt-2 max-w-[540px] text-pretty text-muted-foreground">
                            Esta excursión requiere certificado médico para todos los participantes. Podés cargarlo por tu cuenta o
                            pedirle a cada participante que lo haga con su propio ID de reserva y DNI.
                        </p>
                    </div>

                    {titular && (
                        <div className="hidden sm:flex items-center gap-2.5 rounded-xl border border-border bg-card px-3 py-2">
                            <span
                                aria-hidden="true"
                                className="flex size-9 shrink-0 items-center justify-center rounded-full bg-warning text-xs font-semibold text-white"
                            >
                                {iniciales(titular.apellido, titular.nombre)}
                            </span>
                            <span className="truncate text-sm font-medium text-foreground">
                                {titular.apellido}, {titular.nombre}
                            </span>
                        </div>
                    )}
                </header>

                {error && (
                    <Alert variant="destructive" className="border-destructive/30">
                        <AlertCircle aria-hidden="true" />
                        <AlertTitle>No pudimos cargar los certificados</AlertTitle>
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}

                {cargando && (
                    <div className="flex flex-col gap-3" aria-busy="true" aria-live="polite">
                        <span className="sr-only">Cargando certificados…</span>
                        <Skeleton className="h-12 w-full" />
                        <Skeleton className="h-64 w-full" />
                    </div>
                )}

                {datos && (
                    <>
                        <AvisoPlazo horarioLimite={datos.horarioLimite} vencido={vencido} />

                        {datos.certificados.length === 0 ? (
                            <p className="rounded-xl border border-dashed border-border bg-card px-6 py-10 text-center text-muted-foreground">
                                Esta reserva no tiene certificados para cargar.
                            </p>
                        ) : (
                            <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-card">
                                {datos.certificados.map((c) => (
                                    <li key={c.idCertificadoMedico}>
                                        <FilaCertificado certificado={c} plazoVencido={vencido} onCargar={setSeleccionado} />
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}
            </section>

            {seleccionado && (
                <ModalCargaCertificado
                    key={seleccionado.idCertificadoMedico}
                    certificado={seleccionado}
                    onSubido={recargar}
                    onCerrar={() => setSeleccionado(null)}
                />
            )}
        </div>
    );
}