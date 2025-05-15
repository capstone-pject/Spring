// src/Calendar.js
import React, { useState, useEffect, useCallback } from 'react';
import './Calendar.css'; // 간단한 스타일링을 위한 CSS 파일

// API 기본 URL
const API_BASE_URL = 'http://localhost:8080/api';

// 간단한 달력 표시 및 API 연동 예제
function Calendar({ userId }) {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);
  const [schedules, setSchedules] = useState({}); // { "YYYY-MM-DD": [schedule1, schedule2] } 형태
  const [showModal, setShowModal] = useState(false);

  // 약물 일정 등록 폼 상태
  const [medicationName, setMedicationName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [intakeTimes, setIntakeTimes] = useState(['09:00']); // 기본 섭취 시간 예시
  const [dosage, setDosage] = useState('');
  const [frequency, setFrequency] = useState('');
  const [memo, setMemo] = useState('');

  // 약물 검색 결과 상태
  const [drugSearchResults, setDrugSearchResults] = useState([]);
  const [drugSearchTerm, setDrugSearchTerm] = useState('');

  // 달력의 해당 월 일정 가져오기
  const fetchSchedules = useCallback(async (year, month) => {
    if (!userId) return;

    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);

    const formatDate = (date) => date.toISOString().split('T')[0]; // YYYY-MM-DD

    try {
      const response = await fetch(
        `${API_BASE_URL}/medication-schedules/user/${userId}/range?startDate=${formatDate(firstDayOfMonth)}&endDate=${formatDate(lastDayOfMonth)}`
      );
      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`일정 로드 실패: ${response.status} ${errorData}`);
      }
      const data = await response.json();
      const newSchedules = {};
      data.forEach(schedule => {
        // startDate부터 endDate까지 모든 날짜에 일정을 추가 (간단한 방식)
        let currentDate = new Date(schedule.startDate);
        const stopDate = new Date(schedule.endDate);
        while (currentDate <= stopDate) {
            const dateStr = formatDate(currentDate);
            if (!newSchedules[dateStr]) {
                newSchedules[dateStr] = [];
            }
            newSchedules[dateStr].push(schedule);
            currentDate.setDate(currentDate.getDate() + 1);
        }
      });
      setSchedules(newSchedules);
    } catch (error) {
      console.error("일정 가져오기 오류:", error);
      alert(error.message);
    }
  }, [userId]);

  useEffect(() => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth(); // 0 (Jan) - 11 (Dec)
    fetchSchedules(year, month);
  }, [currentMonth, userId, fetchSchedules]);


  const handleDateClick = (day) => {
    const dateStr = `${currentMonth.getFullYear()}-${String(currentMonth.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    setSelectedDate(dateStr);
    setStartDate(dateStr); // 새 일정 등록 시 시작일 기본값
    setEndDate(dateStr);   // 새 일정 등록 시 종료일 기본값 (필요에 따라 조정)
    setShowModal(true);
    // 폼 초기화
    setMedicationName('');
    setIntakeTimes(['09:00']);
    setDosage('');
    setFrequency('');
    setMemo('');
    setDrugSearchTerm('');
    setDrugSearchResults([]);
  };

  const handlePrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
  };

  const renderHeader = () => {
    const dateFormat = "yyyy년 MM월";
    return (
      <div className="calendar-header">
        <button onClick={handlePrevMonth}>&lt;</button>
        <span>{`${currentMonth.getFullYear()}년 ${currentMonth.getMonth() + 1}월`}</span>
        <button onClick={handleNextMonth}>&gt;</button>
      </div>
    );
  };

  const renderDays = () => {
    const days = ["일", "월", "화", "수", "목", "금", "토"];
    return (
      <div className="days-header">
        {days.map((day, i) => <div key={i} className="day-label">{day}</div>)}
      </div>
    );
  };

  const renderCells = () => {
    const monthStart = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
    const monthEnd = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0);
    const startDateCal = new Date(monthStart);
    startDateCal.setDate(startDateCal.getDate() - monthStart.getDay()); // 주의 시작 (일요일)

    const cells = [];
    let day = new Date(startDateCal);

    for (let i = 0; i < 42; i++) { // 최대 6주 * 7일 = 42칸
      const dayOfMonth = day.getDate();
      const dateStr = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(dayOfMonth).padStart(2, '0')}`;
      const isCurrentMonth = day.getMonth() === currentMonth.getMonth();
      
      cells.push(
        <div
          key={day.toISOString()}
          className={`cell ${!isCurrentMonth ? "disabled" : ""} ${dateStr === selectedDate ? "selected" : ""}`}
          onClick={() => isCurrentMonth && handleDateClick(dayOfMonth)}
        >
          <span className="number">{dayOfMonth}</span>
          {schedules[dateStr] && schedules[dateStr].map((s, index) => (
            <div key={`${s.id}-${index}`} className="schedule-item" title={s.medicationName}>
              {s.medicationName.substring(0, 5)}... {/* 간단히 이름 일부만 표시 */}
            </div>
          ))}
        </div>
      );
      day.setDate(day.getDate() + 1);
    }
    return <div className="calendar-grid">{cells}</div>;
  };

  const handleDrugSearch = async (e) => {
    e.preventDefault();
    if (!drugSearchTerm.trim()) return;
    try {
      const response = await fetch(`${API_BASE_URL}/drugs/name?itemName=${encodeURIComponent(drugSearchTerm)}`);
      if (!response.ok) {
        throw new Error('약물 검색 실패');
      }
      const data = await response.json();
      setDrugSearchResults(data);
    } catch (error) {
      console.error("약물 검색 오류:", error);
      alert(error.message);
      setDrugSearchResults([]);
    }
  };

  const selectDrug = (drug) => {
    setMedicationName(drug.itemName);
    // 필요하다면 약물 정보에서 다른 필드도 가져와 폼에 채울 수 있습니다.
    // 예: setFrequency(drug.useMethodQesitm); (데이터 형식에 맞게 파싱 필요)
    setDrugSearchResults([]); // 검색 결과 목록 닫기
    setDrugSearchTerm(drug.itemName);
  };

  const handleTimeChange = (index, value) => {
    const newTimes = [...intakeTimes];
    newTimes[index] = value;
    setIntakeTimes(newTimes);
  };

  const addTimeSlot = () => {
    setIntakeTimes([...intakeTimes, '']);
  };

  const removeTimeSlot = (index) => {
    const newTimes = intakeTimes.filter((_, i) => i !== index);
    setIntakeTimes(newTimes);
  };

  const handleSubmitSchedule = async (e) => {
    e.preventDefault();
    if (!selectedDate || !medicationName || !startDate || !endDate || intakeTimes.some(t => !t)) {
      alert("필수 정보를 모두 입력해주세요 (약물 이름, 시작일, 종료일, 섭취 시간).");
      return;
    }

    const scheduleData = {
      userId: userId, // Calendar 컴포넌트의 props로 받은 userId 사용
      medicationName,
      startDate,
      endDate,
      intakeTimes: intakeTimes.map(t => t + (t.length === 5 ? ":00" : "")), // "HH:mm:ss" 형식으로
      dosage,
      frequency,
      memo,
    };

    try {
      const response = await fetch(`${API_BASE_URL}/medication-schedules`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(scheduleData),
      });
      if (!response.ok) {
        const errorData = await response.json(); // 또는 response.text()
        throw new Error(`일정 등록 실패: ${errorData.message || response.statusText}`);
      }
      // const newSchedule = await response.json(); // 새로 생성된 일정 정보
      setShowModal(false);
      // 일정 다시 로드
      fetchSchedules(currentMonth.getFullYear(), currentMonth.getMonth());
      alert("약물 일정이 성공적으로 등록되었습니다.");
    } catch (error) {
      console.error("일정 등록 오류:", error);
      alert(error.message);
    }
  };


  return (
    <div className="calendar-container">
      {renderHeader()}
      {renderDays()}
      {renderCells()}

      {showModal && (
        <div className="modal">
          <div className="modal-content">
            <span className="close-button" onClick={() => setShowModal(false)}>&times;</span>
            <h2>약물 일정 등록 ({selectedDate})</h2>
            <form onSubmit={handleSubmitSchedule}>
              <div>
                <label>약물 검색:</label>
                <input 
                  type="text" 
                  value={drugSearchTerm} 
                  onChange={(e) => setDrugSearchTerm(e.target.value)} 
                  placeholder="약물 이름 검색"
                />
                <button type="button" onClick={handleDrugSearch} style={{marginLeft: '5px'}}>검색</button>
                {drugSearchResults.length > 0 && (
                  <ul className="drug-search-results">
                    {drugSearchResults.map((drug, index) => (
                      <li key={drug.itemSeq || index} onClick={() => selectDrug(drug)}>
                        {drug.itemName} ({drug.entpName})
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <label>약물 이름: <span style={{color: "red"}}>*</span></label>
                <input type="text" value={medicationName} onChange={(e) => setMedicationName(e.target.value)} required />
              </div>
              <div>
                <label>복용 시작일: <span style={{color: "red"}}>*</span></label>
                <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
              </div>
              <div>
                <label>복용 종료일: <span style={{color: "red"}}>*</span></label>
                <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required />
              </div>
              <div>
                <label>섭취 시간: <span style={{color: "red"}}>*</span></label>
                {intakeTimes.map((time, index) => (
                  <div key={index} style={{ display: 'flex', alignItems: 'center', marginBottom: '5px' }}>
                    <input 
                      type="time" 
                      value={time} 
                      onChange={(e) => handleTimeChange(index, e.target.value)} 
                      required 
                    />
                    {index > 0 && <button type="button" onClick={() => removeTimeSlot(index)} style={{marginLeft: '5px'}}>삭제</button>}
                  </div>
                ))}
                <button type="button" onClick={addTimeSlot}>시간 추가</button>
              </div>
              <div>
                <label>복용량:</label>
                <input type="text" value={dosage} onChange={(e) => setDosage(e.target.value)} />
              </div>
              <div>
                <label>복용 주기:</label>
                <input type="text" value={frequency} onChange={(e) => setFrequency(e.target.value)} />
              </div>
              <div>
                <label>메모:</label>
                <textarea value={memo} onChange={(e) => setMemo(e.target.value)} />
              </div>
              <button type="submit">저장</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Calendar;