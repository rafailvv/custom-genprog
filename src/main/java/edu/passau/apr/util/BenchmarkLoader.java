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
import java.util.stream.Stream;

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

            List<String> discoveredTests = discoverTestClassNames(config.getTestSourcePath());
            if (!discoveredTests.isEmpty()) {
                config.setTestClassNames(discoveredTests);
            }
            
            return config;
        }
    }

    private static List<String> discoverTestClassNames(String testSourcePath) throws IOException {
        Path testPath = Paths.get(testSourcePath);
        if (!Files.exists(testPath)) {
            return List.of();
        }

        if (Files.isRegularFile(testPath)) {
            return List.of(classNameFromFile(testPath));
        }

        List<String> classNames = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(testPath)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("Test.java"))
                .map(BenchmarkLoader::classNameFromFile)
                .sorted()
                .forEach(classNames::add);
        }
        return classNames;
    }

    private static String classNameFromFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf(".java");
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
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
}
