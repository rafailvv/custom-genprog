package edu.passau.apr.util;

import com.google.gson.Gson;
import edu.passau.apr.model.BenchmarkConfig;
import edu.passau.apr.model.StatementWeight;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for loading benchmark configuration and fault localization data.
 */
public class BenchmarkLoader {
    private static final Gson gson = new Gson();

    /**
     * Loads benchmark configuration from benchmark.json file.
     */
    public static BenchmarkConfig loadConfig(String benchmarkPath) throws IOException {
        Path configPath = Paths.get(benchmarkPath, "benchmark.json");
        if (!Files.exists(configPath)) {
            throw new IOException("benchmark.json not found in " + benchmarkPath);
        }

        try (FileReader reader = new FileReader(configPath.toFile())) {
            BenchmarkConfig config = gson.fromJson(reader, BenchmarkConfig.class);
            
            // Resolve relative paths
            Path basePath = Paths.get(benchmarkPath);
            config.setBuggySourcePath(basePath.resolve(config.getBuggySourcePath()).toString());
            config.setFixedSourcePath(basePath.resolve(config.getFixedSourcePath()).toString());
            config.setTestSourcePath(basePath.resolve(config.getTestSourcePath()).toString());
            config.setFaultLocalizationPath(basePath.resolve(config.getFaultLocalizationPath()).toString());
            
            return config;
        }
    }

    /**
     * Loads fault localization weights from JSON file.
     * Expected format: [{"lineNumber": 5, "weight": 1.0}, ...]
     */
    public static Map<Integer, Double> loadFaultLocalization(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Fault localization file not found: " + filePath);
        }

        try (FileReader reader = new FileReader(path.toFile())) {
            StatementWeight[] weights = gson.fromJson(reader, StatementWeight[].class);
            Map<Integer, Double> weightMap = new HashMap<>();
            Arrays.asList(weights).forEach(sw -> weightMap.put(sw.getLineNumber(), sw.getWeight()));
            return weightMap;
        }
    }

    /**
     * Reads source code file into a list of lines.
     */
    public static List<String> readSourceFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllLines(path);
    }

    /**
     * Writes source code lines to a file.
     */
    public static void writeSourceFile(String filePath, List<String> lines) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, lines);
    }
}

