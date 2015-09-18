package org.jvnet.hudson.plugins.seleniumhtmlreport;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import hudson.FilePath;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.CoreStep;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SeleniumHtmlReportTest {

    @Rule
    public JenkinsRule story = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        SeleniumHtmlReportPublisher reportPublisher = new SeleniumHtmlReportPublisher();
        reportPublisher.setTestResultsDir(".");
        reportPublisher.setFailureIfExceptionOnParsingResultFiles(false);
        CoreStep step = new CoreStep(reportPublisher);
        step = new StepConfigTester(story).configRoundTrip(step);
        SimpleBuildStep delegate = step.delegate;

        assertThat(delegate, instanceOf(SeleniumHtmlReportPublisher.class));

        SeleniumHtmlReportPublisher s = (SeleniumHtmlReportPublisher) delegate;
        assertEquals(".", s.getTestResultsDir());
        assertFalse(s.getFailureIfExceptionOnParsingResultFiles());
    }

    @Test
    public void configRoundTripWithDefaultValues() throws Exception {
        SeleniumHtmlReportPublisher reportPublisher = new SeleniumHtmlReportPublisher();
        CoreStep step = new CoreStep(reportPublisher);
        step = new StepConfigTester(story).configRoundTrip(step);
        SimpleBuildStep delegate = step.delegate;

        assertThat(delegate, instanceOf(SeleniumHtmlReportPublisher.class));

        SeleniumHtmlReportPublisher s = (SeleniumHtmlReportPublisher) delegate;
        assertEquals("target", s.getTestResultsDir());
        assertTrue(s.getFailureIfExceptionOnParsingResultFiles());
    }

    /**
     * Parse a selenium test result
     */
    @Test
    public void testParserResultFromStep() throws Exception {
        WorkflowJob p = story.jenkins.createProject(WorkflowJob.class, "p");
        FilePath testResults = story.jenkins.getWorkspaceFor(p).child("result.html");
        testResults.copyFrom(new ByteArrayInputStream(createSeleniumResultFile()));

        p.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  step([$class: 'SeleniumHtmlReportPublisher', testResultsDir: '.'])\n"
                + "}", true));
        WorkflowRun b = story.assertBuildStatusSuccess(p.scheduleBuild2(0));

        story.assertLogContains("parsing resultFile result.html", b);
        assertEquals(42, b.getAction(SeleniumHtmlReportAction.class).getTotalTime());
    }

    private byte[] createSeleniumResultFile() {
        String result = "<html>\n" +
                "<head><title>SeleniumHtmlReportTest</title></head>\n" +
                "<body>\n" +
                "<table cellpadding=\"2\" cellspacing=\"0\" border=\"1\">\n" +
                "    <tr>\n" +
                "        <td rowspan=\"1\" colspan=\"3\">SeleniumHtmlReportTest</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>result:</td>\n" +
                "        <td>passed</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>totalTime:</td>\n" +
                "        <td>42</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numTestTotal:</td>\n" +
                "        <td>4</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numTestPasses:</td>\n" +
                "        <td>4</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numTestFailures:</td>\n" +
                "        <td>0</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numCommandPasses:</td>\n" +
                "        <td>42</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numCommandFailures:</td>\n" +
                "        <td>0</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>numCommandErrors:</td>\n" +
                "        <td>0</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>setSpeed</td>\n" +
                "        <td>1000</td>\n" +
                "        <td></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>open</td>\n" +
                "        <td></td>\n" +
                "        <td></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>type</td>\n" +
                "        <td>q</td>\n" +
                "        <td>SeleniumHtmlReportTest</td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>click</td>\n" +
                "        <td>btnG</td>\n" +
                "        <td></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td>verifyTextPresent</td>\n" +
                "        <td>SeleniumHtmlReportTest</td>\n" +
                "        <td></td>\n" +
                "    </tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>";

        return result.getBytes(StandardCharsets.UTF_8);
    }

}
