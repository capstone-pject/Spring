package com.example.demo.dto;

import java.util.Date;

public class UserInfoDto {
    private int userInfoId;

    private String userId;

    private String password;

    private Date birth;

    private String userName;

    public UserInfoDto() {
    }

    public UserInfoDto(int userInfoId, String userId, String password, Date birth) {
        this.userInfoId = userInfoId;
        this.userId = userId;
        this.password = password;
        this.birth = birth;
    }

    public String getUserName() {
        return userName;
    }


    public void setUserName(String userName) {
        this.userName = userName;
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

    @Override
    public String toString() {
        return "UserInfoDto [userInfoId=" + userInfoId + ", userId=" + userId + ", password=" + password + ", birth="
                + birth + ", userName=" + userName + "]";
    }

}
