package com.example.demo.Repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.UserInfo;

public interface UserInfoRepo  extends JpaRepository<UserInfo, Integer>{
    
    Optional<UserInfo> findByUserId(String userId);

}
