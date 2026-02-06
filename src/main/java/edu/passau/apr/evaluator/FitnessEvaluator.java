package edu.passau.apr.evaluator;

import edu.passau.apr.model.FitnessResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Evaluates fitness of a patch by compiling the modified code
 * and running tests against it.
 */
public class FitnessEvaluator {

    private record CompilationResult(boolean success, String classPath) {}
    private record TestExecutionResult(int passingCount, int failingCount, int totalCount,
                                       Set<String> passedTests, Set<String> discoveredTests) {}
    // End-to-end evaluation should be long enough for compile + full suite execution.
    private static final int EVALUATION_TIMEOUT_SEC = 30;
    private static final int COMPILATION_TIMEOUT_SEC = 5;
    private static final int TEST_TIMEOUT_SEC = 2;
    private static final Object STD_IO_LOCK = new Object();

    private final String buggySourcePath;
    private final String fixedSourcePath;
    private final String testSourcePath;
    private final List<String> testClassNames;
    private final double positiveTestWeight;
    private final double negativeTestWeight;
    private final Path tempDir;
    private final String mainClassName;
    private final Path testClassesDir; // Pre-compiled test classes
    private Set<String> positiveTestIds = Set.of();
    private Set<String> negativeTestIds = Set.of();
    private Set<String> allTestIds = Set.of();

    public FitnessEvaluator(String buggySourcePath, String fixedSourcePath, String testSourcePath, 
                           List<String> testClassNames,
                           double positiveTestWeight, double negativeTestWeight,
                           String mainClassName) throws IOException {
        this.buggySourcePath = buggySourcePath;
        this.fixedSourcePath = fixedSourcePath;
        this.testSourcePath = testSourcePath;
        this.testClassNames = new ArrayList<>(testClassNames);
        this.positiveTestWeight = positiveTestWeight;
        this.negativeTestWeight = negativeTestWeight;
        this.mainClassName = mainClassName;
        this.tempDir = Files.createTempDirectory("apr-eval-");
        this.testClassesDir = tempDir.resolve("test-classes");
        Files.createDirectories(testClassesDir);
        
        precompileTests();
        initializeFitnessPartitions();
    }
    
