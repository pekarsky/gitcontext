package com.pekarsky.gitcontext.processor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A file processor that walks through a directory tree and processes files according to
 * specified template and exclusion patterns.
 */
public class FileProcessor {
    private final String template;
    private final List<String> excludePatterns;
    private final List<String> results = new ArrayList<>();

    /**
     * Creates a new FileProcessor with the specified template and exclusion patterns.
     *
     * @param template The template string to use for processing files
     * @param excludePatterns List of patterns for files to exclude from processing
     */
    public FileProcessor(String template, List<String> excludePatterns) {
        this.template = template;
        this.excludePatterns = excludePatterns;
    }

    /**
     * Processes all files in the specified directory and its subdirectories.
     *
     * @param directory The root directory to start processing from
     * @return A list of processed file contents using the template
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException If the directory does not exist
     */
    public List<String> processDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException("Directory does not exist: " + directory);
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (shouldProcessFile(file)) {
                    processFile(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to process file: " + file);
                return FileVisitResult.CONTINUE;
            }
        });

        return results;
    }

    /**
     * Determines whether a file should be processed based on exclusion patterns.
     *
     * @param file The file to check
     * @return true if the file should be processed, false otherwise
     */
    private boolean shouldProcessFile(Path file) {
        try {
            String relativePath = file.toAbsolutePath().normalize().toString();
            
            // Check if any parent directory should be excluded
            Path current = file.toAbsolutePath().normalize();
            while (current != null) {
                String pathToCheck = current.toString();
                if (excludePatterns.stream()
                        .map(this::convertWildcardToRegex)
                        .anyMatch(regex -> pathToCheck.matches(regex))) {
                    return false;
                }
                current = current.getParent();
            }

            // Check if the file is binary
            if (isBinaryFile(file)) {
                return false;
            }

            // Check the file itself
            return excludePatterns.stream()
                    .map(this::convertWildcardToRegex)
                    .noneMatch(regex -> relativePath.matches(regex));
        } catch (IOException e) {
            System.err.println("Error processing file " + file + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a file is binary by looking for null bytes.
     *
     * @param file The file to check
     * @return true if the file is binary, false otherwise
     * @throws IOException If an I/O error occurs
     */
    private boolean isBinaryFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a wildcard pattern to a regular expression.
     *
     * @param wildcard The wildcard pattern to convert
     * @return The equivalent regular expression
     */
    private String convertWildcardToRegex(String wildcard) {
        return "^" + wildcard.replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", ".") + "$";
    }

    /**
     * Processes a single file using the template.
     *
     * @param file The file to process
     * @throws IOException If an I/O error occurs
     */
    private void processFile(Path file) throws IOException {
        String content = new String(Files.readAllBytes(file));
        String fileName = file.getFileName().toString();
        String filePath = file.toString();
        long fileSize = Files.size(file);
        String fileExtension = getFileExtension(fileName);
        
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        String creationDate = formatDate(attrs.creationTime().toInstant());
        String modificationDate = formatDate(attrs.lastModifiedTime().toInstant());

        String result = template
                .replace("#file_name", fileName)
                .replace("#file_path", filePath)
                .replace("#file_size", String.valueOf(fileSize))
                .replace("#file_extension", fileExtension != null ? fileExtension : "")
                .replace("#file_creation_date", creationDate)
                .replace("#file_modification_date", modificationDate)
                .replace("#file_content", content);

        results.add(result);
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param fileName The name of the file
     * @return The file extension, or null if none exists
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : null;
    }

    /**
     * Formats an Instant as a date-time string.
     *
     * @param instant The instant to format
     * @return The formatted date-time string
     */
    private String formatDate(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
} 