package com.indeed.proctor.store.git;

import com.google.common.collect.Lists;
import com.indeed.proctor.store.FileBasedProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/** @author parker */
public class GitProctorStore extends FileBasedProctorStore {
    private static final Logger LOGGER = Logger.getLogger(GitProctorStore.class);
    private final Git git;

    public GitProctorStore(final GitProctorStoreCore core) {
        super(core);
        this.git = core.getGit();
    }

    @Override
    public void verifySetup() throws StoreException {
        final String refName = getGitCore().getRefName();
        try {
            final ObjectId branchHead = git.getRepository().resolve(refName);
            if (branchHead == null) {
                throw new StoreException("git repository couldn't resolve " + refName);
            }
        } catch (IncorrectObjectTypeException e) {
            throw new StoreException("Could get resolve " + refName);
        } catch (AmbiguousObjectException e) {
            throw new StoreException("Could get resolve " + refName);
        } catch (IOException e) {
            throw new StoreException("Could get resolve " + refName);
        }
    }

    protected GitProctorStoreCore getGitCore() {
        return (GitProctorStoreCore) core;
    }

    @Override
    public String getLatestVersion() throws StoreException {
        try {
            final Ref branch = git.getRepository().getRef(getGitCore().getRefName());
            // final RevWalk walk = new RevWalk(git.getRepository());
            // final RevCommit headCommit = walk.parseCommit(branch.getObjectId());
            return branch.getObjectId().name();
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public List<Revision> getMatrixHistory(final int start,
                                                   final int limit) throws StoreException {
        final LogCommand logCommand;
        try {
            final ObjectId branchHead = git.getRepository().resolve(getGitCore().getRefName());
            logCommand = git.log()
                .add(branchHead)
                .setSkip(start)
                .setMaxCount(limit);
            return getHistoryFromLogCommand(logCommand);
        } catch (MissingObjectException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (IncorrectObjectTypeException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (AmbiguousObjectException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (IOException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        }
    }

    @Override
    public List<Revision> getHistory(final String test,
                                             final int start,
                                             final int limit) throws StoreException {
        return getHistory(test, getLatestVersion(), start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test,
                                             final String revision,
                                             final int start,
                                             final int limit) throws StoreException {
        try {
            final ObjectId commitId = ObjectId.fromString(revision);
            final LogCommand logCommand = git.log()
                // TODO: create path to definition.json file, sanitize test name for invalid / relative characters
                .addPath("matrices/test-definitions/" + test + "/definition.json")
                .add(commitId)
                .setSkip(start)
                .setMaxCount(limit);
            return getHistoryFromLogCommand(logCommand);

        } catch (IOException e) {
            throw new StoreException("Could not get history for " + test + " starting at " + getGitCore().getRefName(), e);
        }
    }

    private List<Revision> getHistoryFromLogCommand(final LogCommand command) throws StoreException {
        final List<Revision> versions = Lists.newArrayList();
        final Iterable<RevCommit> commits;
        try {
            commits = command.call();
        } catch (GitAPIException e) {
            throw new StoreException("Could not get history", e);
        }
        for( RevCommit commit : commits) {
            versions.add(new Revision(
                commit.getName(),
                commit.getAuthorIdent().toExternalString(),
                new Date(commit.getCommitTime()),
                commit.getFullMessage()
            ));
        }
        return versions;
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
