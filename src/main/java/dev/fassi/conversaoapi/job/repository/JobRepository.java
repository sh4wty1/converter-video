package dev.fassi.conversaoapi.job.repository;

import dev.fassi.conversaoapi.job.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {
}
