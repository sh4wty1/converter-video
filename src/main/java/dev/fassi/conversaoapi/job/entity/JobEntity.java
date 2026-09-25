package dev.fassi.conversaoapi.job.entity;

import dev.fassi.conversaoapi.job.enums.JobStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
public class JobEntity {

    @Id
    private UUID id;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatusEnum status;

    private String caminhoEntrada;

    private String caminhoSaida;

    private Instant ultimoHeartbeatEm;

    private int tentativas;

    private String erro;

    private Instant criadoEm;

    protected JobEntity() {}

    public JobEntity(UUID id, String caminhoEntrada) {
        this.id = id;
        this.status = JobStatusEnum.WAITING;
        this.caminhoEntrada = caminhoEntrada;
        this.criadoEm = Instant.now();
        this.tentativas = 0;
    }

}
