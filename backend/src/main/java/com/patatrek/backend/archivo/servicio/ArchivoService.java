package com.patatrek.backend.archivo.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
public class ArchivoService {
    private final Path directorioBase;

    /**busca el directorio donde se van a guardar los archivos (desde la configuracion: aplication.propierties)*/
    public ArchivoService(@Value("${patatrek.archivos.directorio}") String directorio) {
        this.directorioBase = Path.of(directorio).toAbsolutePath().normalize();
    }

    /** Guarda el archivo en una subcarpeta y devuelve el path relativo.
     * Las subcarpetas pueden ser (dependiendo de que es lo que se esta guardando):
     *      certificadosMedicos
     *      posterExcursion
     * */
    public String guardar(MultipartFile archivo, String subcarpeta) {
        try {
            Path destinoDir = directorioBase.resolve(subcarpeta);
            Files.createDirectories(destinoDir);

            String nombre = UUID.randomUUID() + extension(archivo.getOriginalFilename());
            Path destino = destinoDir.resolve(nombre);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            return subcarpeta + "/" + nombre;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    /* Valida que se esta pidiendo un archivo que sea de la carpeta base (uploads), y no de otro lado
    Devuelve el archivo para que el administrador lo abra (CU-25).
    ejemplo:
    pathRelativo = "certificadosMedicos/00874.pdf"
    */
    public Path obtener(String pathRelativo) {
        Path archivo = directorioBase.resolve(pathRelativo).normalize();
        if (!archivo.startsWith(directorioBase)) {
            throw new IllegalArgumentException("Path inválido");
        }
        return archivo;
    }

    private String extension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) return "";
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.')).toLowerCase();
    }
}
