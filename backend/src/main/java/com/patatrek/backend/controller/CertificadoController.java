package com.patatrek.backend.controller;

import com.patatrek.backend.certificado.CertificadoMedicoResponse;
import com.patatrek.backend.certificado.CertificadoMedicoService;
import com.patatrek.backend.certificado.dto.CertificadosReservaResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CU-24 Cargar certificado médico: lo usan el Cliente y el Participante.
 * El Participante se identifica con ID de reserva + DNI (parámetro "dni"); el Cliente, sin dni.
 */
@RestController
@RequestMapping("/api")
public class CertificadoController {

    private final CertificadoMedicoService service;

    public CertificadoController(CertificadoMedicoService service) {
        this.service = service;
    }

    /** Certificados de la reserva (todos, o solo el del participante si se envía su dni). */
    @GetMapping("/reservas/{idReserva}/certificados")
    public CertificadosReservaResponse certificados(@PathVariable Long idReserva,
                                                    @RequestParam(required = false) String dni) {
        return service.certificadosDeReserva(idReserva, dni);
    }

    /** Sube el archivo (multipart, campo "archivo"). */
    @PostMapping(value = "/certificados/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CertificadoMedicoResponse cargar(@PathVariable Long id,
                                            @RequestParam("archivo") MultipartFile archivo,
                                            @RequestParam(required = false) String dni) {
        return CertificadoMedicoResponse.de(service.confirmarEnvio(id, archivo, dni));
    }
}
