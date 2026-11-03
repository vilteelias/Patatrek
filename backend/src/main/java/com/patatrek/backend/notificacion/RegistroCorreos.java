package com.patatrek.backend.notificacion;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Últimos correos procesados (en memoria, no se guardan en la base).
 * Sirve para las pruebas automáticas y para revisar qué se mandó mientras se desarrolla.
 */
@Component
public class RegistroCorreos {

    public enum Resultado { ENVIADO, SIMULADO, ERROR }

    public record Envio(Correo correo, Resultado resultado, String detalle, LocalDateTime momento) {}

    private static final int MAXIMO = 200;

    private final Deque<Envio> envios = new ArrayDeque<>();

    synchronized void registrar(Correo correo, Resultado resultado, String detalle) {
        envios.addLast(new Envio(correo, resultado, detalle, LocalDateTime.now()));
        while (envios.size() > MAXIMO) {
            envios.removeFirst();
        }
    }

    public synchronized List<Envio> todos() {
        return List.copyOf(envios);
    }

    /** Correos dirigidos a esa dirección (sin distinguir mayúsculas). */
    public synchronized List<Envio> para(String destinatario) {
        return envios.stream().filter(e -> e.correo().destinatario().equalsIgnoreCase(destinatario)).toList();
    }

    public synchronized void limpiar() {
        envios.clear();
    }
}
