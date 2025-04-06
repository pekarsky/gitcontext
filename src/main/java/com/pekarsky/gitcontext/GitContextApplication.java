package com.pekarsky.gitcontext;

import com.pekarsky.gitcontext.config.FileProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class GitContextApplication implements CommandLineRunner {

    @Autowired
    private FileProcessorConfig fileProcessorConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    public static void main(String[] args) {
        SpringApplication.run(GitContextApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please provide a path as an argument");
            return;
        }

        String path = args[0];
        Path startPath = Paths.get(path);

        if (!Files.exists(startPath)) {
            System.out.println("Path does not exist: " + path);
            return;
        }

        String template = loadTemplate();
        processFiles(startPath, template);
    }

    private String loadTemplate() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:template.txt");
        return new String(resource.getInputStream().readAllBytes());
    }

    private void processFiles(Path startPath, String template) throws IOException {
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (shouldProcessFile(file)) {
                    processFile(file, template);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to process file: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldProcessFile(Path file) {
        try {
            String relativePath = file.toAbsolutePath().normalize().toString();
            
            // Check if any parent directory should be excluded
            Path current = file.toAbsolutePath().normalize();
            while (current != null) {
                String pathToCheck = current.toString();
                if (fileProcessorConfig.getExcludePatterns().stream()
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
            return fileProcessorConfig.getExcludePatterns().stream()
                    .map(this::convertWildcardToRegex)
                    .noneMatch(regex -> relativePath.matches(regex));
        } catch (IOException e) {
            System.err.println("Error processing file " + file + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isBinaryFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private String convertWildcardToRegex(String wildcard) {
        return "^" + wildcard.replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", ".") + "$";
    }

    private void processFile(Path file, String template) throws IOException {
        String content = new String(Files.readAllBytes(file));
        String fileName = file.getFileName().toString();
        String filePath = file.toString();
        long fileSize = Files.size(file);
        String fileExtension = StringUtils.getFilenameExtension(fileName);
        
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

        System.out.println(result);
    }

    private String formatDate(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
} 