package com.nova.mcart.search.reindex;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ReindexJobStore {

    private final Map<String, ReindexJob> jobs = new ConcurrentHashMap<>();

    public ReindexJob create() {
        String id = UUID.randomUUID().toString();
        ReindexJob job = new ReindexJob(id);
        jobs.put(id, job);
        return job;
    }

    public Optional<ReindexJob> get(String id) {
        return Optional.ofNullable(jobs.get(id));
    }
}
