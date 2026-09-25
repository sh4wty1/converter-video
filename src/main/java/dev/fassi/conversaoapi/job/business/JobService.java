package dev.fassi.conversaoapi.job.business;

import dev.fassi.conversaoapi.job.entity.JobEntity;
import dev.fassi.conversaoapi.job.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public UUID criarJob(MultipartFile video) {
        UUID jobId = UUID.randomUUID();
        return jobRepository.save(new JobEntity(jobId, caminhoEntrada)).getId();
    }
}
