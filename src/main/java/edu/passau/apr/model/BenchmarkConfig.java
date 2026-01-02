package edu.passau.apr.model;

import java.util.List;

/**
 * Configuration for a benchmark.
 * Loaded from benchmark.json file.
 */
public class BenchmarkConfig {
    private String name;
    private String buggySourcePath;
    private String fixedSourcePath;
    private String testSourcePath;
    private String faultLocalizationPath;
    private String mainClassName;
    private List<String> testClassNames;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuggySourcePath() {
        return buggySourcePath;
    }

    public void setBuggySourcePath(String buggySourcePath) {
        this.buggySourcePath = buggySourcePath;
    }

    public String getFixedSourcePath() {
        return fixedSourcePath;
    }

    public void setFixedSourcePath(String fixedSourcePath) {
        this.fixedSourcePath = fixedSourcePath;
    }

    public String getTestSourcePath() {
        return testSourcePath;
    }

    public void setTestSourcePath(String testSourcePath) {
        this.testSourcePath = testSourcePath;
    }

    public String getFaultLocalizationPath() {
        return faultLocalizationPath;
    }

    public void setFaultLocalizationPath(String faultLocalizationPath) {
        this.faultLocalizationPath = faultLocalizationPath;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public List<String> getTestClassNames() {
        return testClassNames;
    }

    public void setTestClassNames(List<String> testClassNames) {
        this.testClassNames = testClassNames;
    }
}

