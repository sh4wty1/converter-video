package dev.fassi.conversaoapi.job.storage;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Component
public class LocalStorage implements StorageInterface {

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

        String nomeOriginal = video.getOriginalFilename();
        String extensao = ".mp4";

        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }

        String nomeVideo = jobId.toString() + extensao;

        Path caminhoCompleto = this.diretorioBase.resolve(nomeVideo);

        try (InputStream is = video.getInputStream()) {
            Files.copy(is, caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
        }

        return caminhoCompleto.toString();
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
