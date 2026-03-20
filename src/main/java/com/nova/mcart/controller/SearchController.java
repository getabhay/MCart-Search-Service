package com.nova.mcart.controller;

import com.nova.mcart.dto.response.ReindexJobStatusResponse;
import com.nova.mcart.dto.response.ReindexStartResponse;
import com.nova.mcart.dto.response.SearchStatusResponse;
import com.nova.mcart.search.ProductIndexService;
import com.nova.mcart.search.ProductSearchAdminService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final ProductSearchAdminService adminService;
    private final ProductIndexService productIndexService;

    @GetMapping("/es/reindex")
    public String reindexAll() throws IOException {
        productIndexService.bulkReindexAll();
        return "Reindex done";
    }

    @GetMapping("/es/index")
    public String indexOne(@RequestParam Long id) throws IOException {
        productIndexService.indexProduct(id);
        return "Indexed product id=" + id;
    }

    @PostMapping("/bootstrap")
    public void bootstrap() throws IOException {
        adminService.bootstrap();
    }

    @GetMapping("/status")
    public SearchStatusResponse status() throws IOException {
        return adminService.status();
    }

    @PostMapping("/reindex")
    public ReindexStartResponse reindex() {
        var job = adminService.startReindex();

        ReindexStartResponse out = new ReindexStartResponse();
        out.setJobId(job.getId());
        out.setMessage("Reindex started");
        return out;
    }

    @GetMapping("/reindex/status/{jobId}")
    public ReindexJobStatusResponse reindexStatus(@PathVariable String jobId) {
        var job = adminService.getJob(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));

        ReindexJobStatusResponse out = new ReindexJobStatusResponse();
        out.setJobId(job.getId());
        out.setStatus(job.getStatus());
        out.setTotalRead(job.getTotalRead());
        out.setTotalIndexed(job.getTotalIndexed());
        out.setError(job.getError());
        return out;
    }
}
