package com.indeed.proctor.store;

import java.io.File;

public interface GitWorkspaceProvider {
    /**
     * Delete the temp files in the working directory.
     * @return A flag whose value indicates if the directory cleanup was
     * successful
     */
    public boolean cleanWorkingDirectory();

    public File getRootDirectory();

}
