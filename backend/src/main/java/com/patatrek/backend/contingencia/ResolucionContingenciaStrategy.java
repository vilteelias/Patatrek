package com.patatrek.backend.contingencia;

import com.patatrek.backend.salida.Salida;

public interface ResolucionContingenciaStrategy {
    // Recibe la salida afectada, la entidad contingencia a guardar y el DTO con los datos del frontend
    void procesarResolucion(Salida salida, Contingencia contingencia, ContingenciaDTO dto);
}
