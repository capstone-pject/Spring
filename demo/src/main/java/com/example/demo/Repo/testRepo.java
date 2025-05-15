package com.example.demo.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.testEntity;

public interface testRepo extends JpaRepository <testEntity, Long> {
    
}
