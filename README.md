# conversaoAPI

Estudo de caso: uma API que converte vídeo 16:9 pra 9:16 (formato de Reels/Shorts).

O vídeo em si é só o pretexto. O que eu quero aprender aqui é como lidar com **processamento pesado sem prender a requisição HTTP**.

## A ideia

Converter vídeo leva minutos. Se a API fizer isso dentro da requisição, dá timeout, trava threads e perde o trabalho se a conexão cair. Então:

1. O cliente envia o vídeo (`POST /jobs`)
2. A API guarda o arquivo, cria um job com status `WAITING` e responde na hora com `202` e um `jobId`
3. Um worker separado pega o job, converte com FFmpeg e marca como pronto
4. O cliente consulta `GET /jobs/{id}` até terminar e baixa o resultado

A analogia que usei pra montar a arquitetura é a de uma lanchonete: a API é o atendente, o `jobId` é a senha, o worker é o chapeiro e a tabela `jobs` é o painel de senhas.

## O que quero praticar

- Fila e workers rodando em paralelo
- Mais de um worker sem pegar o mesmo job (update condicional atômico)
- Lease + heartbeat pra recuperar job de worker que morreu
- Idempotência e limite de tentativas
- Separar armazenamento de arquivo (storage) do estado (banco)

O passo a passo completo está no [GUIDE_API.md](GUIDE_API.md).

## Stack

Java 21, Spring Boot, PostgreSQL (Docker), Flyway e FFmpeg.

## Rodando

Precisa de Java 21, Docker e FFmpeg instalado.

```bash
docker compose up -d
./mvnw spring-boot:run
```

## Status

Em construção. Por enquanto só o começo do `POST /jobs` (modelo de dados e criação do job). Storage, conversão, worker e o resto do roteiro ainda vêm.
