package com.patatrek.backend.contingencia;

import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.contingencia.dto.ContingenciaResponse;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.salida.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContingenciaService {

    private static final Logger log = LoggerFactory.getLogger(ContingenciaService.class);
    private static final int MAXIMO_TEXTO = 500;
    private static final int MAXIMO_RECORRIDO = 1000;

    private final ContingenciaRepository contingenciaRepository;
    private final GestionSalidaService gestionSalida;
    private final Map<TipoResolucion, ResolucionContingenciaStrategy> estrategias;

    public ContingenciaService(ContingenciaRepository contingenciaRepository, GestionSalidaService gestionSalida,
                               List<ResolucionContingenciaStrategy> listaEstrategias) {
        this.contingenciaRepository = contingenciaRepository;
        this.gestionSalida = gestionSalida;
        this.estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(
                        ResolucionContingenciaStrategy::getTipoResolucion,
                        Function.identity()
                ));
    }

    @Transactional
    public ContingenciaResponse registrar(Long idSalida, ContingenciaRequest d) {
        Salida s = gestionSalida.buscar(idSalida, true);
        if (!GestionSalidaService.ACTIVAS.contains(s.getEstadoSalida())) {
            throw new ReglaNegocioException(
                    "Solo se puede registrar una contingencia en salidas Disponibles, Completas o En curso.");
        }

        Map<String, String> errores = new LinkedHashMap<>();
        if (d.tipoContingencia() == null) errores.put("tipoContingencia", "Elegí el tipo de contingencia.");
        if (d.resolucion() == null) errores.put("resolucion", "Elegí la resolución.");
        String motivo = d.motivo() == null ? "" : d.motivo().trim();
        if (motivo.isEmpty()) {
            errores.put("motivo", "Ingresá el motivo de la contingencia.");
        } else if (motivo.length() > MAXIMO_TEXTO) {
            errores.put("motivo", "El motivo puede tener hasta " + MAXIMO_TEXTO + " caracteres.");
        }
        String recorrido = d.nuevoRecorrido() == null ? "" : d.nuevoRecorrido().trim();
        if (recorrido.length() > MAXIMO_RECORRIDO) {
            errores.put("nuevoRecorrido", "El recorrido puede tener hasta " + MAXIMO_RECORRIDO + " caracteres.");
        }
        if (d.resolucion() == TipoResolucion.MODIFICACION && recorrido.isEmpty()) {
            errores.put("nuevoRecorrido", "Ingresá el recorrido alternativo.");
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos marcados.", errores);
        }

        Contingencia c = new Contingencia();
        c.setSalida(s);
        c.setTipoContingencia(d.tipoContingencia().name());
        c.setTipoResolucion(d.resolucion().name());
        c.setMotivo(motivo);

        ResolucionContingenciaStrategy estrategia = estrategias.get(d.resolucion());
        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de resolución no soportado: " + d.resolucion());
        }

        ResultadoContingencia r = estrategia.procesar(s, c, d);
        contingenciaRepository.save(c);

        log.info("Contingencia {} ({}) en la salida #{}: {} reservas afectadas", d.resolucion(), d.tipoContingencia(),
                idSalida, r.reservas());
        return new ContingenciaResponse(c.getIdContingencia(), r.mensaje(), r.reservas(), r.correos(),
                gestionSalida.detalle(idSalida));
    }
}