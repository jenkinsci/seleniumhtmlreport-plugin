package org.jvnet.hudson.plugins.seleniumhtmlreport;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.io.File;

import java.io.IOException;
import java.io.Serializable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Marco Machmer
 */
public class TestResult implements Serializable {

    private final String resultFileName;
    private String result = "";
    private int totalTime = 0;
    private int numTestPasses = 0;
    private int numTestFailures = 0;
    private int numCommandPasses = 0;
    private int numCommandFailures = 0;
    private int numCommandErrors = 0;

    private TestResult(String resultFileName) {
        super();
        this.resultFileName = resultFileName;
    }

    public int getNumTestPasses() {
        return this.numTestPasses;
    }

    public int getNumTestFailures() {
        return this.numTestFailures;
    }

    public int getNumCommandPasses() {
        return this.numCommandPasses;
    }

    public int getNumCommandFailures() {
        return this.numCommandFailures;
    }

    public int getNumCommandErrors() {
        return this.numCommandErrors;
    }

    public int getNumTestTotal() {
        return getNumTestPasses() + getNumTestFailures();
    }

    public String getResult() {
        return this.result;
    }

    public int getTotalTime() {
        return this.totalTime;
    }

    public String getResultFileName() {
        return this.resultFileName;
    }

    public static TestResult parse(AbstractBuild<?,?> build, BuildListener listener, String resultFileName, File seleniumReportsDir) throws IOException {
        TestResult result = new TestResult(resultFileName);
        listener.getLogger().println("parsing resultFile " + result.getResultFileName());
        File reportFile = getReportFileFor(build, result, seleniumReportsDir);
        InfoParser parser = new InfoParser(reportFile);
        result.result = parser.getString("result:");
        result.totalTime = parser.getInt("totalTime:");
        result.numTestPasses = parser.getInt("numTestPasses:");
        result.numTestFailures = parser.getInt("numTestFailures:");
        result.numCommandPasses = parser.getInt("numCommandPasses:");
        result.numCommandFailures = parser.getInt("numCommandFailures:");
        result.numCommandErrors = parser.getInt("numCommandErrors:");
        return result;
    }

    protected static File getReportFileFor(final AbstractBuild<?,?> build, final TestResult testResult, final File seleniumReportsDir) {
        return new File(seleniumReportsDir + "/" + testResult.getResultFileName());
    }

    private static class InfoParser {
        private final File reportFile;

        public InfoParser(File reportFile) {
            super();
            this.reportFile = reportFile;
        }

        public String getString(final String infoName) throws IOException {
            return retrieve(infoName);
        }

        public int getInt(final String infoName) throws IOException {
            return Integer.parseInt(retrieve(infoName));
        }

        protected String retrieve(final String infoName) throws IOException {
            try {
                return parseFor(infoName);
            } catch (ParserConfigurationException e) {
                throw new IOException(e);
            } catch (SAXException e) {
                throw new IOException(e);
            }
        }

        protected String parseFor(final String infoName) throws ParserConfigurationException, SAXException, IOException {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);

            SAXParser saxParser = factory.newSAXParser();
            ReadInfoHandler riHandler = new ReadInfoHandler(infoName);
            try {
                saxParser.parse(this.reportFile, riHandler);
            } catch (BreakParsingException e) {
                // break parsing
            }
            return riHandler.getInfo();
        }
    }

    private static class ReadInfoHandler extends DefaultHandler {
        private final String infoName;
        private String tempVal;
        private boolean readInfo = false;
        private String info;

        public ReadInfoHandler(String infoName) {
            super();
            this.infoName = infoName;
        }

        public String getInfo() {
            return this.info;
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("td".equals(qName)) {
                if (this.readInfo) {
                    this.info = this.tempVal;
                    this.readInfo = false;
                    throw new BreakParsingException();
                }
                if (this.tempVal.equals(this.infoName)) {
                    this.readInfo = true;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            this.tempVal = new String(ch, start, length).trim();
        }
    }
    
    private static class BreakParsingException extends SAXException {
        public BreakParsingException() {
            super();
        }
    }
}
