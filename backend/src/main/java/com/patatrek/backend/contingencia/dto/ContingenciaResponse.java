package com.patatrek.backend.contingencia.dto;

import com.patatrek.backend.salida.dto.SalidaAdminResponse;

/** Resultado de CU-14: mensaje de éxito, reservas afectadas, correos enviados y la salida actualizada. */
public record ContingenciaResponse(
        Long idContingencia,
        String mensaje,
        int reservasAfectadas,
        int correosEnviados,
        SalidaAdminResponse salida
) {}
