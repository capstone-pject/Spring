package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Dto.DrugInfoDto;
import com.example.demo.Sercive.DrugInfoService;
// import com.example.demo.Sercive.OcrService;
import com.example.demo.Sercive.OcrService;

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
public class DrugInfoController {
        @Autowired
      DrugInfoService drugInfoService;
      @Autowired 
      OcrService ocrService;

  
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





}