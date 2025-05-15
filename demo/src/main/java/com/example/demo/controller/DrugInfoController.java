package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Dto.DrugInfoDto;
import com.example.demo.Sercive.DrugInfoService;
import com.example.demo.Sercive.Interface.OcrService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drugs")
public class DrugInfoController {
        @Autowired
      DrugInfoService drugInfoService;
    //   @Autowired // OcrService 주입
    //   OcrService ocrService;

    //   @GetMapping("/effect")
    //   public ResponseEntity<?> getDrugsByEffect(@RequestParam String effect) {
    //     System.out.println(effect);
    //       return ResponseEntity.ok(drugInfoService.getDrugInfo(effect));
    //   }
  
    @GetMapping("/name")
    public ResponseEntity<List<DrugInfoDto>> getDrugByName(@RequestParam String itemName) {
        System.out.println("Received request for itemName: " + itemName);
        List<DrugInfoDto> drugInfoList = drugInfoService.getDrugInfoByName(itemName);

        if (drugInfoList.isEmpty()) {
            // 검색 결과가 없을 경우 404 Not Found 또는 적절한 응답 반환
            return ResponseEntity.notFound().build();
        }
        // 검색 결과가 있을 경우 200 OK 와 함께 JSON 데이터 반환
        return ResponseEntity.ok(drugInfoList);
    }





// OCR을 통해 약 이름 추출 후 약 정보 검색
// @PostMapping("/ocr/extract-drug-name")
// public List<Map<String, String>> getDrugInfoByOcr(
//         @RequestParam("image") MultipartFile imageFile,
//         @RequestParam("x") int x,
//         @RequestParam("y") int y,
//         @RequestParam("width") int width,
//         @RequestParam("height") int height) {
    
//     String extractedDrugName = null;
//     try {
//         extractedDrugName = ocrService.extractTextFromImageRegion(imageFile, x, y, width, height);
//     } catch (IOException e) {
//         // TODO: 예외 처리 로직 (예: 로깅, 사용자에게 오류 메시지 반환)
//         e.printStackTrace(); // 기본 예외 스택 트레이스 출력
//         // 적절한 HTTP 상태 코드와 함께 오류 응답을 반환할 수 있습니다.
//         // 예: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//         return List.of(); // 임시로 빈 리스트 반환
//     }
    
//     System.out.println("Image received: " + imageFile.getOriginalFilename());
//     System.out.println("Crop region: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
//     System.out.println("Extracted drug name: " + extractedDrugName);

//     if (extractedDrugName != null && !extractedDrugName.trim().isEmpty()) {
//         return drugInfoService.getDrugInfoByName(extractedDrugName.trim());
//     } else {
//         System.out.println("Extracted drug name is empty or null.");
//         return List.of(); 
//     }
// }
}