package org.jvnet.hudson.plugins.seleniumhtmlreport;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Marco Machmer
 */
public class SeleniumHtmlReportPublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String SELENIUM_REPORTS_TARGET = "seleniumReports";

    /**
     * {@link FileSet} "includes" string, like "foo/bar/*.html"
     */
    private final String testResultsDir;

    /**
     * 
     * @param testResults
     * @stapler-constructor
     */
    @DataBoundConstructor
    public SeleniumHtmlReportPublisher(final String testResultsDir) {
        super();
        this.testResultsDir = testResultsDir;
    }

    public String getTestResultsDir() {
        return testResultsDir;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Publishing Selenium report...");
        FilePath seleniumResults = build.getWorkspace().child(this.testResultsDir);
        if (!seleniumResults.exists()) {
            listener.getLogger().println("Missing directory " + this.testResultsDir);
            return false;
        }
        if (seleniumResults.list().isEmpty()) {
            listener.getLogger().println("Missing selenium result files in directory " + this.testResultsDir);
            return false;
        }
        FilePath target = new FilePath(getSeleniumReportsDir(build));
        copyReports(seleniumResults, target, listener);
        List<TestResult> results = createResults(build, listener);
        SeleniumHtmlReportAction action = new SeleniumHtmlReportAction(build, listener, results);
        build.getActions().add(action);
        calculateResultState(build, results, listener);
        return true;
    }

    private void copyReports(FilePath seleniumResults, FilePath target, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Copying the reports.");
        seleniumResults.copyRecursiveTo(target);
    }

    private List<TestResult> createResults(AbstractBuild<?,?> build, BuildListener listener) throws IOException {
        List<TestResult> results = new ArrayList<TestResult>();
        FileSet fs = Util.createFileSet(getSeleniumReportsDir(build), "**/*");
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
        if (files.length == 0) {
            return results;
        }
        for (String selfile : files) {
            results.add(TestResult.parse(build, listener, selfile));
        }
        return results;
    }

    private void calculateResultState(AbstractBuild<?,?> build, List<TestResult> results, BuildListener listener) {
        if (Result.ABORTED == build.getResult() || Result.FAILURE == build.getResult()) {
            return;
        }
        for (TestResult result : results) {
            if (result.getNumCommandFailures() > 0) {
                listener.getLogger().println("Set result to UNSTABLE");
                build.setResult(Result.UNSTABLE);
            }
            if (result.getNumCommandErrors() > 0) {
                listener.getLogger().println("Set result to FAILURE");
                build.setResult(Result.FAILURE);
                break;
            }
        }
    }
    
    /**
     * Gets the directory where the latest selenium reports are stored for the
     * given build.
     */
    protected static File getSeleniumReportsDir(AbstractBuild<?,?> build) {
        return new File(build.getRootDir(), SELENIUM_REPORTS_TARGET);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(SeleniumHtmlReportPublisher.class);
        }

        public String getDisplayName() {
            return "Publish Selenium Html Report";
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public FormValidation doCheck(@AncestorInPath AbstractProject project,
                @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
