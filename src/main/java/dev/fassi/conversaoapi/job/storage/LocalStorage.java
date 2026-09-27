package dev.fassi.conversaoapi.job.storage;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class LocalStorage implements StorageInterface {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(".mp4", ".mov", ".mkv", ".avi", ".webm");

    private final Path diretorioBase = Paths.get("storage/uploads").toAbsolutePath().normalize();

    public LocalStorage() {
        try {
            // Garante que a pasta existe assim que o componente é inicializado
            Files.createDirectories(diretorioBase);
        } catch (IOException error) {
            throw new RuntimeException("Não foi possível inicializar o diretório de upload: " + error.getMessage(), error);
        }
    }

    @Override
    public String salvar(MultipartFile video, UUID jobId) throws IOException {
        // Valida se o video existe ou está vazio
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("O vídeo não pode ser nulo");
        }

        if (jobId == null) {
            throw new IllegalArgumentException("O jobId não pode ser nulo");
        }

        String extensao = extrairExtensao(video.getOriginalFilename());

        String nomeVideo = jobId.toString() + extensao;

        Path caminhoCompleto = this.diretorioBase.resolve(nomeVideo);

        try (InputStream is = video.getInputStream()) {
            Files.copy(is, caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
        }

        return caminhoCompleto.toString();
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            throw new IllegalArgumentException("O arquivo precisa ter uma extensão");
        }

        String extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf(".")).toLowerCase(Locale.ROOT);

        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new IllegalArgumentException("Extensão de arquivo não permitida: " + extensao);
        }

        return extensao;
    }

    @Override
    public Resource ler(String path) {
        // Implementação para ler o vídeo localmente
        return null;
    }

    @Override
    public void deletar(String path) {
        // Implementação para deletar o vídeo localmente
    }
}
