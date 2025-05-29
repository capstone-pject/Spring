// ... existing code ...
package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserInfoDto;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.sercive.UserInfoService;

import io.swagger.v3.oas.annotations.Operation; // 추가
import io.swagger.v3.oas.annotations.media.Content; // 추가
import io.swagger.v3.oas.annotations.media.ExampleObject; // 추가
import io.swagger.v3.oas.annotations.media.Schema; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponse; // 추가
import io.swagger.v3.oas.annotations.responses.ApiResponses; // 추가
import io.swagger.v3.oas.annotations.tags.Tag; // 추가

@Tag(name = "사용자 API", description = "사용자 관련 API 명세입니다.") // 컨트롤러 레벨 태그 추가
@RequestMapping("/users")
@RestController
public class Rest {

@Autowired
UserInfoService UserInfoService;



    
@Operation(summary = "테스트 API", description = "간단한 JSON 응답을 반환하는 테스트 API입니다.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "성공", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        examples = @ExampleObject(value = "{\"id\": \"을 반환 \", \"passwd\": \"이렇게 json\", \"email\": \"합니다 \"}")))
})
@GetMapping("/test")
    public ResponseEntity<?> getMethodName() {
        Map<String, String> response = new HashMap<>();
        response.put("id", "을 반환 " );
        response.put("passwd", "이렇게 json");
        response.put("email", "합니다 ");
        System.out.println("Test 용도");
        return ResponseEntity.ok(response); 
    }


@Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "회원가입 성공", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = UserInfoDto.class))), // UserInfoDto 스키마 참조
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 필수 필드 누락)") 
})
@PostMapping("/register")
public ResponseEntity<?> join(@RequestBody UserInfoDto UserInfoDto) { // @RequestBody 어노테이션 옆에 UserInfoDto에 대한 설명을 @Parameter 등으로 추가할 수도 있습니다.
    
    System.out.println(UserInfoDto);

    UserInfoService.Join(UserInfoDto);

    return ResponseEntity.ok(UserInfoDto);
}


@Operation(summary = "로그인", description = "사용자 ID와 비밀번호로 로그인을 시도합니다.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "로그인 성공", 
        content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, examples = @ExampleObject(value = "로그인 성공: userId"))),
    @ApiResponse(responseCode = "401", description = "인증 실패 (아이디 또는 비밀번호 오류)",
        content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, examples = @ExampleObject(value = "아이디 또는 비밀번호가 잘못되었습니다.")))
})
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody UserInfoDto userInfoDto) { // DTO에 @Schema 어노테이션을 사용하면 더 좋습니다.
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
