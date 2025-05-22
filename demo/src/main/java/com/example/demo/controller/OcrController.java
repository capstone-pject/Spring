package com.example.demo.controller;

import com.example.demo.Dto.DrugInfoDto;
import com.example.demo.Sercive.DrugInfoService;
import com.example.demo.Sercive.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/ocr")
// 중요: 프론트엔드 주소에 맞게 origins를 설정하고, allowCredentials가 true이면 "*" 사용 불가
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DrugInfoService drugInfoService;

    /**
     * 사용자가 지정한 이미지 영역(ROI)에서 텍스트를 추출하고,
     * 추출된 텍스트의 단어별로 약물 정보를 검색합니다.
     * @param imageFile 업로드된 이미지 파일
     * @param x ROI 시작 x 좌표
     * @param y ROI 시작 y 좌표
     * @param width ROI 너비
     * @param height ROI 높이
     * @return ROI 내 OCR 텍스트, 검색된 약물 정보 리스트 등을 포함한 응답
     */
    @PostMapping("/upload-with-roi-and-search")
    public ResponseEntity<?> uploadImageWithRoiAndSearch(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("x") int x,
            @RequestParam("y") int y,
            @RequestParam("width") int width,
            @RequestParam("height") int height) {

        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Image file is required."));
        }
        if (width <= 0 || height <= 0) {
             return ResponseEntity.badRequest().body(Collections.singletonMap("error", "ROI width and height must be positive."));
        }

        try {
            // 1. OcrService를 사용하여 지정된 ROI에서 텍스트 추출
            String textInRoi = ocrService.extractTextFromImageRoi(imageFile, x, y, width, height);
            // System.out.println("ROI OCR 추출 텍스트: " + textInRoi);

            if (textInRoi.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "선택한 영역에서 텍스트를 추출하지 못했습니다.",
                    "ocrTextInRoi", textInRoi // 빈 텍스트라도 전달
                ));
            }

            // 2. 추출된 ROI 텍스트를 단어 단위로 분리 및 약물 검색
            String[] words = textInRoi.trim().split("\\s+");
            // System.out.println("ROI에서 분리된 단어들: " + Arrays.toString(words));
            
            Set<DrugInfoDto> aggregatedResults = new HashSet<>();
            List<String> searchTermsUsed = new ArrayList<>();

            for (String word : words) {
                String searchTerm = word.trim();
                // TODO: 여기에 필요한 단어 필터링 로직을 이전과 같이 적용 (길이, 숫자, 불용어 등)
                if (searchTerm.length() < 2 || searchTerm.matches("^\\d+$")) {
                    continue;
                }
                // List<String> stopwords = Arrays.asList(...);
                // if (isStopWord(searchTerm, stopwords)) continue;


                // System.out.println("ROI 단어로 약물 검색 시도: '" + searchTerm + "'");
                searchTermsUsed.add(searchTerm);
                try {
                    List<DrugInfoDto> drugInfos = drugInfoService.getDrugInfoByName(searchTerm);
                    if (drugInfos != null && !drugInfos.isEmpty()) {
                        aggregatedResults.addAll(drugInfos);
                    }
                } catch (Exception e) {
                    System.err.println("ROI 단어 '" + searchTerm + "'로 약물 정보 검색 중 오류 발생: " + e.getMessage());
                }
            }

            List<DrugInfoDto> finalResults = new ArrayList<>(aggregatedResults);

            if (finalResults.isEmpty()) {
                 return ResponseEntity.ok(Map.of(
                    "message", "선택 영역의 텍스트로 검색된 약물 정보가 없습니다.",
                    "ocrTextInRoi", textInRoi,
                    "searchedTerms", searchTermsUsed
                ));
            }

            return ResponseEntity.ok(Map.of(
                "ocrTextInRoi", textInRoi,
                "searchedTerms", searchTermsUsed,
                "drugInfos", finalResults
            ));

        } catch (IllegalArgumentException e) {
             return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } 
        catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonMap("error", "이미지 처리 또는 OCR 중 오류가 발생했습니다: " + e.getMessage()));
        } catch (RuntimeException e) {
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Collections.singletonMap("error", "요청 처리 중 내부 오류가 발생했습니다: " + e.getMessage()));
        }
    }


    // 기존의 전체 이미지 OCR 후 단어별 검색 엔드포인트 (유지)
    @PostMapping("/upload-and-search-by-words")
    public ResponseEntity<?> uploadAndSearchByWords(@RequestParam("imageFile") MultipartFile imageFile) {
        // ... (이전 코드와 동일) ...
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Image file is required."));
        }

        try {
            String fullExtractedText = ocrService.extractTextFromImage(imageFile);
            if (fullExtractedText.isEmpty() || fullExtractedText.startsWith("No text")) {
                return ResponseEntity.ok(Map.of("message", "No text found in image or failed to extract.", "ocrText", fullExtractedText));
            }
            String[] words = fullExtractedText.trim().split("\\s+");
            Set<DrugInfoDto> aggregatedResults = new HashSet<>();
            List<String> searchTermsUsed = new ArrayList<>();
            for (String word : words) {
                 String searchTerm = word.trim();
                if (searchTerm.length() < 2 || searchTerm.matches("\\d+")) {
                    continue;
                }
                // 이전 불용어 처리 로직 등 유지
                List<String> stopwords = Arrays.asList("SINCE", "일반의약품", "연질캡슐", "캡슐", "정", "제약", "주식회사","연질"); // "일반의약품" 중복 제거
                boolean isStopword = stopwords.stream().anyMatch(sw -> searchTerm.toLowerCase().contains(sw.toLowerCase()));
                 if (isStopword && searchTerm.length() < 4) { 
                    // continue; 
                }
                searchTermsUsed.add(searchTerm);
                try {
                    List<DrugInfoDto> drugInfos = drugInfoService.getDrugInfoByName(searchTerm);
                    if (drugInfos != null && !drugInfos.isEmpty()) {
                        aggregatedResults.addAll(drugInfos);
                    }
                } catch (Exception e) {
                    System.err.println("Error searching for drug with term '" + searchTerm + "': " + e.getMessage());
                }
            }
            List<DrugInfoDto> finalResults = new ArrayList<>(aggregatedResults);
            if (finalResults.isEmpty()) {
                 return ResponseEntity.ok(Map.of(
                    "message", "No drug information found for any extracted words.",
                    "ocrText", fullExtractedText,
                    "searchedTerms", searchTermsUsed
                ));
            }
            return ResponseEntity.ok(Map.of(
                "ocrText", fullExtractedText,
                "searchedTerms", searchTermsUsed,
                "drugInfos", finalResults
            ));
        } catch (Exception e) { // 더 포괄적인 예외 처리
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Collections.singletonMap("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    // 기존의 OCR 텍스트만 반환하는 엔드포인트 (유지)
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImageAndExtractOnlyText(@RequestParam("imageFile") MultipartFile imageFile) {
       // ... (이전 코드와 동일) ...
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Image file is required."));
        }
        try {
            String extractedText = ocrService.extractTextFromImage(imageFile);
            return ResponseEntity.ok(Collections.singletonMap("extractedText", extractedText));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Failed to extract text: " + e.getMessage()));
        }
    }
}