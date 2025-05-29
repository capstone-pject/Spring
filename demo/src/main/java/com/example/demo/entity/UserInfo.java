// demo/src/main/java/com/example/demo/Entity/UserInfo.java
package com.example.demo.entity;

import java.util.ArrayList; // 추가
import java.util.Date;
import java.util.List; // 추가

import jakarta.persistence.*; // 변경

@Entity
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userInfoId; // 필드명은 Java Naming Convention (camelCase) 권장: userInfoId -> id

    private String userId; // 이 필드가 실제 사용자의 고유 ID (로그인 ID)로 사용되는 것 같습니다.

    private String password;

    @Temporal(TemporalType.DATE) // java.util.Date를 LocalDate처럼 날짜만 저장하도록 매핑
    private Date birth;

    // MedicationCalendar와의 일대다 관계 (양방향)
    @OneToMany(mappedBy = "userInfo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicationCalendar> medicationSchedules = new ArrayList<>();


    public UserInfo() {
    }

    // 생성자, getter, setter는 Lombok @Data, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor 등으로 대체 가능

    public UserInfo(int userInfoId, String userId, String password, Date birth) {
        this.userInfoId = userInfoId;
        this.userId = userId;
        this.password = password;
        this.birth = birth;
    }

    public int getUserInfoId() {
        return userInfoId;
    }

    public void setUserInfoId(int userInfoId) {
        this.userInfoId = userInfoId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public List<MedicationCalendar> getMedicationSchedules() {
        return medicationSchedules;
    }

    public void setMedicationSchedules(List<MedicationCalendar> medicationSchedules) {
        this.medicationSchedules = medicationSchedules;
    }

    // 편의 메서드 (양방향 관계 설정 시 유용)
    public void addMedicationSchedule(MedicationCalendar schedule) {
        medicationSchedules.add(schedule);
        schedule.setUserInfo(this);
    }

    public void removeMedicationSchedule(MedicationCalendar schedule) {
        medicationSchedules.remove(schedule);
        schedule.setUserInfo(null);
    }
}