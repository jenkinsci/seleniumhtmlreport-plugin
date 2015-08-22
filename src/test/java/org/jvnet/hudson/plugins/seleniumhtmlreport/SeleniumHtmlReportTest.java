package org.jvnet.hudson.plugins.seleniumhtmlreport;

import hudson.model.Result;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.CoreStep;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SeleniumHtmlReportTest {

    @Rule
    public RestartableJenkinsRule story
            = new RestartableJenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SeleniumHtmlReportPublisher reportPublisher = new SeleniumHtmlReportPublisher(".", false);
                CoreStep step = new CoreStep(reportPublisher);

                step = new StepConfigTester(story.j).configRoundTrip(step);
                SimpleBuildStep delegate = step.delegate;

                assertTrue(String.valueOf(delegate), delegate instanceof SeleniumHtmlReportPublisher);
                SeleniumHtmlReportPublisher s = (SeleniumHtmlReportPublisher) delegate;
                assertEquals(".", s.getTestResultsDir());
                assertFalse(s.getFailureIfExceptionOnParsingResultFiles());
            }
        });
    }

    /**
     * Parse a selenium test result
     */
    @Test
    public void parse() throws Exception {
        assertNotNull("Test file missing", getClass().getResource("/SeleniumHtmlReportTest.html"));

        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(""
                        + "node {\n"
                        + "  step([$class: 'SeleniumHtmlReportPublisher', testResultsDir: '" + Paths.get(getClass().getResource("/SeleniumHtmlReportTest.html").toURI()).getParent().toString().replace("\\", "/") + "'])\n"
                        + "}", true));
                WorkflowRun b = story.j.assertBuildStatusSuccess(p.scheduleBuild2(0));

                story.j.assertLogContains("parsing resultFile SeleniumHtmlReportTest.html", b);
            }
        });
    }

}
