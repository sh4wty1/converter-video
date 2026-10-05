package dev.fassi.conversaoapi.job.business;

import dev.fassi.conversaoapi.job.entity.JobEntity;
import dev.fassi.conversaoapi.job.repository.JobRepository;
import dev.fassi.conversaoapi.job.storage.StorageInterface;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final StorageInterface storage;

    public JobService(JobRepository jobRepository, StorageInterface storage) {
        this.jobRepository = jobRepository;
        this.storage = storage;
    }

    public UUID criarJob(MultipartFile video) throws IOException {
        UUID jobId = UUID.randomUUID();
        String caminhoEntrada = storage.salvar(video, jobId);
        return jobRepository.save(new JobEntity(jobId, caminhoEntrada)).getId();
    }
}
