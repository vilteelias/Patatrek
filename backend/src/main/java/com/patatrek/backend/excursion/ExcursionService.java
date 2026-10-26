package com.patatrek.backend.excursion;

import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.excursion.dto.ExcursionAdminResponse;
import com.patatrek.backend.excursion.dto.ExcursionPublicaResponse;
import com.patatrek.backend.excursion.dto.ExcursionRequest;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.SalidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reglas de negocio del paquete Excursión:
 * CU-04 Registrar, CU-05 Agregar equipamiento, CU-06 Listar, CU-07 Modificar, CU-08 Baja.
 */
@Service
public class ExcursionService {

    static final String MSG_BLOQUEADO =
            "No se puede modificar: la excursión tiene salidas programadas activas.";

    private final ExcursionRepository excursionRepository;
    private final EquipamientoRepository equipamientoRepository;
    private final SalidaRepository salidaRepository;
    private final ImagenExcursionService imagenService;

    public ExcursionService(ExcursionRepository excursionRepository,
                            EquipamientoRepository equipamientoRepository,
                            SalidaRepository salidaRepository,
                            ImagenExcursionService imagenService) {
        this.excursionRepository = excursionRepository;
        this.equipamientoRepository = equipamientoRepository;
        this.salidaRepository = salidaRepository;
        this.imagenService = imagenService;
    }

    // =========================================================
    //  CU-06 Listar excursiones
    // =========================================================

