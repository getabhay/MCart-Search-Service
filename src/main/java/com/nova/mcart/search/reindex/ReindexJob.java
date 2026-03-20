package com.nova.mcart.search.reindex;

import lombok.Getter;

@Getter
public class ReindexJob {

    private final String id;

    private volatile String status = "RUNNING"; // RUNNING | COMPLETED | FAILED
    private volatile long totalRead = 0;
    private volatile long totalIndexed = 0;
    private volatile String error;

    public ReindexJob(String id) {
        this.id = id;
    }

    public void incRead(long delta) {
        totalRead += delta;
    }

    public void incIndexed(long delta) {
        totalIndexed += delta;
    }

    public void completed() {
        status = "COMPLETED";
    }

    public void failed(String error) {
        status = "FAILED";
        this.error = error;
    }
}
