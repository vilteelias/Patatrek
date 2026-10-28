package com.patatrek.backend.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Devuelve el usuario que inició sesión en el pedido actual (si hay uno). */
@Component
public class UsuarioActual {

    public Optional<UsuarioSesion> obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioSesion u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    /** El usuario si inició sesión con ese rol. */
    public Optional<UsuarioSesion> conRol(Rol rol) {
        return obtener().filter(u -> u.rol() == rol);
    }
}
