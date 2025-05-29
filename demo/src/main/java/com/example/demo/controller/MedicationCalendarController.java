// ... existing code ...
// demo/src/main/java/com/example/demo/controller/MedicationCalendarController.java
package com.example.demo.controller;

import com.example.demo.dto.MedicationCalendarDto;
import com.example.demo.sercive.MedicationCalendarService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; // 날짜 파싱을 위해 추가
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation; // 추가
import io.swagger.v3.oas.annotations.Parameter; // 추가
import io.swagger.v3.oas.annotations.media.ArraySchema; // 추가
import io.swagger.v3.oas.annotations.media.Content; // 추가
import io.swagger.v3.oas.annotations.media.Schema; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponse; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponses; // 추가
import io.swagger.v3.oas.annotations.tags.Tag; // 추가

import java.time.LocalDate; // LocalDate 임포트
import java.util.List;

@Tag(name = "복용일정 관리 API", description = "사용자의 복용 일정 등록, 조회, 수정, 삭제 관련 API 명세입니다.")
@RestController
@RequestMapping("/api/medication-schedules")
public class MedicationCalendarController {

    @Autowired
  MedicationCalendarService medicationCalendarService;


    @Operation(summary = "사용자 기간별 복용 일정 조회", description = "특정 사용자의 지정된 기간 내 복용 일정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = MedicationCalendarDto.class)))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 날짜 형식 오류)", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "404", description = "사용자 또는 일정 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<?> getSchedulesByUserIdAndDateRange(
            @Parameter(description = "사용자 ID", required = true, example = "user123") @PathVariable String userId,
            @Parameter(description = "조회 시작일 (YYYY-MM-DD)", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (YYYY-MM-DD)", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getMedicationSchedulesByUserIdAndDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching schedules by date range for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "복용 일정 추가", description = "새로운 복용 일정을 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "등록 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicationCalendarDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 필수 필드 누락)", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "404", description = "관련 사용자 정보를 찾을 수 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })

    @PostMapping
    public ResponseEntity<?> addMedicationSchedule(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "등록할 복용 일정 정보", required = true, content = @Content(schema = @Schema(implementation = MedicationCalendarDto.class)))
        @RequestBody MedicationCalendarDto scheduleDto) {
        try {
            MedicationCalendarDto createdSchedule = medicationCalendarService.addMedicationSchedule(scheduleDto);
            return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); 
        } catch (Exception e) {
            System.err.println("Unexpected error adding medication schedule: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "특정 ID 복용 일정 조회", description = "ID에 해당하는 복용 일정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicationCalendarDto.class))),
        @ApiResponse(responseCode = "404", description = "일정 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicationScheduleById(
            @Parameter(description = "복용 일정 ID", required = true, example = "1") @PathVariable Long id) {
        try {
            MedicationCalendarDto scheduleDto = medicationCalendarService.getMedicationScheduleById(id);
            return ResponseEntity.ok(scheduleDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching medication schedule by ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "특정 사용자 모든 복용 일정 조회", description = "ID에 해당하는 사용자의 모든 복용 일정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = MedicationCalendarDto.class)))),
        @ApiResponse(responseCode = "404", description = "사용자 또는 일정 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMedicationSchedulesByUserId(
            @Parameter(description = "사용자 ID", required = true, example = "user123") @PathVariable String userId) {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getMedicationSchedulesByUserId(userId);
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching schedules for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    
    @Operation(summary = "모든 복용 일정 조회 (관리자용)", description = "시스템의 모든 복용 일정을 조회합니다. 관리자 권한이 필요할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = MedicationCalendarDto.class)))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류") // 반환 타입이 ResponseEntity<List<MedicationCalendarDto>> 이므로, 여기서는 간단히.
    })
    @GetMapping
    public ResponseEntity<List<MedicationCalendarDto>> getAllMedicationSchedules() {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getAllMedicationSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("Error fetching all medication schedules: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // build() 사용 시 content 없음
        }
    }

    @Operation(summary = "특정 ID 복용 일정 수정", description = "ID에 해당하는 복용 일정을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicationCalendarDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "404", description = "일정 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicationSchedule(
            @Parameter(description = "수정할 복용 일정 ID", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 복용 일정 정보", required = true, content = @Content(schema = @Schema(implementation = MedicationCalendarDto.class)))
            @RequestBody MedicationCalendarDto scheduleDto) {
        try {
            MedicationCalendarDto updatedSchedule = medicationCalendarService.updateMedicationSchedule(id, scheduleDto);
            return ResponseEntity.ok(updatedSchedule);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error updating medication schedule " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "특정 ID 복용 일정 삭제", description = "ID에 해당하는 복용 일정을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공 (No Content)"),
        @ApiResponse(responseCode = "404", description = "일정 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationSchedule(
            @Parameter(description = "삭제할 복용 일정 ID", required = true, example = "1") @PathVariable Long id) {
        try {
            medicationCalendarService.deleteMedicationSchedule(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            System.err.println("Error deleting medication schedule " + id + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error deleting medication schedule " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "사용자 약물 이름으로 복용 일정 검색", description = "특정 사용자의 복용 일정 중 약물 이름으로 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = MedicationCalendarDto.class)))),
        @ApiResponse(responseCode = "404", description = "사용자 또는 검색 결과 없음", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<?> searchMedicationForUser(
            @Parameter(description = "사용자 ID", required = true, example = "user123") @PathVariable String userId,
            @Parameter(description = "검색할 약물 이름", required = true, example = "아스피린") @RequestParam String medicationName) {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.findByMedicationNameForUser(userId, medicationName);
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error searching medication for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}
// ... existing code ...