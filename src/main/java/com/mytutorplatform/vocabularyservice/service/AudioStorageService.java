package com.mytutorplatform.vocabularyservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class AudioStorageService {
    private static final String AUDIO_DIR = "public/audio";

    @Value("${vocabulary-service.base-url}")
    public String baseUrl;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(AUDIO_DIR));
    }

  public String saveAudioToFile(byte[] audio, String baseName) throws IOException {
    String fileName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    Path baseDir = Path.of(AUDIO_DIR);
    Path path = baseDir.resolve(fileName + ".mp3");

    Files.createDirectories(path.getParent());
    Files.write(
            path,
            audio,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
    );
    return baseUrl + "/audio/" + fileName + ".mp3";
  }
}
