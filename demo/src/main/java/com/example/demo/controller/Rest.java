package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Dto.UserInfoDto;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.Sercive.UserInfoService;

@RequestMapping("/users")
@RestController
public class Rest {

@Autowired
UserInfoService UserInfoService;



    
@GetMapping("/test")
    public ResponseEntity<?> getMethodName() {
        Map<String, String> response = new HashMap<>();
        response.put("id", "을 반환 " );
        response.put("passwd", "이렇게 json");
        response.put("email", "합니다 ");

        return ResponseEntity.ok(response); 
    }


@PostMapping("/register")
public ResponseEntity<?> join(@RequestBody UserInfoDto UserInfoDto) {
    
    System.out.println(UserInfoDto);

    UserInfoService.Join(UserInfoDto);

    return ResponseEntity.ok(UserInfoDto);
}


@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody UserInfoDto userInfoDto) {
    // UserInfoService의 login 메서드 호출
    boolean loginSuccess = UserInfoService.login(userInfoDto);

    if (loginSuccess) {
      
        System.out.println("로그인 성공");
         return ResponseEntity.ok().body("로그인 성공: " + userInfoDto.getUserId());
    } else {
        System.out.println("실패");
        // 로그인 실패 시: 401 Unauthorized 또는 400 Bad Request 와 함께 실패 메시지 반환
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 잘못되었습니다.");
    }
}





}
