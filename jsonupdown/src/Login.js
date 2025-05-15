import React, { useState } from 'react';

export default function Login() {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [loginMessage, setLoginMessage] = useState(''); // 로그인 결과 메시지 상태

  const handleSubmit = async (event) => {
    event.preventDefault(); // 폼 기본 제출 동작 방지
    setLoginMessage(''); // 이전 메시지 초기화

    // 백엔드로 보낼 데이터 (UserInfoDto와 유사한 구조, birth는 제외)
    const loginDto = {
      userId,
      password,
    };

    try {
      // 백엔드 로그인 API 엔드포인트 URL
      const response = await fetch('/login', { // 프록시 설정이 적용됩니다.
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(loginDto),
      });

      // 응답 텍스트를 먼저 받아옵니다 (성공/실패 메시지 처리를 위해)
      const responseBody = await response.text();

      if (response.ok) {
        console.log('로그인 성공:', responseBody);
        setLoginMessage(`로그인 성공! 환영합니다, ${userId}님.`); // 성공 메시지 설정
        // 성공 시 추가 작업 가능 (예: 토큰 저장, 다른 페이지로 이동 등)
        // 로그인 성공 후 입력 필드 초기화 (선택 사항)
        // setUserId('');
        // setPassword('');
      } else {
        console.error('로그인 실패:', response.status, responseBody);
        // 백엔드에서 보낸 실패 메시지를 사용하거나 기본 메시지 설정
        setLoginMessage(responseBody || '아이디 또는 비밀번호가 잘못되었습니다.');
      }
    } catch (error) {
      console.error('로그인 요청 중 오류 발생:', error);
      setLoginMessage('로그인 요청 중 오류가 발생했습니다.');
    }
  };

  return (
    <div>
      <h2>로그인</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="loginUserId">아이디:</label>
          <input
            type="text"
            id="loginUserId" // ID 중복 방지를 위해 'login' 접두사 추가
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="loginPassword">비밀번호:</label>
          <input
            type="password"
            id="loginPassword" // ID 중복 방지를 위해 'login' 접두사 추가
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit">로그인</button>
      </form>
      {loginMessage && <p>{loginMessage}</p>} {/* 로그인 결과 메시지 표시 */}
    </div>
  );
}