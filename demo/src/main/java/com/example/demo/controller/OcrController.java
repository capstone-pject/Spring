package com.example.demo.controller;

import com.example.demo.Sercive.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImageAndExtractText(@RequestParam("imageFile") MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Image file is required."));
        }

        try {
            String extractedText = ocrService.extractTextFromImage(imageFile);
            if (extractedText.isEmpty() || extractedText.startsWith("No text")) {
                 return ResponseEntity.ok(Collections.singletonMap("extractedText", "")); // 텍스트가 없거나 못 찾았으면 빈 문자열
            }
            return ResponseEntity.ok(Collections.singletonMap("extractedText", extractedText));
        } catch (IllegalArgumentException e) {
             return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } 
        catch (IOException e) {
            e.printStackTrace(); // 서버 로그에 스택 트레이스 기록
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonMap("error", "Failed to process image: " + e.getMessage()));
        } catch (RuntimeException e) { // OCR 처리 중 발생할 수 있는 런타임 예외
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Collections.singletonMap("error", "Error during OCR processing: " + e.getMessage()));
        }
    }
}