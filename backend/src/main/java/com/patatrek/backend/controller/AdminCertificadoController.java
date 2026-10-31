package com.patatrek.backend.controller;

import com.patatrek.backend.certificado.CertificadoMedicoResponse;
import com.patatrek.backend.certificado.CertificadoMedicoService;
import com.patatrek.backend.certificado.RechazoRequest;
import com.patatrek.backend.certificado.dto.CertificadoValidacionResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * CU-25 Validar certificados médicos (rol Administrador).
 * Las rutas /api/admin/** quedarán protegidas por rol en el módulo de Autenticación.
 */
@RestController
@RequestMapping("/api/admin/certificados")
public class AdminCertificadoController {

    private final CertificadoMedicoService service;

    public AdminCertificadoController(CertificadoMedicoService service) {
        this.service = service;
    }

    /** Certificados "Cargado" de salidas que no iniciaron, la más próxima primero. */
    @GetMapping("/pendientes")
    public List<CertificadoValidacionResponse> pendientes() {
        return service.listarCertificadosAValidar();
    }

    /** Muestra el archivo adjunto en el navegador (el frontend lo abre en una pestaña nueva). */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long id) {
        CertificadoMedicoService.ArchivoCertificado archivo = service.obtenerArchivo(id);
        return ResponseEntity.ok()
                .contentType(tipoDe(archivo))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombre()).build().toString())
                .body(new FileSystemResource(archivo.path()));
    }

    @PutMapping("/{id}/aprobar")
    public CertificadoMedicoResponse aprobar(@PathVariable Long id) {
        return CertificadoMedicoResponse.de(service.aprobar(id));
    }

    /** Devuelve el certificado actualizado: el frontend lee estadoCertificado (RECHAZADO o RECHAZADO_DEFINITIVO). */
    @PutMapping("/{id}/rechazar")
    public CertificadoMedicoResponse rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest datos) {
        return CertificadoMedicoResponse.de(service.rechazar(id, datos.motivo()));
    }

    private MediaType tipoDe(CertificadoMedicoService.ArchivoCertificado archivo) {
        try {
            String tipo = Files.probeContentType(archivo.path());
            if (tipo != null) {
                return MediaType.parseMediaType(tipo);
            }
        } catch (IOException ignorada) {
            // si no se puede detectar el tipo, se manda como binario
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
