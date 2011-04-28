package org.jvnet.hudson.plugins.seleniumhtmlreport;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DirectoryBrowserSupport;
import java.io.File;

import java.io.Serializable;
import java.util.List;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Marco Machmer
 */
public class SeleniumHtmlReportAction implements Action, Serializable {

    public final AbstractBuild<?, ?> build;
    private final List<TestResult> results;
    private final File seleniumReportsDir;

    public SeleniumHtmlReportAction(AbstractBuild<?, ?> build, BuildListener listener, List<TestResult> results, File seleniumReportsDir) {
        super();
        this.build = build;
        this.results = results;
        this.seleniumReportsDir = seleniumReportsDir;
    }

    public String getIconFileName() {
        return "/plugin/seleniumhtmlreport/icons/sla-48x48.png";
    }

    public String getDisplayName() {
        return "Selenium Html Report";
    }

    public String getUrlName() {
        return "seleniumhtmlreport";
    }

    public AbstractBuild<?, ?>getOwner() {
        return this.build;
    }

    public List<TestResult> getResults() {
        return this.results;
    }

    public int getSumTestPasses() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumTestPasses();
            }
        });
    }

    protected static interface TestResultValueProvider {
        public int getValueOf(TestResult result);
    }

    protected int calculateSumOf(TestResultValueProvider values) {
        int sum = 0;
        for (TestResult r : this.results) {
            sum = sum + values.getValueOf(r);
        }
        return sum;
    }

    public int getSumTestFailures() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumTestFailures();
            }
        });
    }

    public int getSumCommandPasses() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumCommandPasses();
            }
        });
    }

    public int getSumCommandFailures() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumCommandFailures();
            }
        });
    }

    public int getSumCommandErrors() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumCommandErrors();
            }
        });
    }

    public int getSumTestTotal() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getNumTestTotal();
            }
        });
    }

    public int getTotalTime() {
        return calculateSumOf(new TestResultValueProvider() {
            public int getValueOf(final TestResult result) {
                return result.getTotalTime();
            }
        });
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) {
        if (this.build != null) {
            return new DirectoryBrowserSupport(this, new FilePath(this.seleniumReportsDir),
                    "seleniumhtmlreport", "clipboard.gif", false);
        }
        return null;
    }
}
