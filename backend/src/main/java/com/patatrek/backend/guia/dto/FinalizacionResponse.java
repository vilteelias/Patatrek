package com.patatrek.backend.guia.dto;

/** Resultado de CU-16: cuántos participantes recibieron la solicitud de valoración. */
public record FinalizacionResponse(int cantidadNotificados, SalidaGuiaResponse salida) {
}
