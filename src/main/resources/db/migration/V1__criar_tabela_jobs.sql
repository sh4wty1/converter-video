CREATE TABLE jobs (
                      id                   UUID PRIMARY KEY,
                      status               VARCHAR(20) NOT NULL,
                      caminho_entrada      TEXT NOT NULL,
                      caminho_saida        TEXT,
                      ultimo_heartbeat_em  TIMESTAMPTZ,
                      tentativas           INT NOT NULL DEFAULT 0,
                      erro                 TEXT,
                      criado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                      CONSTRAINT jobs_status_valido
                          CHECK (status IN ('PENDENTE', 'PROCESSANDO', 'PRONTO', 'ERRO'))
);

CREATE INDEX idx_jobs_status_criado_em ON jobs (status, criado_em);