    /** Visitante y Cliente: solo excursiones activas. */
    @Transactional(readOnly = true)
    public List<ExcursionPublicaResponse> listarPublicas() {
        return excursionRepository.findByEstadoExcursionOrderByNombreAsc(EstadoExcursion.ACTIVA)
                .stream().map(ExcursionPublicaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ExcursionPublicaResponse obtenerPublica(Long id) {
        Excursion e = buscar(id);
        if (!e.estaActiva()) {
            throw new RecursoNoEncontradoException("La excursión no está disponible.");
        }
        return ExcursionPublicaResponse.de(e);
    }

    /** Administrador: catálogo completo (activas e inactivas), opcionalmente filtrado. */
    @Transactional(readOnly = true)
    public List<ExcursionAdminResponse> listarAdmin(EstadoExcursion estado) {
        List<Excursion> excursiones = (estado == null)
                ? excursionRepository.findAllByOrderByNombreAsc()
                : excursionRepository.findByEstadoExcursionOrderByNombreAsc(estado);
        Set<Long> conSalidas = salidaRepository.findIdsExcursionConSalidasEn(EstadoSalida.ACTIVAS);
        return excursiones.stream()
                .map(e -> ExcursionAdminResponse.de(e, conSalidas.contains(e.getIdExcursion())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExcursionAdminResponse obtenerAdmin(Long id) {
        Excursion e = buscar(id);
        return ExcursionAdminResponse.de(e, tieneSalidasActivas(id));
    }

    // =========================================================
    //  CU-04 Registrar excursión (+ CU-05 equipamiento)
    // =========================================================

    @Transactional
    public ExcursionAdminResponse registrar(ExcursionRequest datos) {
        String nombre = datos.nombre().trim();
        if (excursionRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicado();
        }

        Excursion e = new Excursion();
        e.setNombre(nombre);
        e.setDescripcionGeneral(datos.descripcionGeneral().trim());
        e.setDescripcionTecnica(datos.descripcionTecnica().trim());
        aplicarParametrosOperativos(e, datos);
        e.setEstadoExcursion(EstadoExcursion.ACTIVA);

        return ExcursionAdminResponse.de(excursionRepository.save(e), false);
    }

    // =========================================================
    //  CU-07 Modificar excursión
    // =========================================================

    @Transactional
    public ExcursionAdminResponse modificar(Long id, ExcursionRequest datos) {
        Excursion e = buscar(id);

        String nombre = datos.nombre().trim();
        if (excursionRepository.existsByNombreIgnoreCaseAndIdExcursionNot(nombre, id)) {
            throw nombreDuplicado();
        }

        boolean salidasActivas = tieneSalidasActivas(id);
        if (salidasActivas) {
            Map<String, String> bloqueados = camposOperativosModificados(e, datos);
            if (!bloqueados.isEmpty()) {
                throw new ReglaNegocioException(
                        "La excursión tiene salidas programadas activas. Solo podés modificar el nombre, "
                                + "las descripciones y la imagen.", bloqueados);
            }
        }

        // Campos informativos: se pueden modificar siempre (RF-06)
        e.setNombre(nombre);
        e.setDescripcionGeneral(datos.descripcionGeneral().trim());
        e.setDescripcionTecnica(datos.descripcionTecnica().trim());

        // Parámetros operativos: solo sin salidas activas
        if (!salidasActivas) {
            aplicarParametrosOperativos(e, datos);
        }

        return ExcursionAdminResponse.de(excursionRepository.save(e), salidasActivas);
    }

    // =========================================================
    //  CU-08 Baja de excursión (baja lógica)
    // =========================================================

    @Transactional
    public ExcursionAdminResponse darDeBaja(Long id, String motivo) {
        Excursion e = buscar(id);
        if (!e.estaActiva()) {
            throw new ReglaNegocioException("La excursión ya se encuentra inactiva.");
        }
        e.setEstadoExcursion(EstadoExcursion.INACTIVA);
        e.setMotivoBaja(motivo.trim());
        // Las salidas ya creadas y sus reservas no se tocan (RF-07).
        return ExcursionAdminResponse.de(excursionRepository.save(e), tieneSalidasActivas(id));
    }

    // =========================================================
    //  Imagen promocional (campo informativo: editable siempre)
    // =========================================================

    @Transactional
    public ExcursionAdminResponse actualizarImagen(Long id, MultipartFile archivo) {
        Excursion e = buscar(id);
        String anterior = e.getImagenPromocional();
        e.setImagenPromocional(imagenService.guardar(id, archivo));
        excursionRepository.save(e);
        imagenService.eliminar(anterior);
        return ExcursionAdminResponse.de(e, tieneSalidasActivas(id));
    }

    // =========================================================
    //  Auxiliares
    // =========================================================

    private Excursion buscar(Long id) {
        return excursionRepository.findWithEquipamientoByIdExcursion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la excursión solicitada."));
    }

    private boolean tieneSalidasActivas(Long idExcursion) {
        return salidaRepository.existsByExcursionIdExcursionAndEstadoSalidaIn(idExcursion, EstadoSalida.ACTIVAS);
    }

    private ReglaNegocioException nombreDuplicado() {
        return ReglaNegocioException.enCampo("nombre",
                "Ya existe una excursión con ese nombre (activa o inactiva).");
    }

    private void aplicarParametrosOperativos(Excursion e, ExcursionRequest datos) {
        e.setDificultad(datos.dificultad());
        e.setDuracion(datos.duracion());
        e.setPrecio(datos.precio());
        e.setRecorridoBase(datos.recorridoBase().trim());
        e.setRequiereCertificado(datos.requiereCertificado());
        e.setAceptaMenores(datos.aceptaMenores());
        e.setRequiereEquipamiento(datos.requiereEquipamiento());
        e.setEquipamientoObligatorio(resolverEquipamiento(datos));
    }

    /**
     * CU-05 / flujo 6a del CU-04: si se marcó equipamiento obligatorio debe haber al menos un ítem,
     * todos del catálogo precargado y sin repetir. Si no se marcó, el listado queda vacío.
     */
    private Set<Equipamiento> resolverEquipamiento(ExcursionRequest datos) {
        if (!datos.requiereEquipamiento()) {
            return new LinkedHashSet<>();
        }
        Set<Long> ids = datos.equipamientoIds() == null ? Set.of()
                : datos.equipamientoIds().stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw ReglaNegocioException.enCampo("equipamientoIds",
                    "Marcaste equipamiento obligatorio: agregá al menos un ítem.");
        }
        List<Equipamiento> encontrados = equipamientoRepository.findAllById(ids);
        if (encontrados.size() != ids.size()) {
            throw ReglaNegocioException.enCampo("equipamientoIds",
                    "Alguno de los ítems seleccionados no existe en el catálogo de equipamiento.");
        }
        return new LinkedHashSet<>(encontrados);
    }

    /** Devuelve los parámetros operativos que cambiarían, para bloquearlos (RF-06). */
    private Map<String, String> camposOperativosModificados(Excursion e, ExcursionRequest d) {
        Map<String, String> cambios = new LinkedHashMap<>();
        if (e.getDificultad() != d.dificultad()) cambios.put("dificultad", MSG_BLOQUEADO);
        if (!Objects.equals(e.getDuracion(), d.duracion())) cambios.put("duracion", MSG_BLOQUEADO);
        if (e.getPrecio().compareTo(d.precio()) != 0) cambios.put("precio", MSG_BLOQUEADO);
        if (!e.getRecorridoBase().equals(d.recorridoBase().trim())) cambios.put("recorridoBase", MSG_BLOQUEADO);
        if (e.isRequiereCertificado() != d.requiereCertificado()) cambios.put("requiereCertificado", MSG_BLOQUEADO);
        if (e.isAceptaMenores() != d.aceptaMenores()) cambios.put("aceptaMenores", MSG_BLOQUEADO);
        if (e.isRequiereEquipamiento() != d.requiereEquipamiento()) cambios.put("requiereEquipamiento", MSG_BLOQUEADO);

        Set<Long> actuales = e.getEquipamientoObligatorio().stream()
                .map(Equipamiento::getIdEquipamiento).collect(Collectors.toSet());
        Set<Long> nuevos = d.equipamientoIds() == null || !d.requiereEquipamiento() ? Set.of()
                : d.equipamientoIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (!actuales.equals(nuevos)) cambios.put("equipamientoIds", MSG_BLOQUEADO);
        return cambios;
    }
}
