package com.patatrek.backend.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte las excepciones en respuestas JSON con el formato RespuestaError. */
@RestControllerAdvice
public class ManejadorErrores {

    /** Datos obligatorios incompletos o inválidos (flujo alternativo 4a del CU-04). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new RespuestaError("Revisá los datos marcados antes de continuar.", errores));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<RespuestaError> datosInvalidosServicio(DatosInvalidosException ex) {
        return ResponseEntity.badRequest().body(new RespuestaError(ex.getMessage(), ex.getErrores()));
    }

    /** Un filtro con formato inválido en la URL (ej. ?estado=XYZ o ?desde=31-12-2026). */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> parametroInvalido(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new RespuestaError(
                "El valor de \"" + ex.getName() + "\" no es válido.", Map.of(ex.getName(), "Valor no válido.")));
    }

    /** Falta un parámetro obligatorio en la URL (ej. ?reserva=…&dni=…). */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> parametroFaltante(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new RespuestaError(
                "Falta el dato \"" + ex.getParameterName() + "\".", Map.of(ex.getParameterName(), "Dato obligatorio.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new RespuestaError("El formato de los datos enviados no es válido.", Map.of()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespuestaError(ex.getMessage(), ex.getErrores()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> credencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new RespuestaError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(AccesoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespuestaError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RespuestaError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> archivoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new RespuestaError("El archivo no puede superar los 5 MB.", Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaError> argumentoIlegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new RespuestaError(ex.getMessage(), Map.of()));
    }
}
