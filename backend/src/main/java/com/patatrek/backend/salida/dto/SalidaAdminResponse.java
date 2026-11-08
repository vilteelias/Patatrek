package com.patatrek.backend.salida.dto;

import com.patatrek.backend.salida.EstadoSalida;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Salida en el catálogo general del Administrador (RF-09 Listar salidas, CU-10):
 * fecha y hora de inicio/finalización, refugios asignados, guía responsable, guías ayudantes,
 * cupo máximo y estado. Incluye además los datos que usan las pantallas de CU-11 a CU-14.
 */
public record SalidaAdminResponse(
        Long idSalida,
        Long idExcursion,
        String excursion,
        int duracionDias,
        boolean excursionRequiereCertificado,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        LocalTime horaInicio,
        LocalTime horaFinalizacion,
        EstadoSalida estadoSalida,
        int cupoBase,
        int cupoMaximo,
        int cupoTotal,
        /** Participantes de reservas Señadas, Confirmadas o Pendientes de pago (los que ocupan lugar). */
        int cuposOcupados,
        int cuposDisponibles,
        GuiaResumen guiaResponsable,
        List<GuiaResumen> ayudantes,
        List<RefugioAsignado> refugios,
        String recorrido,
        String motivoCancelacion,
        String observaciones,
        int reservasActivas,
        boolean iniciada,
        /** CU-11 / CU-12: la salida no empezó y está Disponible o Completa. */
        boolean modificable,
        /** CU-13: modificable y sin reservas activas. */
        boolean puedeDarseDeBaja,
        /** CU-14: Disponible, Completa o En curso. */
        boolean admiteContingencia,
        List<ContingenciaResumen> contingencias,
        /** CU-16 / CU-17: fecha y hora en que el guía responsable marcó la finalización. */
        LocalDateTime fechaHoraFinReal
) {
    public record GuiaResumen(Long idGuia, String nombre, int capacidadEstandar) {}

    public record RefugioAsignado(Long idRefugio, String nombre, int ordenVisita, int plazasReservadas,
                                  int plazasOcupadas, int plazasRetenidas) {}

    public record ContingenciaResumen(Long idContingencia, LocalDateTime fechaRegistro, String tipoContingencia,
                                      String tipoResolucion, String motivo, String recorridoAlternativo,
                                      LocalDateTime fechaAnteriorSalida) {}
}
