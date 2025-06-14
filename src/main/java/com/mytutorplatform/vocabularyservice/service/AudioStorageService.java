package com.mytutorplatform.vocabularyservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioStorageService {
    private static final String AUDIO_DIR = "public/audio";

    @Value("${vocabulary-service.base-url}")
    public String baseUrl;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(AUDIO_DIR));
    }

    public String saveAudioToFile(byte[] audio, String name) throws IOException {
        String fileName = name.toLowerCase() + ".mp3";
        Path path = Paths.get(AUDIO_DIR, fileName);
        Files.write(path, audio);
        return baseUrl + "/audio/" + fileName;
    }
}