    private void precompileTests() {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) return;
            
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(testClassesDir.toFile()));
            
            List<File> allFiles = new ArrayList<>();
            File sourceFile = new File(buggySourcePath);
            if (fixedSourcePath != null && new File(fixedSourcePath).exists()) {
                sourceFile = new File(fixedSourcePath);
            }
            allFiles.add(sourceFile);
            Path testPath = Paths.get(testSourcePath);
            if (Files.isDirectory(testPath)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(testPath)) {
                    stream.filter(Files::isRegularFile)
                          .filter(p -> p.toString().endsWith(".java"))
                          .forEach(p -> allFiles.add(p.toFile()));
                }
            } else if (Files.isRegularFile(testPath)) {
                allFiles.add(testPath.toFile());
            }
            
            if (!allFiles.isEmpty()) {
                String systemClasspath = System.getProperty("java.class.path");
                List<String> options = new ArrayList<>();
                if (systemClasspath != null && !systemClasspath.isEmpty()) {
                    options.add("-cp");
                    options.add(systemClasspath);
                }

                // Candidate compilation failures are expected during search; keep evaluator output quiet.
                PrintWriter silentOutput = new PrintWriter(new StringWriter());
                JavaCompiler.CompilationTask task = compiler.getTask(
                    silentOutput, fileManager, null, options, null,
                    fileManager.getJavaFileObjectsFromFiles(allFiles)
                );
                task.call();
                silentOutput.close();
            }
            
            fileManager.close();
        } catch (Exception e) {
        }
    }

    private void initializeFitnessPartitions() {
        try {
            CompilationResult baselineCompile = compile(new File(buggySourcePath), testSourcePath);
            if (!baselineCompile.success) {
                return;
            }

            TestExecutionResult baseline = runTestsSilenced(baselineCompile.classPath);
            if (baseline.discoveredTests.isEmpty()) {
                return;
            }

            Set<String> discovered = new HashSet<>(baseline.discoveredTests);
            Set<String> positives = new HashSet<>(baseline.passedTests);
            Set<String> negatives = new HashSet<>(discovered);
            negatives.removeAll(positives);

            this.allTestIds = Set.copyOf(discovered);
            this.positiveTestIds = Set.copyOf(positives);
            this.negativeTestIds = Set.copyOf(negatives);
        } catch (Exception ignored) {
        }
    }

    /**
     * Applies a patch to the source code and evaluates its fitness.
     */
    public FitnessResult evaluate(String patchedSource) {
        ExecutorService executor = newDaemonSingleThreadExecutor("apr-eval");
        Future<FitnessResult> future = executor.submit(() -> {
            try {
                String fileName = mainClassName + ".java";
                Path modifiedSourceFile = tempDir.resolve(fileName);

                Files.writeString(modifiedSourceFile, patchedSource);
                CompilationResult compileResult = compile(modifiedSourceFile.toFile(), testSourcePath);

                if (!compileResult.success) {
                    return new FitnessResult(0, 0, 0, 0.0, false, false);
                }

                TestExecutionResult testResult = runTestsSilenced(compileResult.classPath);

                int positivePassed = countIntersection(testResult.passedTests, positiveTestIds);
                int negativePassed = countIntersection(testResult.passedTests, negativeTestIds);
                double fitness = calculateFitness(positivePassed, negativePassed);
                boolean allPass = testResult.failingCount == 0 && testResult.totalCount > 0 && testResult.passingCount > 0;

                return new FitnessResult(
                    testResult.passingCount,
                    testResult.failingCount,
                    testResult.totalCount,
                    fitness,
                    true,
                    allPass
                );

            } catch (Exception e) {
                return new FitnessResult(0, 0, 0, 0.0, false, false);
            }
        });

        try {
            return future.get(EVALUATION_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return new FitnessResult(0, 0, 0, 0.0, false, false);
        } catch (Exception e) {
            return new FitnessResult(0, 0, 0, 0.0, false, false);
        } finally {
            executor.shutdownNow();
        }
    }

    private CompilationResult compile(File sourceFile, String testSourcePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompilationResult(false, null);
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        
        try {
            Path outputDir = tempDir.resolve("classes");
            Files.createDirectories(outputDir);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            List<File> sourceFiles = new ArrayList<>();
            sourceFiles.add(sourceFile);
            
            // Suppress javac diagnostics for transient mutants to avoid log flooding.
            PrintWriter silentOutput = new PrintWriter(new StringWriter());
            JavaCompiler.CompilationTask sourceTask = compiler.getTask(
                silentOutput, fileManager, null, null, null,
                fileManager.getJavaFileObjectsFromFiles(sourceFiles)
            );

            ExecutorService executor = newDaemonSingleThreadExecutor("apr-compile");
            Future<Boolean> future = executor.submit(sourceTask);
            
            boolean sourceSuccess = false;
            try {
                sourceSuccess = future.get(COMPILATION_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception ignored) {
            } finally {
                silentOutput.close();
                executor.shutdownNow();
            }
            
            fileManager.close();
            
            if (!sourceSuccess) {
                return new CompilationResult(false, null);
            }

            String classpath = outputDir + ":" + testClassesDir.toString();
            return new CompilationResult(true, classpath);

        } catch (IOException e) {
            return new CompilationResult(false, null);
        }
    }

    private TestExecutionResult runTests(String classPath) {
        try {
            return runTestsWithJUnitLauncher(classPath);
        } catch (Exception e) {
            return new TestExecutionResult(0, 0, 0, Set.of(), Set.of());
        }
    }

    private TestExecutionResult runTestsSilenced(String classPath) {
        synchronized (STD_IO_LOCK) {
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
            try {
                System.setOut(silent);
                System.setErr(silent);
                return runTests(classPath);
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
                silent.close();
            }
        }
    }

    private TestExecutionResult runTestsWithJUnitLauncher(String classPath) {
        // Use reflection-based approach as JUnit Platform Launcher requires more setup
        return runTestsWithReflection(classPath);
    }

    private TestExecutionResult runTestsWithReflection(String classPath) {
        int passingCount = 0;
        int failingCount = 0;
        int totalCount = 0;

        URLClassLoader classLoader = null;
        try {
            String systemClasspath = System.getProperty("java.class.path");
            List<URL> urls = new ArrayList<>();
            
            String[] classPathEntries = classPath.split(java.util.regex.Pattern.quote(File.pathSeparator));
            for (String entry : classPathEntries) {
                if (!entry.isEmpty()) {
                    try {
                        File f = new File(entry);
                        if (f.exists()) {
                            urls.add(f.toURI().toURL());
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (systemClasspath != null) {
                String[] entries = systemClasspath.split(System.getProperty("path.separator", ":"));
                for (String entry : entries) {
                    if (!entry.isEmpty()) {
                        try {
                            File f = new File(entry);
                            if (f.exists()) {
                                urls.add(f.toURI().toURL());
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            
            if (urls.isEmpty()) {
                return new TestExecutionResult(0, 0, 0, Set.of(), Set.of());
            }
            
            classLoader = new URLClassLoader(urls.toArray(new URL[0]), null);
            Set<String> passedTests = new HashSet<>();
            Set<String> discoveredTests = new HashSet<>();

            for (String testClassName : testClassNames) {
                try {
                    Class<?> testClass = classLoader.loadClass(testClassName);
                    
                    // Find and run test methods
                    java.lang.reflect.Method[] methods = testClass.getDeclaredMethods();
                    Arrays.sort(methods, Comparator.comparing(java.lang.reflect.Method::getName));
                    for (java.lang.reflect.Method method : methods) {
                        // Check for @Test annotation by name (to avoid import issues)
                        boolean hasTestAnnotation = false;
                        try {
                            for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
                                if (ann.annotationType().getSimpleName().equals("Test")) {
                                    hasTestAnnotation = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                        }
                        
                        if (hasTestAnnotation) {
                            String testId = testClassName + "#" + method.getName();
                            discoveredTests.add(testId);
                            totalCount++;
                            try {
                                java.lang.reflect.Constructor<?> constructor = testClass.getDeclaredConstructor();
                                constructor.setAccessible(true);
                                Object testInstance = constructor.newInstance();

                                method.setAccessible(true);
                                
                                ExecutorService executor = newDaemonSingleThreadExecutor("apr-test");
                                Future<?> future = executor.submit(() -> {
                                    try {
                                        method.invoke(testInstance);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                
                                try {
                                    future.get(TEST_TIMEOUT_SEC, TimeUnit.SECONDS);
                                    passingCount++;
                                    passedTests.add(testId);
                                } catch (TimeoutException e) {
                                    future.cancel(true);
                                    failingCount++;
                                } catch (ExecutionException e) {
                                    failingCount++;
                                } finally {
                                    executor.shutdownNow();
                                }
                            } catch (Exception e) {
                                failingCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }

            if (allTestIds.isEmpty() && !discoveredTests.isEmpty()) {
                allTestIds = Set.copyOf(discoveredTests);
            }

            return new TestExecutionResult(
                passingCount,
                failingCount,
                totalCount,
                Set.copyOf(passedTests),
                Set.copyOf(discoveredTests)
            );
        } catch (Exception e) {
            return new TestExecutionResult(0, 0, 0, Set.of(), Set.of());
        } finally {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private double calculateFitness(int positivePassedTests, int negativePassedTests) {
        return positiveTestWeight * positivePassedTests + negativeTestWeight * negativePassedTests;
    }

    private int countIntersection(Set<String> passedTests, Set<String> referenceSet) {
        if (passedTests.isEmpty()) {
            return 0;
        }
        if (referenceSet.isEmpty()) {
            if (allTestIds.isEmpty()) {
                return passedTests.size();
            }
            return 0;
        }

        int count = 0;
        for (String testId : passedTests) {
            if (referenceSet.contains(testId)) {
                count++;
            }
        }
        return count;
    }

    private ExecutorService newDaemonSingleThreadExecutor(String namePrefix) {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadExecutor(threadFactory);
    }


    public void cleanup() {
        try {
            Files.walk(tempDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                    }
                });
        } catch (IOException e) {
        }
    }
}
