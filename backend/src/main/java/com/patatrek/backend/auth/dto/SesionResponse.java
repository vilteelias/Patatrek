package com.patatrek.backend.auth.dto;

import com.patatrek.backend.auth.UsuarioSesion;

/** Estado de la sesión: usuario = null si es un Visitante. */
public record SesionResponse(UsuarioSesion usuario, boolean exigeLogin) {}
