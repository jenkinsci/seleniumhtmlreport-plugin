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
import java.util.regex.Pattern;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Marco Machmer
 */
public class SeleniumHtmlReportPublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 28042011L;
    
    private String SELENIUM_REPORTS_TARGET = "seleniumReports";

    private final String testResultsDir;

    private boolean failureIfExceptionOnParsingResultFiles = true;

    /**
     * 
     * @param testResults
     * @stapler-constructor
     */
    @DataBoundConstructor
    public SeleniumHtmlReportPublisher(final String testResultsDir, final boolean failureIfExceptionOnParsingResultFiles) {
        super();
        this.testResultsDir = testResultsDir;
        this.failureIfExceptionOnParsingResultFiles = failureIfExceptionOnParsingResultFiles;
    }

    public String getTestResultsDir() {
        return testResultsDir;
    }

    public boolean getFailureIfExceptionOnParsingResultFiles() {
        return this.failureIfExceptionOnParsingResultFiles;
    }

    public boolean isFailureIfExceptionOnParsingResultFiles() {
        return this.failureIfExceptionOnParsingResultFiles;
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
        ResultTuple resultTpl = createResults(build, listener);
        SeleniumHtmlReportAction action = new SeleniumHtmlReportAction(build, listener, resultTpl.results, getSeleniumReportsDir(build));
        build.getActions().add(action);
        if (resultTpl.exceptionWhileParsing && this.failureIfExceptionOnParsingResultFiles) {
            listener.getLogger().println("Set result to FAILURE");
            build.setResult(Result.FAILURE);
        } else {
            calculateResultState(build, resultTpl.results, listener);
        }
        return true;
    }

    private void copyReports(FilePath seleniumResults, FilePath target, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Copying the reports.");
        seleniumResults.copyRecursiveTo(target);
    }

    private ResultTuple createResults(AbstractBuild<?,?> build, BuildListener listener) throws IOException {
        List<TestResult> results = new ArrayList<TestResult>();
        ResultTuple resultTpl = new ResultTuple(false, results);
        FileSet fs = Util.createFileSet(getSeleniumReportsDir(build), "**/*.html");
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
        if (files.length == 0) {
            return resultTpl;
        }
        for (String selfile : files) {
            try{
                results.add(TestResult.parse(build, listener, selfile, getSeleniumReportsDir(build)));
            } catch (Exception e) {
                listener.getLogger().println("Unable to parse " + selfile + ": " + e);
                resultTpl.exceptionWhileParsing = true;
            }
        }
        return resultTpl;
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
    protected File getSeleniumReportsDir(AbstractBuild<?,?> build) {
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

        public FormValidation doCheckTestResultsDir(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Missing tests results location relative to your workspace");
            }
            if(isAbsolute(value)) {
                return FormValidation.error("Please give a results location relative to your workspace");
            }
            return FormValidation.ok();
        }

        private static boolean isAbsolute(String rel) {
            return rel.startsWith("/") || DRIVE_PATTERN.matcher(rel).matches();
        }

        private static final Pattern DRIVE_PATTERN = Pattern.compile("[A-Za-z]:[\\\\/].*"),
            ABSOLUTE_PREFIX_PATTERN = Pattern.compile("^(\\\\\\\\|(?:[A-Za-z]:)?[\\\\/])[\\\\/]*");

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    private class ResultTuple {
        boolean exceptionWhileParsing = false;
        List<TestResult> results;

        public ResultTuple(boolean exceptionWhileParsing, List<TestResult> results) {
            super();
            this.exceptionWhileParsing = exceptionWhileParsing;
            this.results = results;
        }
    }
}
