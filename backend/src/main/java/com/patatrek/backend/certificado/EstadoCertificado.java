package com.patatrek.backend.certificado;

/** Estados del certificado médico (CU-24, CU-25). Solo APROBADO habilita al participante (CU-15). */
public enum EstadoCertificado {
    PENDIENTE, CARGADO, APROBADO, RECHAZADO, RECHAZADO_DEFINITIVO
}
