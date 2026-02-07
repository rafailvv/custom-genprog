package edu.passau.apr;

import edu.passau.apr.algorithm.GeneticAlgorithm;
import edu.passau.apr.config.Config;
import edu.passau.apr.evaluator.FitnessEvaluator;
import edu.passau.apr.model.BenchmarkConfig;
import edu.passau.apr.model.FitnessResult;
import edu.passau.apr.model.Patch;
import edu.passau.apr.operator.PatchGenerator;
import edu.passau.apr.util.BenchmarkLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Main entry point for the Automated Program Repair tool.
 * Implements GenProg algorithm for fixing bugs in Java programs.
 */
public class Main {

    private static final double CROSS_OVER_RATE = .5;

    public static void main(String[] args) {
        try {
            Config config = parseArguments(args);
            
            if (config.getBenchmarkPath() == null) {
                printUsage();
                System.exit(1);
            }

            System.out.println("=== Automated Program Repair Tool ===");
            System.out.println("Benchmark: " + config.getBenchmarkPath());
            System.out.println("Seed: " + config.getSeed());
            System.out.println("Population Size: " + config.getPopulationSize());
            System.out.println("Max Generations: " + config.getMaxGenerations());
            System.out.println("Time Limit: " + config.getTimeLimitSec() + " seconds");
            System.out.println();

            var random = new Random(config.getSeed());

            var benchmarkConfig = BenchmarkLoader.loadConfig(config.getBenchmarkPath());
            var weights = BenchmarkLoader.loadFaultLocalization(benchmarkConfig.getFaultLocalizationPath());

            var buggySourcePath = Paths.get(benchmarkConfig.getBuggySourcePath());
            var buggySource = Files.readString(buggySourcePath);
            var patchGenerator = new PatchGenerator(buggySource, weights, config.getMutationWeight(), random);

            var fitnessEvaluator = new FitnessEvaluator(
                benchmarkConfig.getBuggySourcePath(),
                benchmarkConfig.getFixedSourcePath(),
                benchmarkConfig.getTestSourcePath(),
                benchmarkConfig.getTestClassNames(),
                config.getPositiveTestWeight(),
                config.getNegativeTestWeight(),
                benchmarkConfig.getMainClassName()
            );

            if (config.getRunTestsTarget() != null) {
                runTestsOnly(config, benchmarkConfig, fitnessEvaluator);
                return;
            }

            var ga = new GeneticAlgorithm(
                config.getPopulationSize(),
                config.getMaxGenerations(),
                config.getTimeLimitSec() * 1_000L,
                config.getMutationWeight(),
                CROSS_OVER_RATE,
                random,
                patchGenerator,
                fitnessEvaluator
            );

            System.out.println("Starting genetic algorithm...");
            GeneticAlgorithm.AlgorithmResult result = ga.run();
            System.out.println();
            System.out.println("=== Results ===");
            System.out.println("Generations: " + result.generations());
            System.out.println("Time: " + (result.elapsedTimeMs() / 1000.0) + " seconds");
            
            if (result.foundSolution()) {
                System.out.println("SUCCESS: Found a patch that passes all tests!");
                System.out.println();
                System.out.println("Patch:");
                System.out.println(result.bestPatch().toString());
                
                savePatchedFile(config.getBenchmarkPath(), benchmarkConfig, result.bestPatch());
            } else {
                System.out.println("No solution found within limits.");
                if (result.bestFitness() != null) {
                    System.out.println("Best fitness: " + result.bestFitness());
                    System.out.println("Best Patch:");
                    System.out.println(result.bestPatch().toString());

                    savePatchedFile(config.getBenchmarkPath(), benchmarkConfig, result.bestPatch());
                }
            }

            fitnessEvaluator.cleanup();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (args.length > 0 && args[0].equals("--verbose")) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void runTestsOnly(Config config, BenchmarkConfig benchmarkConfig, FitnessEvaluator fitnessEvaluator)
        throws IOException {
        String target = config.getRunTestsTarget().trim().toLowerCase();
        String sourcePath;
        if ("buggy".equals(target)) {
            sourcePath = benchmarkConfig.getBuggySourcePath();
        } else if ("fixed".equals(target)) {
            sourcePath = benchmarkConfig.getFixedSourcePath();
        } else {
            throw new IllegalArgumentException("--runTests must be one of: buggy, fixed");
        }

        Path path = Paths.get(sourcePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source file does not exist: " + sourcePath);
        }

        System.out.println("=== Test Execution Only ===");
        System.out.println("Variant: " + target);
        System.out.println("Source: " + sourcePath);
        System.out.println();

        String src = Files.readString(path);
        FitnessResult result = fitnessEvaluator.evaluate(src);
        System.out.printf("Compiles: %s%n", result.compiles());
        System.out.printf("Passing: %d%n", result.passingTests());
        System.out.printf("Failing: %d%n", result.failingTests());
        System.out.printf("Total: %d%n", result.totalTests());
        System.out.printf("AllPass: %s%n", result.allTestsPass());
        System.out.println();

        fitnessEvaluator.cleanup();
        if (!result.allTestsPass()) {
            System.exit(2);
        }
    }

    private static Config parseArguments(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--benchmark":
                    if (i + 1 < args.length) {
                        config.setBenchmarkPath(args[++i]);
                    }
                    break;
                case "--seed":
                    if (i + 1 < args.length) {
                        config.setSeed(Long.parseLong(args[++i]));
                    }
                    break;
                case "--maxGenerations":
                    if (i + 1 < args.length) {
                        config.setMaxGenerations(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--timeLimitSec":
                    if (i + 1 < args.length) {
                        config.setTimeLimitSec(Long.parseLong(args[++i]));
                    }
                    break;
                case "--populationSize":
                    if (i + 1 < args.length) {
                        config.setPopulationSize(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--positiveTestWeight":
                    if (i + 1 < args.length) {
                        config.setPositiveTestWeight(Double.parseDouble(args[++i]));
                    }
                    break;
                case "--negativeTestWeight":
                    if (i + 1 < args.length) {
                        config.setNegativeTestWeight(Double.parseDouble(args[++i]));
                    }
                    break;
                case "--mutationWeight":
                    if (i + 1 < args.length) {
                        config.setMutationWeight(Double.parseDouble(args[++i]));
                    }
                    break;
                case "--runTests":
                    if (i + 1 < args.length) {
                        config.setRunTestsTarget(args[++i]);
                    } else {
                        throw new IllegalArgumentException("--runTests requires an argument: buggy|fixed");
                    }
                    break;
                case "--verbose":
                    config.setVerbose(true);
                    break;
            }
        }

        return config;
    }

    private static void printUsage() {
        String usage = """
            Usage: java -jar apr-tool.jar [options]
            
            Required options:
              --benchmark <path>     Path to benchmark directory
            
            Optional options:
              --seed <number>        Random seed (default: current time)
              --maxGenerations <n>   Maximum generations (default: 50)
              --timeLimitSec <n>     Time limit in seconds (default: 60)
              --populationSize <n>   Population size (default: 40)
              --positiveTestWeight <w>  Weight for passing tests (default: 1.0)
              --negativeTestWeight <w>  Weight for failing tests (default: 10.0)
              --mutationWeight <w>   Mutation weight (default: 0.06)
              --runTests <buggy|fixed>  Only compile+run tests for buggy/fixed version
              --verbose              Enable verbose output
            """;
        System.out.println(usage);
    }

    private static void savePatchedFile(String benchmarkPath, BenchmarkConfig benchmarkConfig, Patch patch) {
        try {
            String patchedSource = patch.getCompilationUnit().toString();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path outputDir = Paths.get("out", Paths.get(benchmarkPath).getFileName().toString(),
                                       "patch_" + timestamp);
            Files.createDirectories(outputDir);

            Path patchedFile = outputDir.resolve(benchmarkConfig.getMainClassName() + ".java");
            Files.writeString(patchedFile, patchedSource);

            System.out.println("Patched file saved to: " + patchedFile);

        } catch (IOException e) {
            System.err.println("Warning: Could not save patched file: " + e.getMessage());
        }
    }
}
