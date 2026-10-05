# Pendências

Coisas que ficaram pra resolver depois. Riscar conforme for fazendo.

## LocalStorage

- [x] **Extensão vem do cliente (segurança).** Resolvido com a lista `EXTENSOES_PERMITIDAS`. `getOriginalFilename()` é controlado pelo usuário. Uma extensão com `/` ou `..` pode fazer o `resolve` sair de `storage/uploads`. Saídas: lista permitida (`.mp4`, `.mov`, `.mkv`...) e/ou conferir com `startsWith` que o caminho final continua dentro de `diretorioBase`. Extensão inválida = erro do cliente (4xx).
- [ ] **Caminho absoluto no banco.** `salvar` devolve `C:\Users\...`, que vai parar em `caminhoEntrada`. Decidir se guarda algo relativo à pasta base (ex.: `uploads/{jobId}.mp4`, como no guia). Isso já é dado persistido, então vale decidir cedo.
- [ ] Implementar `ler` (devolver `Resource`) e `deletar` (`Files.deleteIfExists`). Hoje são esqueletos com `return null`.
- [ ] Pasta base configurável (`application.properties` + `@Value`) em vez de fixa no código.
- [ ] Definir o que o worker/FFmpeg vai usar: ele precisa de um caminho real em disco, e `ler` devolve `Resource`. Pensar em como isso convive com uma futura implementação S3.

## Ligar tudo (Etapa 1)

- [x] `JobService` gerar o `jobId`, chamar `storage.salvar` e só então criar o `JobEntity` com o caminho devolvido.
- [ ] **Arquivo órfão.** Se o `salvar` funciona e o `jobRepository.save` falha, o vídeo fica em `storage/uploads` sem job no banco (aconteceu no teste). Saída: `try/catch` em volta do `save` chamando `storage.deletar` e relançando. Depende do `deletar` implementado.
- [x] `JobController`: trocar `@RequestBody String` por `MultipartFile` (`multipart/form-data`).
- [ ] Responder `202 Accepted` com `{ jobId, status }` (DTO de resposta, sem devolver a entidade).
- [ ] Criar `GET /jobs/{id}`.

## Tratamento de erros

- [ ] Vídeo vazio e extensão não permitida hoje lançam `IllegalArgumentException` (cai em 500 por padrão). Deveria ser 400.
- [x] Requisição sem a parte `video` devolve 400 (`@RequestParam("video")`) e requisição que não é multipart devolve 415 (`consumes` no `@PostMapping`).
- [ ] Falha de I/O sobe como `IOException` e o Spring devolve 500. Ok por enquanto.
- [ ] Upload acima de 500MB (`MaxUploadSizeExceededException`) deveria ser 413.
- [ ] Criar o handler global (`@RestControllerAdvice`) com log da exceção real. Cuidado com um `@ExceptionHandler(Exception.class)` genérico engolir erros que o Spring já trata bem.
- [ ] Decidir: a interface continua com `throws IOException` ou a implementação traduz pra uma exceção própria (`StorageException`), pra não vazar detalhe de disco.

## Decisões de organização

- [ ] Convenção de nomes: `StorageInterface` vs `Storage`, `deletar` vs `apagar`, português vs inglês nos nomes. (O enum de status já ficou em português, igual à constraint da migration.)
- [ ] Pacote do storage: fica em `job/storage` ou vai pra `storage/` no mesmo nível de `job`?
- [ ] Quando existir mais de uma implementação de storage (local e S3), resolver qual injetar (`@Profile`, `@Primary` ou configuração condicional).
- [ ] `@Repository` no `JobRepository` é opcional (interface Spring Data). Manter ou tirar.

## Housekeeping

- [x] Conferir que `target/` não está rastreada (`git status`).
- [ ] Atualizar a seção "Status" do `README.md` conforme avançar.
- [x] Imports não usados no `JobController` e `JobService` (`Autowired`) e na entidade (`Setter`).
