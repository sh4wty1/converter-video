# API de conversão de vídeo 16:9 → 9:16

Estudo de caso: processamento pesado com **fila + workers**, sem prender a conexão HTTP.

---

## 1. O problema

Processar vídeo leva minutos. Se a API processar **dentro da requisição**:

- **Timeout**: navegador, proxy e load balancer derrubam a conexão depois de ~30–60s
- **Threads presas**: cada requisição ocupa uma thread do servidor; poucos uploads simultâneos travam a API pra todo mundo
- **Trabalho perdido**: se a conexão do usuário cai, o processamento vai pro lixo

**Regra de ouro:** a requisição HTTP só *inicia* o trabalho. Ela não espera ele terminar.

---

## 2. A analogia da lanchonete

| Lanchonete | Sistema |
|---|---|
| Atendente no balcão | API (thread HTTP) |
| Senha | `jobId` |
| Trilho de comandas | Fila de mensagens |
| Chapeiro | Worker |
| Painel de senhas | Tabela `jobs` no banco (status) |
| Prato numerado com a senha | Arquivo `outputs/{jobId}.mp4` no storage |
| Cliente olhando o painel | Polling (`GET /jobs/{id}`) |
| Contratar mais chapeiros | Escalar workers horizontalmente |

O atendente anota, pendura a comanda no trilho e **volta pro balcão na hora**. Chapeiros livres **puxam** a próxima comanda.

---

## 3. Fluxo completo

1. Usuário clica em "enviar"; o front faz `POST /jobs` com o vídeo
2. API salva o original no storage: `uploads/{jobId}.mp4`
3. API cria o registro na tabela `jobs` com status `PENDENTE`
4. API publica na fila uma mensagem com `jobId` e o caminho do arquivo (**não o vídeo em si**)
5. API responde **na hora** `202 Accepted` com `{ "jobId": ..., "status": "PENDENTE" }`
6. Um worker livre puxa a mensagem e tenta **reivindicar** o job no banco (update atômico, seção 6)
7. Se ganhou: converte o vídeo, mandando heartbeats periódicos enquanto trabalha
8. Salva o resultado em `outputs/{jobId}.mp4`, marca `PRONTO` e dá **ack** na fila
9. Front faz polling em `GET /jobs/{jobId}` até o status virar `PRONTO` e recebe o link de download

---

## 4. Componentes

- **API**: recebe upload, cria job, publica na fila, responde status. Nunca processa vídeo.
- **Storage**: guarda arquivos pesados. Pasta local pra estudar; S3 ou similar em produção.
- **Fila**: lista de trabalhos a fazer. Pode começar usando o próprio banco como fila e depois migrar pra RabbitMQ/SQS.
- **Banco**: fonte da verdade do status de cada job.
- **Worker**: processo separado que converte os vídeos. Pode ter vários rodando.

> Fila e banco têm papéis diferentes: a fila diz **o que tem pra fazer**, o banco diz **em que pé está cada coisa**.

---

## 5. Modelo de dados

Tabela `jobs`:

| Coluna | Tipo | Pra quê |
|---|---|---|
| `id` | UUID / bigint | Identificador do job (a "senha") |
| `status` | texto | `PENDENTE`, `PROCESSANDO`, `PRONTO`, `ERRO` |
| `caminho_entrada` | texto | Ex.: `uploads/123.mp4` |
| `caminho_saida` | texto | Ex.: `outputs/123.mp4` (preenchido no fim) |
| `ultimo_heartbeat_em` | timestamp | Último "tô vivo" do worker que está com o job |
| `tentativas` | int | Quantas vezes o job já foi pego |
| `erro` | texto | Mensagem de erro, se falhar |
| `criado_em` | timestamp | Quando o job foi criado |

---

## 6. Conceitos de concorrência (o coração do case)

### 6.1 Ack na fila
O worker **não remove** a mensagem ao começar. Só dá ack ao terminar. Se morrer sem ack, a fila reentrega a mensagem pra outro worker. Não precisa de worker parado de reserva.

Efeito colateral: **o mesmo job pode ser entregue duas vezes**. Os próximos itens tratam disso.

### 6.2 Idempotência
Fazer duas vezes dá o mesmo resultado que fazer uma. O worker sempre grava em `outputs/{jobId}.mp4`, então reprocessar só sobrescreve o mesmo arquivo. É a rede de segurança.

Além disso: se o worker recebe um job que já está `PRONTO`, só dá ack e descarta.

### 6.3 Update condicional atômico (compare-and-set / lock otimista)
"Só mudo se ainda estiver como eu espero." O banco executa um UPDATE de cada vez, então só um worker consegue alterar a linha.

- Alterou **1 linha** → ganhei o job, posso processar
- Alterou **0 linhas** → outro pegou, largo

### 6.4 Lease + heartbeat
Problema: se o worker marca `PROCESSANDO` e morre, o job fica preso pra sempre.

Solução: o worker **aluga** o job por um tempo (lease). Enquanto trabalha, renova o aluguel atualizando `ultimo_heartbeat_em` periodicamente (heartbeat). Se parar de mandar sinal, o aluguel vence e outro worker pode assumir.

Reivindicar o job:

