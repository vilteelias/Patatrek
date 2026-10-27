package com.patatrek.backend.excursion;

import com.patatrek.backend.excepcion.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda las imágenes promocionales en disco (carpeta "uploads/excursiones")
 * y devuelve la ruta pública con la que el frontend las muestra.
 * Solo acepta JPG o PNG (se verifica el contenido real del archivo, no solo la extensión).
 */
@Service
public class ImagenExcursionService {

    static final String PREFIJO_URL = "/uploads/excursiones/";

    private final Path carpeta;

    public ImagenExcursionService(@Value("${patatrek.carpeta-uploads:uploads}") String carpetaUploads) {
        this.carpeta = Path.of(carpetaUploads, "excursiones").toAbsolutePath().normalize();
    }

    public String guardar(Long idExcursion, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ReglaNegocioException.enCampo("imagen", "Seleccioná una imagen.");
        }
        String extension = detectarFormato(archivo);
        if (extension == null) {
            throw ReglaNegocioException.enCampo("imagen", "La imagen debe ser JPG o PNG.");
        }
        String nombreArchivo = idExcursion + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        try {
            Files.createDirectories(carpeta);
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, carpeta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }
        return PREFIJO_URL + nombreArchivo;
    }

    /** Borra una imagen anterior (si era nuestra). Si falla no interrumpe la operación. */
    public void eliminar(String rutaPublica) {
        if (rutaPublica == null || !rutaPublica.startsWith(PREFIJO_URL)) return;
        Path archivo = carpeta.resolve(rutaPublica.substring(PREFIJO_URL.length())).normalize();
        if (!archivo.startsWith(carpeta)) return;
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // la imagen vieja queda huérfana, no es un error para el usuario
        }
    }

    /** Lee los primeros bytes para confirmar que es PNG o JPG. */
    private String detectarFormato(MultipartFile archivo) {
        byte[] cabecera = new byte[8];
        try (InputStream in = archivo.getInputStream()) {
            int leidos = in.readNBytes(cabecera, 0, cabecera.length);
            if (leidos >= 8 && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P'
                    && cabecera[2] == 'N' && cabecera[3] == 'G') {
                return "png";
            }
            if (leidos >= 3 && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8
                    && (cabecera[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}
