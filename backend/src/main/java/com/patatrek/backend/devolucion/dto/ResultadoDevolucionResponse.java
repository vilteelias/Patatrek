package com.patatrek.backend.devolucion.dto;

import java.math.BigDecimal;

/** CU-27 paso 9: mensaje de éxito con el comprobante del reintegro. */
public record ResultadoDevolucionResponse(
        Long idReserva,
        BigDecimal importeReintegrado,
        String nroTransaccionReintegro,
        String mensaje,
        DevolucionResponse devolucion
) {}
