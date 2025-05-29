package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "medication_calendar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicationCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicationName;

    private LocalDate startDate;

    private LocalDate endDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medication_intake_times", joinColumns = @JoinColumn(name = "medication_calendar_id"))
    @Column(name = "intake_time")
    private List<LocalTime> intakeTimes;

    private String dosage;

    private String frequency;

    @Lob
    private String memo;

    // UserInfo와의 다대일 관계 설정
    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩으로 성능 최적화
    @JoinColumn(name = "user_info_id", nullable = false) // 외래 키 컬럼명 지정, null 허용 안 함
    private UserInfo userInfo;
}