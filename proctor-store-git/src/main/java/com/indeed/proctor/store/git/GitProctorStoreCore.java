package com.indeed.proctor.store.git;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.FileBasedPersisterCore;
import com.indeed.proctor.store.FileBasedProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.TestVersionResult;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** @author parker */
public class GitProctorStoreCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(GitProctorStoreCore.class);
    private static final String DIRECTORY_IDENTIFIER = ".git";

    // TODO (parker) 6/30/14 - Is Git / Repository thread safe ?
    private final Git git;
    private final String refName;

    public static void main(String args[]) throws IOException, GitAPIException, StoreException {
        // remote url allows file:/// and http:// git urls
        final String remoteUrl = args[0];

        File localPath = Files.createTempDir();
        System.out.println("temp dir: " + localPath);

        // then clone
        System.out.println("Cloning from " + remoteUrl + " to " + localPath);
        final Git git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localPath)
                .setProgressMonitor(new TextProgressMonitor())
                .call();

        git.fetch()
            .setProgressMonitor(new TextProgressMonitor())
            .call();


        final GitProctorStore store = new GitProctorStore(new GitProctorStoreCore(git, Constants.HEAD));

        // final TestMatrixVersion artiact = store.getCurrentTestMatrix();
        // Serializers.lenient().writeValue(System.err, artiact);
        final String test = "acmeexcerptellipsistst";
        final List<Revision> history = store.getHistory(test, 0, 2);
        final TestDefinition definition = store.getTestDefinition(test, history.get(0).getRevision());
        Serializers.lenient().writeValue(System.err, definition);
        final TestDefinition testDefinition = store.getCurrentTestDefinition(test);
        Serializers.lenient().writeValue(System.err, testDefinition);

        git.getRepository().close();
    }

    public GitProctorStoreCore(final Git git,
                               final String refName) {
        this.git = git;
        this.refName = refName;
    }

    @Override
    public <C> C getFileContents(final Class<C> c,
                                 final java.lang.String[] path,
                                 final C defaultValue,
                                 final String revision) throws StoreException.ReadException, JsonProcessingException {
        try {
            if (!ObjectId.isId(revision)) {
                throw new StoreException.ReadException("Malformed id " + revision);
            }
            final ObjectId blobOrCommitId = ObjectId.fromString(revision);

            final ObjectLoader loader = git.getRepository().open(blobOrCommitId);

            if (loader.getType() == Constants.OBJ_COMMIT) {
                // look up the file at this revision
                final RevCommit commit = RevCommit.parse(loader.getCachedBytes());

                final TreeWalk treeWalk2 = new TreeWalk(git.getRepository());
                treeWalk2.addTree(commit.getTree());
                treeWalk2.setRecursive(true);
                final String joinedPath = "matrices" + "/" + Joiner.on("/").join(path);
                treeWalk2.setFilter(PathFilter.create(joinedPath));

                if (!treeWalk2.next()) {
                    throw new StoreException.ReadException("Did not find expected file '" + joinedPath + "'");
                }
                final ObjectId blobId = treeWalk2.getObjectId(0);
                return getFileContents(c, blobId);
            } else if (loader.getType() == Constants.OBJ_BLOB) {
                return getFileContents(c, blobOrCommitId);
            } else {
                throw new StoreException.ReadException("Invalid Object Type " + loader.getType() + " for id " + revision);
            }
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    private <C> C getFileContents(final Class<C> c,
                                  final ObjectId blobId) throws IOException {
        ObjectLoader loader = git.getRepository().open(blobId);
        final ObjectMapper mapper = Serializers.lenient();
        return mapper.readValue(loader.getBytes(), c);
    }


    @Override
    public void doInWorkingDirectory(final java.lang.String username,
                                     final java.lang.String password,
                                     final java.lang.String comment,
                                     final String previousVersion,
                                     final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        /**
         * TODO:
         * - create repository for user
         * - copy n clone appropriate ref
         * - bring changes up to date
         * - call updater
         * - if anything has changed, commit and push to remote repository
         */
        throw new UnsupportedOperationException("Not implemented");
    }

    private static class GitRcsClient implements FileBasedProctorStore.RcsClient {
        final Git userGit;
        final File localDirectory;

        private GitRcsClient(final Git userGit) {
            this.userGit = userGit;
            this.localDirectory = userGit.getRepository().getDirectory();
        }

        @Override
        public void add(final File file) throws Exception {
            // un-tested
            final String absolutePath = file.getAbsolutePath();
            final String directoryPath = localDirectory.getAbsolutePath();
            final String relativePath = absolutePath.substring(directoryPath.length());
            userGit.add().addFilepattern(relativePath);
        }

        @Override
        public void delete(final File testDefinitionDirectory) throws Exception {
            // un-tested
            final String absolutePath = testDefinitionDirectory.getAbsolutePath();
            final String directoryPath = localDirectory.getAbsolutePath();
            final String relativePath = absolutePath.substring(directoryPath.length());
            userGit.rm().addFilepattern(relativePath);
        }
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        try {
            final RevWalk walk = new RevWalk(git.getRepository());
            final ObjectId commitId = ObjectId.fromString(fetchRevision);
            final RevCommit headTree = walk.parseCommit(commitId);
            final RevTree tree = headTree.getTree();
            System.out.println("Having tree: " + tree);


            // now use a TreeWalk to iterate over all files in the Tree recursively
            // you can set Filters to narrow down the results if needed
            TreeWalk treeWalk = new TreeWalk(git.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setFilter(AndTreeFilter.create(PathFilter.create("matrices/test-definitions"), PathSuffixFilter.create("definition.json")));
            treeWalk.setRecursive(true);

            final List<TestVersionResult.Test> tests = Lists.newArrayList();
            while (treeWalk.next()) {
                final ObjectId id = treeWalk.getObjectId(0);
                // final RevTree revTree = walk.lookupTree(id);

                final String path = treeWalk.getPathString();
                final String[] pieces = path.split("/");
                final String testname = pieces[pieces.length - 2]; // tree / parent directory name

                // testname, blobid pair
                // note this is the blobid hash - not a commit hash
                // RevTree.id and RevBlob.id
                tests.add(new TestVersionResult.Test(testname, id.name()));
            }

            walk.dispose();
            return new TestVersionResult(
                tests,
                new Date(headTree.getCommitTime()),
                headTree.getAuthorIdent().toExternalString(),
                headTree.toObjectId().getName(),
                headTree.getFullMessage()
            );
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    Git getGit() {
        return git;
    }

    String getRefName() {
        return refName;
    }

    @Override
    public String getAddTestRevision() {
        return ObjectId.zeroId().name();
    }

    @Override
    public void close() throws IOException {
        // Is this ThreadSafe ?
        git.getRepository().close();
    }
}
