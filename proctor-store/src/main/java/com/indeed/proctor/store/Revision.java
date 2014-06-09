package com.indeed.proctor.store;

import java.util.Date;

public class Revision<RevisionType> {
    private final RevisionType revision;
    private final String author;
    private final Date date;
    private final String message;

    public Revision(final RevisionType revision, final String author, final Date date, final String message) {
        this.revision = revision;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public RevisionType getRevision() {
        return revision;
    }

    public String getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }
}
