import { useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, FileText, Loader2, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { subirCertificado, TAMANIO_MAXIMO, TIPOS_PERMITIDOS } from '@/api/certificados';
import { cn } from '@/lib/utils';

const ARCHIVO_INVALIDO = 'El archivo no cumple con el formato o el tamaño permitido (PDF, JPG o PNG · máx. 5 MB).';
const TITULO = 'font-serif text-xl font-semibold text-foreground';

const tamanio = (bytes) => (bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(bytes / 1024)} KB`);

/**
 * Pasos: seleccionando → listo (confirmar) → procesando → exito.
 * Montar con key={idCertificado} para que el estado se reinicie en cada apertura.
 * onSubido: se llama apenas el backend confirma la carga (para refrescar la lista de fondo).
 */
export function ModalCargaCertificado({ certificado, dni, onCerrar, onSubido }) {
  const [paso, setPaso] = useState('seleccionando');
  const [archivo, setArchivo] = useState(null);
  const [error, setError] = useState('');
  const [arrastrando, setArrastrando] = useState(false);
  const inputRef = useRef(null);
  const procesando = paso === 'procesando';

  function elegir(elegido) {
    if (!elegido) return;
    if (!TIPOS_PERMITIDOS.includes(elegido.type) || elegido.size > TAMANIO_MAXIMO) {
      setError(ARCHIVO_INVALIDO);
      return;
    }
    setError('');
    setArchivo(elegido);
    setPaso('listo');
  }

  async function subir() {
    setPaso('procesando');
    setError('');
    try {
      await subirCertificado(certificado.idCertificadoMedico, archivo, dni);
      setPaso('exito');
      onSubido();
    } catch (e) {
      setError(e.message);
      setPaso('listo');
    }
  }

  const mensajeError = error && (
    <p
      role="alert"
      className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive"
    >
      <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {error}
    </p>
  );

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && !procesando && onCerrar()}>
      <DialogContent showCloseButton={false} className="gap-5 rounded-xl p-6 sm:max-w-[460px]">
        {paso === 'seleccionando' && (
          <>
            <DialogHeader>
              <DialogTitle className={TITULO}>Cargar certificado médico</DialogTitle>
              <DialogDescription>
                {certificado.apellido}, {certificado.nombre}
              </DialogDescription>
            </DialogHeader>

            <input
              ref={inputRef}
              type="file"
              hidden
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              onChange={(e) => {
                elegir(e.target.files?.[0]);
                e.target.value = '';
              }}
            />
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              onDragOver={(e) => {
                e.preventDefault();
                setArrastrando(true);
              }}
              onDragLeave={() => setArrastrando(false)}
              onDrop={(e) => {
                e.preventDefault();
                setArrastrando(false);
                elegir(e.dataTransfer.files?.[0]);
              }}
              className={cn(
                'flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-input bg-muted/60 px-6 py-8 text-center transition-colors',
                'hover:bg-muted focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none',
                arrastrando && 'border-primary bg-accent',
              )}
            >
              <Upload className="size-6 text-muted-foreground" aria-hidden="true" />
              <span className="text-sm text-foreground">Arrastrá el archivo o hacé clic para seleccionar</span>
              <span className="text-xs text-muted-foreground">
                Formatos permitidos: PDF, JPG o PNG · Tamaño máximo: 5 MB
              </span>
            </button>

            {mensajeError}

            <DialogFooter className="sm:justify-center">
              <Button variant="ghost" onClick={onCerrar} className="text-muted-foreground">
                Cancelar
              </Button>
            </DialogFooter>
          </>
        )}

        {paso === 'listo' && (
          <>
            <DialogHeader>
              <DialogTitle className={TITULO}>Confirmar carga</DialogTitle>
              <DialogDescription>
                {certificado.apellido}, {certificado.nombre}
              </DialogDescription>
            </DialogHeader>
            <div className="flex items-center gap-3 rounded-lg border border-border bg-muted/60 px-4 py-3.5">
              <FileText className="size-5 shrink-0 text-primary" aria-hidden="true" />
              <span className="min-w-0 flex-1 truncate text-sm text-foreground">{archivo.name}</span>
              <span className="shrink-0 text-xs text-muted-foreground">{tamanio(archivo.size)}</span>
            </div>
            {mensajeError}
            <DialogFooter>
              <Button variant="outline" size="lg" onClick={() => setPaso('seleccionando')}>
                Elegir otro
              </Button>
              <Button size="lg" onClick={subir}>
                Subir certificado
              </Button>
            </DialogFooter>
          </>
        )}

        {procesando && (
          <div className="flex flex-col items-center gap-4 py-6" role="status" aria-live="polite">
            <Loader2 className="size-7 animate-spin text-primary" aria-hidden="true" />
            <DialogTitle className="text-sm font-normal text-muted-foreground">Subiendo el archivo…</DialogTitle>
          </div>
        )}

        {paso === 'exito' && (
          <div className="flex flex-col items-center gap-3 pt-2 text-center">
            <span className="flex size-12 items-center justify-center rounded-full bg-accent text-accent-foreground">
              <CheckCircle2 className="size-6" aria-hidden="true" />
            </span>
            <DialogTitle className="text-base font-semibold text-foreground">Certificado cargado</DialogTitle>
            <DialogDescription className="max-w-[340px]">
              Quedó en estado <strong>Cargado</strong>, a la espera de revisión por parte del administrador.
            </DialogDescription>
            <Button size="lg" onClick={onCerrar} className="mt-2 px-6">
              Listo
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
