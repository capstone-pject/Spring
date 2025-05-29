package com.example.demo.sercive;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserInfoDto;
import com.example.demo.entity.UserInfo;
import com.example.demo.repo.UserInfoRepo;
import com.example.demo.sercive.utile.ConversionService;
@Service
public class UserInfoService {
    
    @Autowired
    UserInfoRepo UserInfoRepo;
@Autowired
ConversionService ConversionService;

public void Join(UserInfoDto UserInfoDto ){

    UserInfo userInfoEntity = ConversionService.convertToEntity(UserInfoDto, UserInfo.class); // UserInfo.class 추가

    UserInfoRepo.save(userInfoEntity);

}
public boolean login(UserInfoDto userInfoDto){
    // UserInfoDto에서 userId 가져오기
    String userId = userInfoDto.getUserId();
    if (userId == null || userId.isEmpty()) {
        return false; // 사용자 ID가 없으면 로그인 실패
    }

    // Repository를 사용하여 userId로 사용자 정보 조회
    Optional<UserInfo> userOptional = UserInfoRepo.findByUserId(userId); // 실제 Repository 메서드명은 다를 수 있습니다.

    // 사용자가 존재하고 비밀번호가 일치하는지 확인
    if (userOptional.isPresent()) {
        UserInfo userInfo = userOptional.get();
        // UserInfoDto에서 password 가져오기
        String providedPassword = userInfoDto.getPassword();
        // 저장된 비밀번호와 제공된 비밀번호 비교 (실제 애플리케이션에서는 해싱된 비밀번호를 비교해야 합니다)
        if (userInfo.getPassword().equals(providedPassword)) {
            return true; // 로그인 성공
        }
    }

    return false; // 사용자가 없거나 비밀번호가 틀리면 로그인 실패
}


}
