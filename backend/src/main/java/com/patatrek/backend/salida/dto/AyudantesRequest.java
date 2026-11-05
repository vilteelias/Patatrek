package com.patatrek.backend.salida.dto;

import java.util.List;

/** CU-12 Asignar personal y cupos: la lista completa de guías ayudantes que debe quedar en la salida. */
public record AyudantesRequest(List<Long> idsGuias) {}
