package com.pekarsky.gitcontext.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for the file processor.
 * This class is used to configure the template, exclude patterns, and output settings.
 */
@Configuration
@ConfigurationProperties(prefix = "file-processor")
public class FileProcessorConfig {
    private String template;
    private List<String> excludePatterns;
    private Output output = new Output();

    /**
     * Gets the template string used for processing files.
     *
     * @return The template string
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Sets the template string used for processing files.
     *
     * @param template The template string
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Gets the list of patterns for files to exclude from processing.
     *
     * @return The list of exclude patterns
     */
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * Sets the list of patterns for files to exclude from processing.
     *
     * @param excludePatterns The list of exclude patterns
     */
    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * Gets the output configuration.
     *
     * @return The output configuration
     */
    public Output getOutput() {
        return output;
    }

    /**
     * Sets the output configuration.
     *
     * @param output The output configuration
     */
    public void setOutput(Output output) {
        this.output = output;
    }

    /**
     * Configuration for output settings.
     */
    public static class Output {
        private String path;

        /**
         * Gets the output file path.
         *
         * @return The output file path
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets the output file path.
         *
         * @param path The output file path
         */
        public void setPath(String path) {
            this.path = path;
        }
    }
} 