package dev.fassi.conversaoapi.job.repository;

import dev.fassi.conversaoapi.job.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {
}
