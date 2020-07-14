package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.TranscodingJob;
import com.ctzn.springmongoreactivechat.repository.TranscodingJobRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@Service
public class TranscodingJobService {
    private TranscodingJobRepository repository;

    public TranscodingJobService(TranscodingJobRepository repository) {
        this.repository = repository;
    }

    public Mono<TranscodingJob> save(TranscodingJob job) {
        return repository.save(job);
    }

    public Mono<TranscodingJob> getNextPendingJob() {
        return repository.getByStatus(TranscodingJob.Status.PENDING)
                .sort(Comparator.comparingInt(TranscodingJob::getPriority).thenComparing(TranscodingJob::getCreatedOn))
                .next();
//        return mongo.find(new BasicQuery("{'status': 'PENDING'}"), TranscodingJob.class)
//                .sort(Comparator.comparingInt(TranscodingJob::getPriority).thenComparing(TranscodingJob::getCreatedOn))
//                .next();
    }

    public Mono<TranscodingJob> updateJobStatus(TranscodingJob job, TranscodingJob.Status status, String message) {
        job.setStatusAndLog(status, message);
        return repository.save(job);
//        return mongo.updateFirst(query(where("_id").is(job.getId())), Update.update("status", status), TranscodingJob.class);
    }

    public Mono<TranscodingJob> updateLog(TranscodingJob job, String message) {
        job.appendToLog(message);
        return repository.save(job);
    }
}
