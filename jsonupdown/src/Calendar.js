// src/Calendar.js
import React, { useState, useEffect, useCallback } from 'react';
import './Calendar.css'; // 간단한 스타일링을 위한 CSS 파일

// API 기본 URL
const API_BASE_URL = 'http://localhost:8080/api';
const KOREA_OPEN_API_KEY = 'YOUR_KOREA_OPEN_API_KEY';


function Calendar({ userId }) {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);
  const [schedules, setSchedules] = useState({});
  const [showModal, setShowModal] = useState(false);
  const [selectedDateSchedules, setSelectedDateSchedules] = useState([]);

  const [medicationName, setMedicationName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [intakeTimes, setIntakeTimes] = useState(['09:00']);
  const [dosage, setDosage] = useState('');
  const [frequency, setFrequency] = useState('');
  const [memo, setMemo] = useState('');

  const [drugSearchResults, setDrugSearchResults] = useState([]);
  const [drugSearchTerm, setDrugSearchTerm] = useState('');

  // 날짜 포맷 함수 (로컬 시간 기준 YYYY-MM-DD)
  const formatDateToYYYYMMDD = useCallback((date) => {
    if (!(date instanceof Date) || isNaN(date)) {
        // console.warn("Invalid date passed to formatDateToYYYYMMDD:", date);
        return null; // 혹은 오류 처리
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // getMonth()는 0-11 반환
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }, []);

  const parseYYYYMMDDToDate = (dateString) => {
    if (!dateString || typeof dateString !== 'string') return null;
    const parts = dateString.split('-');
    if (parts.length === 3) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1; // month는 0-indexed
      const day = parseInt(parts[2], 10);
      if (!isNaN(year) && !isNaN(month) && !isNaN(day)) {
        return new Date(year, month, day);
      }
    }
    return null; // 잘못된 형식이면 null 반환 또는 에러 처리
  };


  const fetchSchedules = useCallback(async (year, month) => {
    if (!userId) return;
    // month는 0-indexed (0 = January, 11 = December)
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0); // 다음 달의 0번째 날 = 이번 달의 마지막 날

    try {
      const response = await fetch(
        `${API_BASE_URL}/medication-schedules/user/${userId}/range?startDate=${formatDateToYYYYMMDD(firstDayOfMonth)}&endDate=${formatDateToYYYYMMDD(lastDayOfMonth)}`
      );
      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`일정 로드 실패: ${response.status} ${errorData}`);
      }
      const data = await response.json();
      const newSchedules = {};
      data.forEach(schedule => {
        // 백엔드에서 오는 startDate, endDate는 YYYY-MM-DD 형식의 문자열로 가정
        // 이 문자열을 new Date()로 파싱할 때 타임존 문제를 피하기 위해 명시적으로 분리 후 생성
        const scheduleStartDate = parseYYYYMMDDToDate(schedule.startDate);
        const scheduleEndDate = parseYYYYMMDDToDate(schedule.endDate);

        if (!scheduleStartDate || !scheduleEndDate) {
            console.warn("Invalid schedule date format received from backend:", schedule);
            return; // 이 일정은 건너뜀
        }
        
        let currentDate = new Date(scheduleStartDate.valueOf()); // valueOf() 또는 getTime()으로 복사본 생성

        // currentDate가 scheduleEndDate 보다 크거나 같을 때까지 반복
        // 주의: Date 객체 비교는 getTime()을 사용하는 것이 안전
        while (currentDate.getTime() <= scheduleEndDate.getTime()) {
          const dateStr = formatDateToYYYYMMDD(currentDate);
          
          // 현재 달력에 표시될 날짜들만 newSchedules에 추가
          if (currentDate.getTime() >= firstDayOfMonth.getTime() && currentDate.getTime() <= lastDayOfMonth.getTime()) {
            if (!newSchedules[dateStr]) {
              newSchedules[dateStr] = [];
            }
            newSchedules[dateStr].push(schedule);
          }
          currentDate.setDate(currentDate.getDate() + 1);
        }
      });
      setSchedules(newSchedules);
    } catch (error) {
      console.error("일정 가져오기 오류:", error);
    }
  }, [userId, formatDateToYYYYMMDD]); // formatDateToYYYYMMDD 의존성 추가

  useEffect(() => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth(); // 0-indexed
    fetchSchedules(year, month);
  }, [currentMonth, userId, fetchSchedules]);

  const resetFormFields = () => {
    setMedicationName('');
    setIntakeTimes(['09:00']);
    setDosage('');
    setFrequency('');
    setMemo('');
    setDrugSearchTerm('');
    setDrugSearchResults([]);
  };

  const handleDateClick = (day) => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth(); // 0-indexed
    // 클릭된 날짜에 대한 Date 객체를 로컬 시간 기준으로 정확히 생성
    const clickedDateObj = new Date(year, month, day);
    const dateStr = formatDateToYYYYMMDD(clickedDateObj); // 포맷된 문자열
    
    setSelectedDate(dateStr);
    setSelectedDateSchedules(schedules[dateStr] || []);

    setStartDate(dateStr); 
    setEndDate(dateStr);   

    resetFormFields(); 
    setShowModal(true);
  };

  // ... (handlePrevMonth, handleNextMonth, renderHeader, renderDays 등 나머지 함수들은 이전과 유사하게 유지) ...

  const renderCells = () => {
    const monthStart = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
    // const monthEnd = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0); // monthEnd는 직접 사용되지 않음
    const startDateCal = new Date(monthStart);
    startDateCal.setDate(startDateCal.getDate() - monthStart.getDay()); 

    const cells = [];
    let dayIter = new Date(startDateCal.valueOf()); // 복사본 사용

    for (let i = 0; i < 42; i++) { 
      const dayOfMonth = dayIter.getDate();
      const fullDateStr = formatDateToYYYYMMDD(dayIter); // 로컬 시간 기준 포맷팅
      const isCurrentMonthDay = dayIter.getMonth() === currentMonth.getMonth();
      
      cells.push(
        <div
          key={dayIter.toISOString()} // key는 고유하면 되므로 ISOString 유지 가능하지만, fullDateStr도 좋음
          className={`cell ${!isCurrentMonthDay ? "disabled" : ""} ${fullDateStr === selectedDate ? "selected" : ""} ${(schedules[fullDateStr] && schedules[fullDateStr].length > 0) ? "has-schedule" : ""}`}
          onClick={() => isCurrentMonthDay && handleDateClick(dayOfMonth)}
        >
          <span className="number">{dayOfMonth}</span>
          <div className="schedule-item-sm-container">
            {schedules[fullDateStr] && schedules[fullDateStr].map((s, index) => (
              <div key={`${s.id || s.medicationName}-${index}`} className="schedule-item-sm" title={s.medicationName}>
                {/* 점 표시는 CSS로 처리 */}
              </div>
            ))}
          </div>
        </div>
      );
      dayIter.setDate(dayIter.getDate() + 1);
    }
    return <div className="calendar-grid">{cells}</div>;
  };
  
  const handleSubmitSchedule = async (e) => {
    e.preventDefault();
    // startDate, endDate는 이미 YYYY-MM-DD 형식의 문자열 상태로 관리됨
    if (!selectedDate || !medicationName || !startDate || !endDate || intakeTimes.some(t => !t)) {
      alert("필수 정보를 모두 입력해주세요 (약물 이름, 시작일, 종료일, 섭취 시간).");
      return;
    }

    const scheduleData = {
      userId: userId,
      medicationName,
      startDate, // YYYY-MM-DD 문자열 그대로 사용
      endDate,   // YYYY-MM-DD 문자열 그대로 사용
      intakeTimes: intakeTimes.map(t => t + (t.length === 5 ? ":00" : "")),
      dosage,
      frequency,
      memo,
    };

    try {
      const response = await fetch(`${API_BASE_URL}/api/medication-schedules`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(scheduleData),
      });
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(`일정 등록 실패: ${errorData.message || response.statusText}`);
      }
      const newScheduleResponse = await response.json();

      resetFormFields();
      alert("약물 일정이 성공적으로 등록되었습니다. 달력이 업데이트 됩니다.");
      
      // 중요: fetchSchedules를 다시 호출하여 전체 데이터를 갱신
      await fetchSchedules(currentMonth.getFullYear(), currentMonth.getMonth());
      
      // 모달에 표시된 당일 일정도 업데이트 (선택 사항: fetchSchedules 후 schedules에서 가져오거나, 직접 추가)
      // 다음 렌더링 시 selectedDateSchedules가 schedules[selectedDate]에 의해 업데이트 될 것임
      // 더 즉각적인 반응을 원하면 아래 로직 사용:
      const addedScheduleForDisplay = { ...scheduleData, id: newScheduleResponse.id || Date.now() };
      if (selectedDate === scheduleData.startDate || (parseYYYYMMDDToDate(selectedDate) >= parseYYYYMMDDToDate(scheduleData.startDate) && parseYYYYMMDDToDate(selectedDate) <= parseYYYYMMDDToDate(scheduleData.endDate))) {
           setSelectedDateSchedules(prev => [...(prev || []), addedScheduleForDisplay]);
      }


    } catch (error) {
      console.error("일정 등록 오류:", error);
      alert(error.message);
    }
  };
  
  // (handleDrugSearch, selectDrug, handleTimeChange, addTimeSlot, removeTimeSlot, closeModal, renderHeader, renderDays 등 나머지 함수들은 이전 코드에서 가져옴)
  // ... 이전에 제공된 전체 코드에서 나머지 함수들을 여기에 포함시켜야 합니다 ...
  // 아래는 누락된 함수들의 예시입니다. 실제 코드는 이전 답변을 참고하세요.
  const handlePrevMonth = () => { setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1)); };
  const handleNextMonth = () => { setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1)); };
  const renderHeader = () => { /* 이전 코드 참고 */ return <div className="calendar-header">...</div>; };
  const renderDays = () => { /* 이전 코드 참고 */ return <div className="calendar-days">...</div>; };
  const handleDrugSearch = async () => { /* 이전 코드 참고 */ };
  const selectDrug = (drug) => { /* 이전 코드 참고 */ };
  const handleTimeChange = (index, value) => { /* 이전 코드 참고 */ };
  const addTimeSlot = () => { /* 이전 코드 참고 */ };
  const removeTimeSlot = (index) => { /* 이전 코드 참고 */ };
  const closeModal = () => { /* 이전 코드 참고 */ };


  return (
    <div className="calendar-container">
      {renderHeader()}
      {renderDays()}
      {renderCells()}

      {showModal && selectedDate && (
        <div className="modal">
          <div className="modal-content">
            <span className="close-button" onClick={closeModal}>&times;</span>
            
            <h3>{selectedDate} 약물 정보</h3>
            {selectedDateSchedules && selectedDateSchedules.length > 0 ? ( // selectedDateSchedules가 null일 수 있음을 고려
              <div className="daily-schedules-list">
                {selectedDateSchedules.map((schedule, index) => (
                  <div key={schedule.id || index} className="daily-schedule-item">
                    <p><strong>약물명:</strong> {schedule.medicationName}</p>
                    <p><strong>섭취 시간:</strong> {schedule.intakeTimes ? schedule.intakeTimes.map(t => t.substring(0,5)).join(', ') : '정보 없음'}</p>
                    {schedule.dosage && <p><strong>복용량:</strong> {schedule.dosage}</p>}
                    {schedule.frequency && <p><strong>복용 주기:</strong> {schedule.frequency}</p>}
                    {schedule.memo && <p><strong>메모:</strong> {schedule.memo}</p>}
                  </div>
                ))}
              </div>
            ) : (
              <p>해당 날짜에 등록된 약물 정보가 없습니다.</p>
            )}
            
            <hr style={{margin: "20px 0"}} />

            <h4>새 약물 일정 등록 ({selectedDate})</h4>
            <form onSubmit={handleSubmitSchedule}>
              {/* 폼 필드들 (이전 코드 참고) */}
              <div>
                <label>약물 검색:</label>
                <input type="text" value={drugSearchTerm} onChange={(e) => setDrugSearchTerm(e.target.value)} placeholder="약물 이름 검색"/>
                <button type="button" onClick={handleDrugSearch} style={{marginLeft: '5px'}}>검색</button>
                {drugSearchResults.length > 0 && (
                  <ul className="drug-search-results">
                    {drugSearchResults.map((drug, index) => ( <li key={drug.itemSeq || index} onClick={() => selectDrug(drug)}> {drug.itemName} ({drug.entpName}) </li> ))}
                  </ul>
                )}
              </div>
              <div> <label>약물 이름: <span style={{color: "red"}}>*</span></label> <input type="text" value={medicationName} onChange={(e) => setMedicationName(e.target.value)} required /> </div>
              <div> <label>복용 시작일: <span style={{color: "red"}}>*</span></label> <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required /> </div>
              <div> <label>복용 종료일: <span style={{color: "red"}}>*</span></label> <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required /> </div>
              <div> <label>섭취 시간: <span style={{color: "red"}}>*</span></label> {intakeTimes.map((time, index) => ( <div key={index} style={{ display: 'flex', alignItems: 'center', marginBottom: '5px' }}> <input type="time" value={time} onChange={(e) => handleTimeChange(index, e.target.value)} required /> {index > 0 && <button type="button" onClick={() => removeTimeSlot(index)} style={{marginLeft: '5px'}}>삭제</button>} </div> ))} <button type="button" onClick={addTimeSlot}>시간 추가</button> </div>
              <div> <label>복용량:</label> <input type="text" value={dosage} onChange={(e) => setDosage(e.target.value)} /> </div>
              <div> <label>복용 주기:</label> <input type="text" value={frequency} onChange={(e) => setFrequency(e.target.value)} /> </div>
              <div> <label>메모:</label> <textarea value={memo} onChange={(e) => setMemo(e.target.value)} /> </div>
              <button type="submit">새 일정 저장</button>
            </form>
          </div>  
        </div>
      )}
    </div>
  );
}

export default Calendar;