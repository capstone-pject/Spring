// demo/src/main/java/com/example/demo/Dto/MedicationCalendarDto.java
package com.example.demo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicationCalendarDto {

    private Long id;
    private String userId; // 사용자 ID (String 타입, UserInfo의 userId 필드와 매칭)
    private String medicationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LocalTime> intakeTimes;
    private String dosage;
    private String frequency;
    private String memo;
}