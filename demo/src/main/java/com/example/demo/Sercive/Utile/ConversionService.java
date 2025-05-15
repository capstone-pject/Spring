// demo/src/main/java/com/example/demo/Sercive/Utile/ConversionService.java
package com.example.demo.Sercive.Utile;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;


@Service
public class ConversionService {
    private final ModelMapper modelMapper;

    public ConversionService() {
        this.modelMapper = new ModelMapper();
        // ModelMapper 기본 설정 (필요에 따라 커스터마이징)
        // 예: private 필드도 매핑 대상으로 설정, 엄격한 매칭 전략 사용 등
        this.modelMapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT) // 필드명이 정확히 일치해야 매핑
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(AccessLevel.PRIVATE);
            // .setPropertyCondition(Conditions.isNotNull()); // DTO의 필드가 null이 아닐 때만 매핑 (선택 사항)
    }

    /**
     * DTO를 새로운 엔티티 객체로 변환합니다.
     * @param <D> DTO 타입
     * @param <E> 엔티티 타입
     * @param dto 변환할 DTO 객체
     * @param entityClass 변환될 엔티티의 클래스
     * @return 새로 생성된 엔티티 객체
     */
    public <D, E> E convertToEntity(D dto, Class<E> entityClass) {
        E entity = modelMapper.map(dto, entityClass);
        return entity;
    }

    /**
     * 엔티티를 새로운 DTO 객체로 변환합니다.
     * @param <E> 엔티티 타입
     * @param <D> DTO 타입
     * @param entity 변환할 엔티티 객체
     * @param dtoClass 변환될 DTO의 클래스
     * @return 새로 생성된 DTO 객체
     */
    public <E, D> D convertToDto(E entity, Class<D> dtoClass) {
        D dto = modelMapper.map(entity, dtoClass);
        return dto;
    }

    /**
     * DTO의 내용으로 기존 엔티티 객체의 필드를 업데이트합니다.
     * @param <D> DTO 타입 (소스)
     * @param <E> 엔티티 타입 (목적지)
     * @param dto 소스 DTO 객체
     * @param entity 업데이트될 목적지 엔티티 객체
     */
    public <D, E> void updateEntityFromDto(D dto, E entity) {
        modelMapper.map(dto, entity); // dto의 값들을 entity 객체에 매핑 (업데이트)
    }
}