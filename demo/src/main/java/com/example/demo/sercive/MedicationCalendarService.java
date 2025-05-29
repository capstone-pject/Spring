// demo/src/main/java/com/example/demo/Sercive/MedicationCalendarService.java
package com.example.demo.sercive;

import com.example.demo.dto.MedicationCalendarDto;
import com.example.demo.entity.MedicationCalendar;
import com.example.demo.entity.UserInfo;
import com.example.demo.repo.MedicationCalendarRepository;
import com.example.demo.repo.UserInfoRepo;
import com.example.demo.sercive.utile.ConversionService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // LocalDate 임포트
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationCalendarService {

    private final MedicationCalendarRepository medicationCalendarRepository;
    private final UserInfoRepo userInfoRepository;
    private final ConversionService conversionService;

    public MedicationCalendarService(MedicationCalendarRepository medicationCalendarRepository,
                                     UserInfoRepo userInfoRepository,
                                     ConversionService conversionService) {
        this.medicationCalendarRepository = medicationCalendarRepository;
        this.userInfoRepository = userInfoRepository;
        this.conversionService = conversionService;
    }

    private MedicationCalendarDto mapEntityToDtoWithUser(MedicationCalendar entity) {
        MedicationCalendarDto dto = conversionService.convertToDto(entity, MedicationCalendarDto.class);
        if (entity.getUserInfo() != null) {
            dto.setUserId(entity.getUserInfo().getUserId());
        }
        return dto;
    }

    @Transactional
    public MedicationCalendarDto addMedicationSchedule(MedicationCalendarDto scheduleDto) {
        UserInfo userInfo = userInfoRepository.findByUserId(scheduleDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with userId: " + scheduleDto.getUserId()));

        MedicationCalendar medicationCalendar = conversionService.convertToEntity(scheduleDto, MedicationCalendar.class);
        medicationCalendar.setUserInfo(userInfo);
        medicationCalendar.setId(null);

        MedicationCalendar savedEntity = medicationCalendarRepository.save(medicationCalendar);
        System.out.println("새로운 약물 일정 추가 완료: " + savedEntity.getMedicationName() + " (ID: " + savedEntity.getId() + ", UserID: " + userInfo.getUserId() + ")");
        return mapEntityToDtoWithUser(savedEntity);
    }

    @Transactional(readOnly = true)
    public MedicationCalendarDto getMedicationScheduleById(Long id) {
        MedicationCalendar entity = medicationCalendarRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medication schedule not found with id: " + id));
        return mapEntityToDtoWithUser(entity);
    }

    @Transactional(readOnly = true)
    public List<MedicationCalendarDto> getAllMedicationSchedules() {
        return medicationCalendarRepository.findAll().stream()
                .map(this::mapEntityToDtoWithUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicationCalendarDto> getMedicationSchedulesByUserId(String userId) {
        UserInfo userInfo = userInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with userId: " + userId));
        
        return medicationCalendarRepository.findByUserInfo(userInfo).stream()
                .map(this::mapEntityToDtoWithUser)
                .collect(Collectors.toList());
    }

    // 시나리오 1: 기간별 약물 일정 조회를 위한 서비스 메서드 추가
    @Transactional(readOnly = true)
    public List<MedicationCalendarDto> getMedicationSchedulesByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        UserInfo userInfo = userInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with userId: " + userId));
        
        // startDate와 endDate의 유효성 검사 (예: startDate가 endDate보다 늦으면 안 됨) - 필요시 추가
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must be provided.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date.");
        }

        return medicationCalendarRepository.findByUserInfoAndDateRange(userInfo, startDate, endDate).stream()
                .map(this::mapEntityToDtoWithUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicationCalendarDto updateMedicationSchedule(Long scheduleId, MedicationCalendarDto updatedDto) {
        MedicationCalendar existingEntity = medicationCalendarRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Medication schedule not found with id: " + scheduleId + " for update."));
        
        Long existingId = existingEntity.getId();
        UserInfo existingUserInfo = existingEntity.getUserInfo();

        conversionService.updateEntityFromDto(updatedDto, existingEntity);
        existingEntity.setId(existingId);

        if (updatedDto.getUserId() != null && (existingUserInfo == null || !updatedDto.getUserId().equals(existingUserInfo.getUserId()))) {
            UserInfo newUserInfo = userInfoRepository.findByUserId(updatedDto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with userId: " + updatedDto.getUserId()));
            existingEntity.setUserInfo(newUserInfo);
        } else if (updatedDto.getUserId() == null && existingUserInfo != null) {
            // 기존 사용자 정보를 유지하거나, 연결을 해제할 수 있습니다. 현재는 유지됩니다.
             existingEntity.setUserInfo(existingUserInfo);
        }

        MedicationCalendar updatedEntity = medicationCalendarRepository.save(existingEntity);
        return mapEntityToDtoWithUser(updatedEntity);
    }

    @Transactional
    public void deleteMedicationSchedule(Long id) {
        if (!medicationCalendarRepository.existsById(id)) {
            throw new EntityNotFoundException("Medication schedule not found with id: " + id + " for deletion.");
        }
        medicationCalendarRepository.deleteById(id);
        System.out.println("약물 일정 삭제 완료 (ID: " + id + ")");
    }

    @Transactional(readOnly = true)
    public List<MedicationCalendarDto> findByMedicationNameForUser(String userId, String medicationName) {
        UserInfo userInfo = userInfoRepository.findByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with userId: " + userId));
        return medicationCalendarRepository.findByUserInfoAndMedicationNameContainingIgnoreCase(userInfo, medicationName).stream()
            .map(this::mapEntityToDtoWithUser)
            .collect(Collectors.toList());
    }
}