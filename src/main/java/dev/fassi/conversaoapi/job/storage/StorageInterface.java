package dev.fassi.conversaoapi.job.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface StorageInterface {

    String salvar(MultipartFile video, UUID jobId) throws IOException;
    Resource ler(String path);
    void deletar(String path);
}
