# Pendências

Coisas que ficaram pra resolver depois. Riscar conforme for fazendo.

## LocalStorage

- [ ] **Extensão vem do cliente (segurança).** `getOriginalFilename()` é controlado pelo usuário. Uma extensão com `/` ou `..` pode fazer o `resolve` sair de `storage/uploads`. Saídas: lista permitida (`.mp4`, `.mov`, `.mkv`...) e/ou conferir com `startsWith` que o caminho final continua dentro de `diretorioBase`. Extensão inválida = erro do cliente (4xx).
- [ ] **Caminho absoluto no banco.** `salvar` devolve `C:\Users\...`, que vai parar em `caminhoEntrada`. Decidir se guarda algo relativo à pasta base (ex.: `uploads/{jobId}.mp4`, como no guia). Isso já é dado persistido, então vale decidir cedo.
- [ ] Implementar `ler` (devolver `Resource`) e `deletar` (`Files.deleteIfExists`). Hoje são esqueletos com `return null`.
- [ ] Pasta base configurável (`application.properties` + `@Value`) em vez de fixa no código.
- [ ] Definir o que o worker/FFmpeg vai usar: ele precisa de um caminho real em disco, e `ler` devolve `Resource`. Pensar em como isso convive com uma futura implementação S3.

## Ligar tudo (Etapa 1)

- [ ] `JobService` gerar o `jobId`, chamar `storage.salvar` e só então criar o `JobEntity` com o caminho devolvido. Pensar na ordem e no que sobra se um dos dois falhar (arquivo salvo e banco falhou, ou o contrário).
- [ ] `JobController`: trocar `@RequestBody String` por `MultipartFile` (`multipart/form-data`).
- [ ] Responder `202 Accepted` com `{ jobId, status }` (DTO de resposta, sem devolver a entidade).
- [ ] Criar `GET /jobs/{id}`.

## Tratamento de erros

- [ ] Vídeo vazio hoje lança `IllegalArgumentException` (cai em 500 por padrão). Deveria ser 400.
- [ ] Falha de I/O sobe como `IOException` e o Spring devolve 500. Ok por enquanto.
- [ ] Upload acima de 500MB (`MaxUploadSizeExceededException`) deveria ser 413.
- [ ] Criar o handler global (`@RestControllerAdvice`) com log da exceção real. Cuidado com um `@ExceptionHandler(Exception.class)` genérico engolir erros que o Spring já trata bem.
- [ ] Decidir: a interface continua com `throws IOException` ou a implementação traduz pra uma exceção própria (`StorageException`), pra não vazar detalhe de disco.

## Decisões de organização

- [ ] Convenção de nomes: `StorageInterface` vs `Storage`, `deletar` vs `apagar`, português vs inglês nos nomes e no enum de status.
- [ ] Pacote do storage: fica em `job/storage` ou vai pra `storage/` no mesmo nível de `job`?
- [ ] Quando existir mais de uma implementação de storage (local e S3), resolver qual injetar (`@Profile`, `@Primary` ou configuração condicional).
- [ ] `@Repository` no `JobRepository` é opcional (interface Spring Data). Manter ou tirar.

## Housekeeping

- [ ] Conferir que `target/` não está rastreada (`git status`).
- [ ] Atualizar a seção "Status" do `README.md` conforme avançar.
- [ ] Imports não usados no `JobController` e `JobService` (`Autowired`) e na entidade (`Setter`).
