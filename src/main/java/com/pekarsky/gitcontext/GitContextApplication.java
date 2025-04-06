package com.pekarsky.gitcontext;

import com.pekarsky.gitcontext.config.FileProcessorConfig;
import com.pekarsky.gitcontext.processor.FileProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main application class that processes files in a directory using configurable templates and exclusion patterns.
 * The application can be run from the command line and requires a directory path as an argument.
 */
@SpringBootApplication
@EnableConfigurationProperties(FileProcessorConfig.class)
public class GitContextApplication implements CommandLineRunner {

    private final FileProcessorConfig fileProcessorConfig;

    /**
     * Creates a new GitContextApplication with the specified configuration.
     *
     * @param fileProcessorConfig The configuration for file processing
     */
    public GitContextApplication(FileProcessorConfig fileProcessorConfig) {
        this.fileProcessorConfig = fileProcessorConfig;
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GitContextApplication.class, args);
    }

    /**
     * Runs the file processing operation when the application starts.
     * Requires a directory path as the first argument.
     *
     * @param args Command line arguments
     * @throws Exception If an error occurs during processing
     */
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Please provide a directory path as an argument");
            return;
        }

        Path directory = Paths.get(args[0]);
        FileProcessor processor = new FileProcessor(
            fileProcessorConfig.getTemplate(),
            fileProcessorConfig.getExcludePatterns()
        );

        List<String> results = processor.processDirectory(directory);
        
        // Write results to file
        String outputPath = fileProcessorConfig.getOutput().getPath();
        Files.write(Paths.get(outputPath), results);
        System.out.println("Context saved to " + outputPath);
    }
} 