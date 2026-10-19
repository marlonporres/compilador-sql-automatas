package ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Localiza los ejemplos desde el proyecto o el paquete, sin depender de la terminal. */
final class RecursosProyecto {
    private RecursosProyecto() { }

    static String leerEjemplo(String nombre) throws IOException {
        try (InputStream recurso = RecursosProyecto.class.getResourceAsStream("/tests/" + nombre)) {
            if (recurso != null) return new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (Path carpeta : carpetasCandidatas()) {
            Path archivo = carpeta.resolve("tests").resolve(nombre);
            if (Files.isRegularFile(archivo)) return Files.readString(archivo, StandardCharsets.UTF_8);
        }
        throw new IOException("No se encontró tests/" + nombre
                + ". Abre la carpeta completa del proyecto o conserva la carpeta tests junto a SQLStudio.jar.");
    }

    static Path carpetaEjemplos() {
        for (Path carpeta : carpetasCandidatas()) {
            if (Files.isDirectory(carpeta.resolve("tests"))) return carpeta.resolve("tests");
        }
        return Path.of(System.getProperty("user.home", "."));
    }

    private static Set<Path> carpetasCandidatas() {
        Set<Path> carpetas = new LinkedHashSet<>();
        try {
            java.security.CodeSource codigo = RecursosProyecto.class.getProtectionDomain().getCodeSource();
            if (codigo != null && codigo.getLocation() != null) {
                Path ubicacion = Path.of(codigo.getLocation().toURI());
                agregarPadres(carpetas, Files.isDirectory(ubicacion) ? ubicacion : ubicacion.getParent());
            }
        } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
            // Un cargador sin ubicación en disco todavía puede proporcionar recursos empaquetados.
        }
        agregarPadres(carpetas, Path.of("").toAbsolutePath());
        return carpetas;
    }

    private static void agregarPadres(Set<Path> carpetas, Path inicio) {
        Path actual = inicio;
        for (int nivel = 0; actual != null && nivel < 8; nivel++, actual = actual.getParent()) {
            carpetas.add(actual.toAbsolutePath().normalize());
        }
    }
}
