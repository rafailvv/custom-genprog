package edu.passau.apr;

import edu.passau.apr.algorithm.GeneticAlgorithm;
import edu.passau.apr.config.Config;
import edu.passau.apr.evaluator.FitnessEvaluator;
import edu.passau.apr.model.BenchmarkConfig;
import edu.passau.apr.model.Patch;
import edu.passau.apr.model.StatementWeight;
import edu.passau.apr.operator.PatchGenerator;
import edu.passau.apr.selection.SelectionOperator;
import edu.passau.apr.util.BenchmarkLoader;
import edu.passau.apr.util.WeightedPathSelector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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

            BenchmarkConfig benchmarkConfig = BenchmarkLoader.loadConfig(config.getBenchmarkPath());
            List<String> sourceLines = BenchmarkLoader.readSourceFile(benchmarkConfig.getBuggySourcePath());
            List<StatementWeight> weights = BenchmarkLoader.loadFaultLocalization(
                benchmarkConfig.getFaultLocalizationPath());

            Random random = new Random(config.getSeed());
            WeightedPathSelector pathSelector = new WeightedPathSelector(weights, random);
            FitnessEvaluator fitnessEvaluator = new FitnessEvaluator(
                benchmarkConfig.getBuggySourcePath(),
                benchmarkConfig.getFixedSourcePath(),
                benchmarkConfig.getTestSourcePath(),
                benchmarkConfig.getTestClassNames(),
                config.getPositiveTestWeight(),
                config.getNegativeTestWeight(),
                benchmarkConfig.getMainClassName()
            );

            PatchGenerator patchGenerator = new PatchGenerator(
                random, pathSelector, sourceLines,
                config.getMutationWeight(), 3
            );

            SelectionOperator selectionOperator = new SelectionOperator(random, 3);

            GeneticAlgorithm ga = new GeneticAlgorithm(
                config.getPopulationSize(),
                config.getMaxGenerations(),
                config.getTimeLimitSec() * 1_000L,
                config.getMutationWeight(),
                CROSS_OVER_RATE,
                random,
                patchGenerator,
                fitnessEvaluator,
                selectionOperator,
                sourceLines
            );

            System.out.println("Starting genetic algorithm...");
            GeneticAlgorithm.AlgorithmResult result = ga.run();
            System.out.println();
            System.out.println("=== Results ===");
            System.out.println("Generations: " + result.getGenerations());
            System.out.println("Time: " + (result.getElapsedTimeMs() / 1000.0) + " seconds");
            
            if (result.foundSolution()) {
                System.out.println("SUCCESS: Found a patch that passes all tests!");
                System.out.println();
                System.out.println("Patch:");
                System.out.println(result.getBestPatch().toString());
                
                savePatchedFile(config.getBenchmarkPath(), benchmarkConfig, sourceLines, result.getBestPatch());
            } else {
                System.out.println("No solution found within limits.");
                if (result.getBestFitness() != null) {
                    System.out.println("Best fitness: " + result.getBestFitness().toString());
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
              --verbose              Enable verbose output
            """;
        System.out.println(usage);
    }

    private static void savePatchedFile(String benchmarkPath, BenchmarkConfig benchmarkConfig,
                                       List<String> originalLines, Patch patch) {
        try {
            List<String> patchedLines = patch.applyTo(originalLines);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path outputDir = Paths.get("out", Paths.get(benchmarkPath).getFileName().toString(), 
                                       "patch_" + timestamp);
            Files.createDirectories(outputDir);

            Path patchedFile = outputDir.resolve(benchmarkConfig.getMainClassName() + ".java");
            Files.write(patchedFile, patchedLines);

            System.out.println("Patched file saved to: " + patchedFile);

        } catch (IOException e) {
            System.err.println("Warning: Could not save patched file: " + e.getMessage());
        }
    }
}

