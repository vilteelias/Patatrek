package com.patatrek.backend.excursion.controlador;

import com.patatrek.backend.excursion.dto.ExcursionRequest;
import com.patatrek.backend.excursion.dto.ExcursionResponse;
import com.patatrek.backend.excursion.servicio.ExcursionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/excursiones")
public class ExcursionController {
    private final ExcursionService excursionService;

    public ExcursionController(ExcursionService excursionService) {
        this.excursionService = excursionService;
    }

    // listar excursiones -> GET /api/excursiones
    @GetMapping
    public List <ExcursionResponse> listar(@RequestParam(defaultValue = "false") boolean todas) {
        return excursionService.listar(todas);
    }

    // registrar excursion -> POST /api/excursiones
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExcursionResponse registrar(@RequestBody ExcursionRequest req) {
        return excursionService.registrar(req);
    }
}
