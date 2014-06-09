package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import java.util.List;

/**
 * @author parker
 */
public interface ProctorReader<RevisionType> {
    TestMatrixVersion getCurrentTestMatrix() throws StoreException;

    TestDefinition getCurrentTestDefinition(String test) throws StoreException;

    void verifySetup() throws StoreException;

    /***** Versioned ProctorReader *****/

    RevisionType getLatestVersion() throws StoreException;

    TestMatrixVersion getTestMatrix(RevisionType fetchRevision) throws StoreException;

    TestDefinition getTestDefinition(String test, RevisionType fetchRevision) throws StoreException;

    List<Revision<RevisionType>> getMatrixHistory(int start, int limit) throws StoreException;

    List<Revision<RevisionType>> getHistory(String test, int start, int limit) throws StoreException;

    List<Revision<RevisionType>> getHistory(String test, RevisionType revision, int start, int limit) throws StoreException;
}
