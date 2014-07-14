package com.indeed.proctor.builder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import com.google.common.io.Files;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.store.GitProctor;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;

public class GitLocalProctorBuilder extends ProctorBuilder {

    private static final Logger LOGGER = Logger.getLogger(GitLocalProctorBuilder.class);

    public GitLocalProctorBuilder(ProctorReader proctorReader, Writer outputSink) {
        super(proctorReader, outputSink);
    }

    public GitLocalProctorBuilder(ProctorReader proctorReader, Writer outputSink, String author) {
        super(proctorReader, outputSink, author);
    }

    public GitLocalProctorBuilder(ProctorReader proctorReader, Writer outputSink, String author, String version) {
        super(proctorReader, outputSink, author, version);
    }

    private static class GitLocalProctorBuilderArgs extends ProctorBuilderArgs {
        private String inputGitUrl;
        private String branchName;

        private GitLocalProctorBuilderArgs() {
            options.addOption(OptionBuilder.hasArg(true)
                    .isRequired()
                    .withLongOpt("input")
                    .withArgName("input git url")
                    .withDescription("The git url to read from.")
                    .create("i"));
            options.addOption(OptionBuilder.hasArg(true)
                    .withLongOpt("branch")
                    .withArgName("git branch")
                    .withDescription("The git branch to checkout.")
                    .create("b"));
        }

        @Override
        protected void extract(CommandLine results)  {
            super.extract(results);
            this.inputGitUrl = results.getOptionValue("input");
            this.branchName = results.getOptionValue("branch");
        }

        public String getInputGitUrl() {
            return inputGitUrl;
        }

        public String getBranchName() {
            return branchName;
        }
    }

    public static void main(final String[] args) throws IOException, StoreException, IncompatibleTestMatrixException {
        final GitLocalProctorBuilderArgs arguments = new GitLocalProctorBuilderArgs();
        arguments.parse(args);

        try {
            new GitLocalProctorBuilder(
                    new GitProctor(arguments.getInputGitUrl(), "", "", arguments.getBranchName()),
                    "-".equals(arguments.getOutputdir()) ?
                            new PrintWriter(System.out) :
                            new FileWriter(new File(arguments.getOutputdir(), arguments.getFilename())),
                    arguments.getAuthor(),
                    arguments.getVersion()).execute();
        } catch (Exception e) {
            LOGGER.error("Failed to generates proctor artifact from " + arguments.getInputGitUrl(), e);
            System.exit(1);
        }
    }
}