package com.patatrek.backend.contingencia;

import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.salida.Salida;

public interface ResolucionContingenciaStrategy {
    TipoResolucion getTipoResolucion();
    ResultadoContingencia procesar(Salida salida, Contingencia contingencia, ContingenciaRequest request);
}