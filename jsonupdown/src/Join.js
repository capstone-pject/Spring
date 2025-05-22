import React, { useState } from 'react';

export default function Join() {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [birth, setBirth] = useState(''); // 생년월일은 문자열로 관리 (YYYY-MM-DD 형식)

  const handleSubmit = async (event) => {
    event.preventDefault(); // 폼 기본 제출 동작 방지

    const userInfoDto = {
      userId,
      password,
      birth, // 문자열 형태 그대로 전송 (백엔드에서 Date 타입으로 변환 필요)
    };

    try {
      // 백엔드 API 엔드포인트 URL (실제 환경에 맞게 수정 필요)
      const response = await fetch('/users/join', { // proxy 설정을 사용하거나 전체 URL을 입력하세요. 예: 'http://localhost:8080/join'
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(userInfoDto),
      });

      if (response.ok) {
        const result = await response.json();
        console.log('회원가입 성공:', result);
        alert('회원가입에 성공했습니다!');
        // 성공 후 입력 필드 초기화
        setUserId('');
        setPassword('');
        setBirth('');
      } else {
        console.error('회원가입 실패:', response.statusText);
        alert('회원가입에 실패했습니다.');
      }
    } catch (error) {
      console.error('회원가입 요청 중 오류 발생:', error);
      alert('회원가입 요청 중 오류가 발생했습니다.');
    }
  };

  return (
    <div>
      <h2>회원가입</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="userId">아이디:</label>
          <input
            type="text"
            id="userId"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">비밀번호:</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="birth">생년월일:</label>
          <input
            type="date" // 날짜 입력을 위한 타입
            id="birth"
            value={birth}
            onChange={(e) => setBirth(e.target.value)}
            required
          />
        </div>
        <button type="submit">가입하기</button>
      </form>
    </div>
  );
}