package com.patatrek.backend.certificadoMedico.controlador;

import com.patatrek.backend.certificadoMedico.dto.CertificadoMedicoResponse;
import com.patatrek.backend.certificadoMedico.dto.RechazoRequest;
import com.patatrek.backend.certificadoMedico.servicio.CertificadoMedicoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoMedicoController {

    private final CertificadoMedicoService certificadoMedicoService;

    public CertificadoMedicoController(CertificadoMedicoService certificadoMedicoService) {
        this.certificadoMedicoService = certificadoMedicoService;
    }

    // CU-24: cargar certificado
    /* value =  la URL que dispara este metodo
    PathVariable toma el valor de id (que se encuantra en la URL)
    @RequestParam toma el valor enviado por el formulario con el nombre "archivo" (similar al $_POST["archivo"])pull
     */
    @PostMapping(value = "/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CertificadoMedicoResponse cargar(@PathVariable Long id,
                                            @RequestParam("archivo") MultipartFile archivo) {
        return CertificadoMedicoResponse.de(certificadoMedicoService.confirmarEnvio(id, archivo));
    }

    // CU-25: listar certificados a validar
    @GetMapping("/pendientes")
    public List<CertificadoMedicoResponse> listarAValidar() {
        return certificadoMedicoService.listarCertificadosAValidar().stream()
                .map(CertificadoMedicoResponse::de)
                .toList();
    }

    // CU-25: abrir el archivo adjunto
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> verArchivo(@PathVariable Long id) throws IOException {
        Path path = certificadoMedicoService.obtenerArchivo(id);
        String tipo = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo != null ? tipo : "application/octet-stream"))
                .body(new FileSystemResource(path));
    }

    // CU-25: aprobar
    @PutMapping("/{id}/aprobar")
    public CertificadoMedicoResponse aprobar(@PathVariable Long id) {
        return CertificadoMedicoResponse.de(certificadoMedicoService.aprobar(id));
    }

    // CU-25: rechazar
    @PutMapping("/{id}/rechazar")
    public CertificadoMedicoResponse rechazar(@PathVariable Long id,
                                              @RequestBody RechazoRequest request) {
        return CertificadoMedicoResponse.de(certificadoMedicoService.rechazar(id, request.motivo()));
    }
}