package com.example.demo.Sercive.Interface;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface OcrService {
    String extractTextFromImageRegion(MultipartFile imageFile, int x, int y, int width, int height) throws IOException;
}
