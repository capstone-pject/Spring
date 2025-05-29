package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserInfo;

public interface UserInfoRepo  extends JpaRepository<UserInfo, Integer>{
    
    Optional<UserInfo> findByUserId(String userId);

}
