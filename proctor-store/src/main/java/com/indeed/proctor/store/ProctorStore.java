package com.indeed.proctor.store;

import java.io.Closeable;

public interface ProctorStore<RevisionType> extends Closeable, ProctorReader<RevisionType>, ProctorWriter<RevisionType> {
}
