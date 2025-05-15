// demo/src/main/java/com/example/demo/controller/MedicationCalendarController.java
package com.example.demo.controller;

import com.example.demo.Dto.MedicationCalendarDto;
import com.example.demo.Sercive.MedicationCalendarService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; // 날짜 파싱을 위해 추가
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // LocalDate 임포트
import java.util.List;

@RestController
@RequestMapping("/api/medication-schedules")
public class MedicationCalendarController {

    @Autowired
  MedicationCalendarService medicationCalendarService;



    // 시나리오 1: 달력에 기간별 약물 일정 표시를 위한 API
    // GET /api/medication-schedules/user/{userId}/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<?> getSchedulesByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getMedicationSchedulesByUserIdAndDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            // 예: 사용자를 찾을 수 없음
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 예: 날짜 파라미터 오류
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching schedules by date range for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }


    // 시나리오 2: 약물 검색, 정보 기입 후 제출 (기존 API 사용)
    // POST /api/medication-schedules
    @PostMapping
    public ResponseEntity<?> addMedicationSchedule(@RequestBody MedicationCalendarDto scheduleDto) {
        try {
            MedicationCalendarDto createdSchedule = medicationCalendarService.addMedicationSchedule(scheduleDto);
            return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 사용자를 찾지 못한 경우
        } catch (Exception e) {
            System.err.println("Unexpected error adding medication schedule: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    // 특정 ID의 약물 복용 일정 조회 (GET /api/medication-schedules/{id})
    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicationScheduleById(@PathVariable Long id) {
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

    // 특정 사용자의 모든 약물 복용 일정 조회 (GET /api/medication-schedules/user/{userId})
    // 이 API는 기간별 조회가 있으므로 중복될 수 있으나, 모든 일정을 한 번에 가져오는 용도로 남겨둘 수 있습니다.
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMedicationSchedulesByUserId(@PathVariable String userId) {
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getMedicationSchedulesByUserId(userId);
            // schedules가 비어있어도 200 OK와 빈 리스트 반환
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching schedules for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    
    // (선택적) 모든 약물 복용 일정 조회 (GET /api/medication-schedules)
    @GetMapping
    public ResponseEntity<List<MedicationCalendarDto>> getAllMedicationSchedules() {
        // 이 API는 인증/인가를 통해 관리자만 접근 가능하도록 하는 것이 일반적입니다.
        try {
            List<MedicationCalendarDto> schedules = medicationCalendarService.getAllMedicationSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.err.println("Error fetching all medication schedules: " + e.getMessage());
            e.printStackTrace();
            // 반환 타입을 ResponseEntity<?> 로 변경하고 에러 메시지 body에 포함 가능
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 특정 ID의 약물 복용 일정 수정 (PUT /api/medication-schedules/{id})
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicationSchedule(@PathVariable Long id, @RequestBody MedicationCalendarDto scheduleDto) {
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

    // 특정 ID의 약물 복용 일정 삭제 (DELETE /api/medication-schedules/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationSchedule(@PathVariable Long id) {
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

    // 특정 사용자의 약물 이름으로 검색 (GET /api/medication-schedules/user/{userId}/search)
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<?> searchMedicationForUser(
            @PathVariable String userId,
            @RequestParam String medicationName) {
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