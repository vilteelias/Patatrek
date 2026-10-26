package com.patatrek.backend.certificadoMedico.servicio;

import com.patatrek.backend.certificadoMedico.modelo.CertificadoMedico;
import com.patatrek.backend.certificadoMedico.modelo.EstadoCertificado;
import com.patatrek.backend.certificadoMedico.repositorio.CertificadoMedicoRepository;
import com.patatrek.backend.archivo.servicio.ArchivoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CertificadoMedicoService {
    private final CertificadoMedicoRepository certificadoMedicoRepository;
    private final ArchivoService archivoService;

    //constructor de la clase
    public CertificadoMedicoService(CertificadoMedicoRepository certificadoMedicoRepository,ArchivoService archivoService){
        this.certificadoMedicoRepository = certificadoMedicoRepository;
        this.archivoService = archivoService;
    }

    @Transactional (readOnly = true)
    public List<CertificadoMedico> listarCertificadosAValidar(){
        return certificadoMedicoRepository.findAllByOrderByFechaSubidaAsc();
    }

    @Transactional
    public CertificadoMedico aprobar(Long idCertificado) {
        CertificadoMedico c = buscar(idCertificado);
        c.setEstadoCertificado(EstadoCertificado.APROBADO);
        // TODO: notificar por mail al cliente
        return certificadoMedicoRepository.save(c);
    }

    @Transactional
    public CertificadoMedico rechazar(Long idCertificado, String motivo) {
        CertificadoMedico c = buscar(idCertificado);
        c.setMotivoRechazo(motivo);

        if (c.alcanzoMaximoIntentos()) {
            c.setEstadoCertificado(EstadoCertificado.RECHAZADO_DEFINITIVO);
            // TODO: participante -> NO_APTO, liberar cupo salida y plaza de refugio
        } else {
            c.setEstadoCertificado(EstadoCertificado.RECHAZADO);
        }
        // TODO: notificar por mail al cliente
        return certificadoMedicoRepository.save(c);
    }

    private CertificadoMedico buscar(Long id) {
        return certificadoMedicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificado no encontrado"));
    }

    private boolean puedeCargar(EstadoCertificado estado){
        return switch (estado){
            case PENDIENTE, RECHAZADO -> true;
            case CARGADO, APROBADO, RECHAZADO_DEFINITIVO -> false;
        };
    }
/*
   private OpcionCarga obtenerOpcion(EstadoCertificado estado){
        return switch (estado){
            case PENDIENTE, RECHAZADO -> OpcionCarga.HABILITADA;
            case CARGADO, APROBADO, RECHAZADO_DEFINITIVO -> OpcionCarga.DESHABILITADA;
        };
   }
*/
   private void validarArchivoCertificado (MultipartFile archivo){
       if (archivo == null || archivo.isEmpty()) {
           throw new IllegalArgumentException("No se subió ningún archivo");
       }
       if (archivo.getSize() > 5 * 1024 * 1024) {
           throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido");
       }
       String tipo = archivo.getContentType();
       if (!List.of("application/pdf", "image/jpeg", "image/png").contains(tipo)) {
           throw new IllegalArgumentException("El archivo no es del tipo requerido");
       }
   }

   @Transactional
   public CertificadoMedico confirmarEnvio(Long idCertificado, MultipartFile archivo){
        CertificadoMedico c = buscar (idCertificado);

        if(!puedeCargar(c.getEstadoCertificado())){
           throw new IllegalStateException("El certificado no admite una nueva carga");
        }
        validarArchivoCertificado(archivo);
       // TODO: validar que falten más de 72 hs (necesita Salida)
       String path = archivoService.guardar(archivo, "certificados");
       c.setPathArchivoAdjunto(path);

       c.setEstadoCertificado(EstadoCertificado.CARGADO);
       c.setFechaSubida(LocalDateTime.now());
       c.incrementarIntentos();
       return certificadoMedicoRepository.save(c);

   }

   /*intermedicario entre el controlador y el  servicio de archivos */
    @Transactional(readOnly = true)
    public Path obtenerArchivo(Long idCertificado) {
        CertificadoMedico c = buscar(idCertificado);
        if (c.getPathArchivoAdjunto() == null) {
            throw new IllegalStateException("El certificado no tiene archivo adjunto");
        }
        return archivoService.obtener(c.getPathArchivoAdjunto());
    }

}
