package com.patatrek.backend.devolucion.dto;

import java.math.BigDecimal;
import java.util.List;

/** Pantalla de devoluciones: las pendientes (CU-27 paso 2), el total a reintegrar y las ya devueltas. */
public record DevolucionesResponse(
        List<DevolucionResponse> pendientes,
        BigDecimal montoTotalPendiente,
        List<DevolucionResponse> realizadas
) {}
