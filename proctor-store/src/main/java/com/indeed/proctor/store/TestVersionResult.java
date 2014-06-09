package com.indeed.proctor.store;

import java.util.Date;
import java.util.List;

/** @author parker */
class TestVersionResult<RevisionType> {
    private List<Test<RevisionType>> tests;
    private Date published;
    private String author;
    private String version;
    private String description;

    TestVersionResult(List<Test<RevisionType>> tests, Date published, String author, String version, String description) {
        this.tests = tests;
        this.published = published;
        this.author = author;
        this.version = version;
        this.description = description;
    }

    static class Test<RevisionType> {
        final String testName;
        final RevisionType revision;

        Test(String testName, RevisionType revision) {
            this.testName = testName;
            this.revision = revision;
        }

        public String getTestName() {
            return testName;
        }

        public RevisionType getRevision() {
            return revision;
        }
    }

    public List<Test<RevisionType>> getTests() {
        return tests;
    }

    public Date getPublished() {
        return published;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }
}
