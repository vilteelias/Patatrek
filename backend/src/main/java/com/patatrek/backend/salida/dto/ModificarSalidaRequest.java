package com.patatrek.backend.salida.dto;

import java.util.List;

/**
 * CU-11 Modificar salida (RF-10): cambiar el guía responsable y los refugios con sus plazas.
 * Los guías ayudantes se cambian con CU-12. El cronograma solo cambia con una contingencia (CU-14).
 */
public record ModificarSalidaRequest(
        Long idGuiaResponsable,
        Integer plazasPorRefugio,
        List<Long> refugiosPorNoche
) {}