```sql
UPDATE jobs
SET status = 'PROCESSANDO',
    ultimo_heartbeat_em = NOW(),
    tentativas = tentativas + 1
WHERE id = :jobId
  AND ( status = 'PENDENTE'
        OR ( status = 'PROCESSANDO'
             AND ultimo_heartbeat_em < NOW() - INTERVAL '2 minutes' ) )
```

Heartbeat (rodar a cada ~30s enquanto processa):

```sql
UPDATE jobs
SET ultimo_heartbeat_em = NOW()
WHERE id = :jobId AND status = 'PROCESSANDO'
```

Finalizar:

```sql
UPDATE jobs
SET status = 'PRONTO', caminho_saida = :saida
WHERE id = :jobId AND status = 'PROCESSANDO'
```

> Regra prática: intervalo do heartbeat bem menor que o timeout do lease (ex.: 30s e 2min), pra um atraso pequeno não derrubar um worker vivo.

### 6.5 Falhas definitivas
Vídeo corrompido nunca vai dar certo. Se `tentativas` passar de um limite (ex.: 3), marca `ERRO` e para de tentar. Em fila de verdade isso vira uma **dead letter queue**.

---

## 7. Contrato da API

### `POST /jobs`
Upload `multipart/form-data` com o vídeo.

Resposta `202 Accepted`:
```json
{ "jobId": "123", "status": "PENDENTE" }
```

### `GET /jobs/{jobId}`
Resposta `200 OK`:
```json
{ "jobId": "123", "status": "PROCESSANDO" }
```
Quando pronto:
```json
{ "jobId": "123", "status": "PRONTO", "downloadUrl": "/jobs/123/video" }
```

### `GET /jobs/{jobId}/video`
Devolve o arquivo convertido (ou `404` se ainda não estiver pronto).

---

## 8. A conversão em si (FFmpeg)

O Java não converte vídeo "na mão": ele chama o **FFmpeg** via `ProcessBuilder`.

Cortar o centro (preenche a tela, perde as laterais):
```bash
ffmpeg -i entrada.mp4 -vf "crop=ih*9/16:ih,scale=1080:1920" -c:a copy saida.mp4
```

Encaixar com barras (mostra tudo, sobra espaço em cima e embaixo):
```bash
ffmpeg -i entrada.mp4 -vf "scale=1080:-2,pad=1080:1920:(ow-iw)/2:(oh-ih)/2" -c:a copy saida.mp4
```

---

## 9. Stack sugerida

- **Java 21 + Spring Boot** (Web + Data JPA)
- **PostgreSQL** (roda fácil com Docker)
- **Fila**: começar com a tabela `jobs` como fila (worker busca `PENDENTE`); depois trocar por **RabbitMQ**
- **Storage**: pasta local `storage/uploads` e `storage/outputs`
- **FFmpeg** instalado na máquina
- **Worker**: pode ser um `@Scheduled` no mesmo projeto no início; depois separar em outra aplicação

---

## 10. Roteiro de construção

Cada etapa funcionando antes da próxima.

**Etapa 1: API síncrona "burra"**
- `POST /jobs` salva o arquivo e cria o registro `PENDENTE`, responde o `jobId`
- `GET /jobs/{id}` lê o status do banco
- ❓ Onde o arquivo é salvo? Como gerar o nome sem colidir?

**Etapa 2: conversão isolada**
- Uma classe que recebe caminho de entrada e saída e chama o FFmpeg
- Teste ela sozinha antes de integrar
- ❓ Como saber se o FFmpeg deu erro? (dica: exit code do processo)

**Etapa 3: worker simples**
- Um `@Scheduled` que busca um job `PENDENTE`, processa e marca `PRONTO`
- ❓ O que acontece se o método demorar mais que o intervalo do agendamento?

**Etapa 4: concorrência**
- Trocar a busca pelo UPDATE condicional atômico
- Rodar duas instâncias do worker e verificar que nunca pegam o mesmo job
- ❓ Como testar isso de propósito?

**Etapa 5: lease + heartbeat**
- Thread de heartbeat enquanto o FFmpeg roda
- Teste: matar o worker no meio e ver outro assumir depois do timeout

**Etapa 6: falhas**
- Contador de tentativas, status `ERRO`, mensagem de erro salva

**Etapa 7 (bônus)**
- Trocar a "fila no banco" por RabbitMQ com ack manual
- Front simples com polling
- Trocar polling por WebSocket

---

## 11. Glossário pra entrevista

- **Processamento assíncrono**: resposta imediata, trabalho feito depois
- **202 Accepted**: "recebi, ainda não terminei"
- **Fila de mensagens**: desacopla quem pede de quem executa
- **Worker**: consumidor da fila que faz o trabalho pesado
- **Escala horizontal**: mais workers, sem mudar a API
- **Ack**: confirmação de que a mensagem foi processada
- **Idempotência**: repetir não causa estrago
- **Compare-and-set / lock otimista**: só altera se o estado ainda for o esperado
- **Lease**: posse temporária de um trabalho
- **Heartbeat**: sinal periódico de "tô vivo"
- **Dead letter queue**: destino de mensagens que falharam demais
- **Polling / webhook / WebSocket**: formas do cliente saber que terminou