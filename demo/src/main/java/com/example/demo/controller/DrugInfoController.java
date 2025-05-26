// ... existing code ...
package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType; // 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Dto.DrugInfoDto;
import com.example.demo.Sercive.DrugInfoService;
// import com.example.demo.Sercive.OcrService;
import com.example.demo.Sercive.OcrService;

import io.swagger.v3.oas.annotations.Operation; // 추가
import io.swagger.v3.oas.annotations.Parameter; // 추가
import io.swagger.v3.oas.annotations.media.ArraySchema; // 추가
import io.swagger.v3.oas.annotations.media.Content; // 추가
import io.swagger.v3.oas.annotations.media.Schema; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponse; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponses; // 추가
import io.swagger.v3.oas.annotations.tags.Tag; // 추가

import java.util.List;

@Tag(name = "의약품 정보 API", description = "의약품 정보 조회 관련 API 명세입니다.") // 컨트롤러 레벨 태그 추가
@RestController
@RequestMapping("/api/drugs")
public class DrugInfoController {
        @Autowired
      DrugInfoService drugInfoService;
      @Autowired 
      OcrService ocrService; // OcrService는 현재 이 컨트롤러에서 사용되지 않는 것 같습니다.

  
    @Operation(summary = "의약품 이름으로 검색", description = "제공된 의약품 이름(itemName)으로 의약품 정보를 검색하여 리스트 형태로 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공", 
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
            array = @ArraySchema(schema = @Schema(implementation = DrugInfoDto.class)))), // DrugInfoDto 배열 반환 명시
        @ApiResponse(responseCode = "404", description = "검색 결과 없음", content = @Content) 
    })
    @GetMapping("/name")
    public ResponseEntity<List<DrugInfoDto>> getDrugByName(
        @Parameter(description = "검색할 의약품 이름", required = true, example = "타이레놀") @RequestParam String itemName) {
        System.out.println("Received request for itemName: " + itemName);
        List<DrugInfoDto> drugInfoList = drugInfoService.getDrugInfoByName(itemName);

        if (drugInfoList.isEmpty()) {
            // 검색 결과가 없을 경우 404 Not Found 또는 적절한 응답 반환
            return ResponseEntity.notFound().build();
        }
        // 검색 결과가 있을 경우 200 OK 와 함께 JSON 데이터 반환
        return ResponseEntity.ok(drugInfoList);
    }

    // 여기에 다른 API 엔드포인트들이 있다면 유사하게 어노테이션을 추가할 수 있습니다.



}