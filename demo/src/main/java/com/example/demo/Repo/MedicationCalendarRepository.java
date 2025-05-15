// demo/src/main/java/com/example/demo/Repository/MedicationCalendarRepository.java
package com.example.demo.Repo;

import com.example.demo.Entity.MedicationCalendar;
import com.example.demo.Entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicationCalendarRepository extends JpaRepository<MedicationCalendar, Long> {

    List<MedicationCalendar> findByUserInfo(UserInfo userInfo);

    List<MedicationCalendar> findByUserInfoAndMedicationNameContainingIgnoreCase(UserInfo userInfo, String medicationName);

    // 기간별 약물 일정 조회를 위한 쿼리 메소드 추가
    // 복용 시작일(startDate)이 조회 기간 내에 있거나,
    // 복용 종료일(endDate)이 조회 기간 내에 있거나,
    // 복용 기간이 조회 기간 전체를 포함하는 경우를 모두 고려해야 합니다.
    @Query("SELECT mc FROM MedicationCalendar mc WHERE mc.userInfo = :userInfo " +
           "AND ((mc.startDate BETWEEN :queryStartDate AND :queryEndDate) OR " +
           "(mc.endDate BETWEEN :queryStartDate AND :queryEndDate) OR " +
           "(mc.startDate <= :queryStartDate AND mc.endDate >= :queryEndDate))")
    List<MedicationCalendar> findByUserInfoAndDateRange(
            @Param("userInfo") UserInfo userInfo,
            @Param("queryStartDate") LocalDate queryStartDate,
            @Param("queryEndDate") LocalDate queryEndDate
    );
}