// ... existing code ...
package com.example.demo.controller;

import com.example.demo.Dto.DrugInfoDto;
import com.example.demo.Sercive.DrugInfoService;
import com.example.demo.Sercive.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation; // 추가
import io.swagger.v3.oas.annotations.Parameter; // 추가
import io.swagger.v3.oas.annotations.media.Content; // 추가
import io.swagger.v3.oas.annotations.media.Schema; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponse; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponses; // 추가
import io.swagger.v3.oas.annotations.tags.Tag; // 추가

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

@Tag(name = "OCR 및 약물 검색 API", description = "이미지에서 텍스트를 추출(OCR)하고, 추출된 텍스트를 기반으로 약물 정보를 검색하는 API입니다.")
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DrugInfoService drugInfoService;

    @Operation(summary = "이미지 영역(ROI) OCR 및 단어별 약물 검색",
               description = "업로드된 이미지의 지정된 영역(ROI)에서 텍스트를 추출하고, 추출된 각 단어로 약물 정보를 검색하여 반환합니다. " +
                             "요청은 'multipart/form-data' 형식이어야 합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 및 검색 성공 또는 부분 성공 (메시지 확인 필요)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(type = "object", example = "{\"ocrTextInRoi\":\"추출된텍스트\", \"searchedTerms\":[\"단어1\",\"단어2\"], \"drugInfos\":[{\"drugCode\":\"123\", \"itemName\":\"약이름\"}]}"))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 이미지 파일 누락, ROI 크기 오류)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류 (이미지 처리, OCR, 또는 검색 중 오류 발생)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}")))
    })
    @PostMapping(value = "/upload-with-roi-and-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImageWithRoiAndSearch(
            @Parameter(description = "업로드할 이미지 파일", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("imageFile") MultipartFile imageFile,
            @Parameter(description = "ROI 시작 x 좌표", required = true, example = "10") @RequestParam("x") int x,
            @Parameter(description = "ROI 시작 y 좌표", required = true, example = "20") @RequestParam("y") int y,
            @Parameter(description = "ROI 너비", required = true, example = "100") @RequestParam("width") int width,
            @Parameter(description = "ROI 높이", required = true, example = "50") @RequestParam("height") int height) {

        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Image file is required."));
        }
        if (width <= 0 || height <= 0) {
             return ResponseEntity.badRequest().body(Collections.singletonMap("error", "ROI width and height must be positive."));
        }

        try {
            String textInRoi = ocrService.extractTextFromImageRoi(imageFile, x, y, width, height);
            if (textInRoi.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "선택한 영역에서 텍스트를 추출하지 못했습니다.",
                    "ocrTextInRoi", textInRoi
                ));
            }
            String[] words = textInRoi.trim().split("\\s+");
            Set<DrugInfoDto> aggregatedResults = new HashSet<>();
            List<String> searchTermsUsed = new ArrayList<>();
            for (String word : words) {
                String searchTerm = word.trim();
                if (searchTerm.length() < 2 || searchTerm.matches("^\\d+$")) {
                    continue;
                }
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

    @Operation(summary = "전체 이미지 OCR 및 단어별 약물 검색",
               description = "업로드된 전체 이미지에서 텍스트를 추출하고, 추출된 각 단어로 약물 정보를 검색하여 반환합니다. " +
                             "요청은 'multipart/form-data' 형식이어야 합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 및 검색 성공 또는 부분 성공 (메시지 확인 필요)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(type = "object", example = "{\"ocrText\":\"추출된전체텍스트\", \"searchedTerms\":[\"단어1\",\"단어2\"], \"drugInfos\":[{\"drugCode\":\"123\", \"itemName\":\"약이름\"}]}"))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 이미지 파일 누락)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}")))
    })
    @PostMapping(value = "/upload-and-search-by-words", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndSearchByWords(
            @Parameter(description = "업로드할 이미지 파일", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("imageFile") MultipartFile imageFile) {
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
                List<String> stopwords = Arrays.asList("SINCE", "일반의약품", "연질캡슐", "캡슐", "정", "제약", "주식회사","연질");
                boolean isStopword = stopwords.stream().anyMatch(sw -> searchTerm.toLowerCase().contains(sw.toLowerCase()));
                 if (isStopword && searchTerm.length() < 4) { 
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
        } catch (Exception e) { 
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Collections.singletonMap("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @Operation(summary = "전체 이미지 OCR (텍스트만 반환)",
               description = "업로드된 전체 이미지에서 텍스트만 추출하여 반환합니다. " +
                             "요청은 'multipart/form-data' 형식이어야 합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 성공",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(type = "object", example = "{\"extractedText\":\"추출된 전체 텍스트\"}"))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 이미지 파일 누락)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류 (텍스트 추출 실패)",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", example = "{\"error\":\"에러 메시지\"}")))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImageAndExtractOnlyText(
            @Parameter(description = "업로드할 이미지 파일", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("imageFile") MultipartFile imageFile) {
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
// ... existing code ...