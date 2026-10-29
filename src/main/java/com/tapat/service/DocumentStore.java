package com.tapat.service;

import com.tapat.exception.TapatException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class DocumentStore {
    private final Path dataDir;

    public DocumentStore(Path dataDir) {
        this.dataDir = dataDir;
    }

    public String save(int applicationId, String documentName, Path source) {
        try {
            Path directory = dataDir.resolve("uploads").resolve(String.valueOf(applicationId));
            Files.createDirectories(directory);
            String filename = slug(documentName) + extension(source);
            Path target = directory.resolve(filename);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return dataDir.relativize(target).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new TapatException("Could not store the document.", ex);
        }
    }

    public Path resolve(String storedPath) {
        return dataDir.resolve(storedPath);
    }

    private String slug(String documentName) {
        String slug = documentName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "document" : slug;
    }

    private String extension(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String extension = name.substring(dot).toLowerCase(Locale.ROOT);
        if (extension.length() > 8 || !extension.matches("\\.[a-z0-9]+")) {
            return "";
        }
        return extension;
    }
}
