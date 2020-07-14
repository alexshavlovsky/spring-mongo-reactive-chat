package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.TranscodingJob;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TranscodingJobRepository extends ReactiveMongoRepository<TranscodingJob, String> {
    Flux<TranscodingJob> getByStatus(TranscodingJob.Status status);
}
