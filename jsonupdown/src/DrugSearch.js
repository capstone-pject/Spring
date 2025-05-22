// src/DrugSearch.js
import React, { useState, useRef } from 'react';

const API_DRUGS_URL = 'http://localhost:8080/api/drugs'; // 약물 검색 API
const API_OCR_URL = 'http://localhost:8080/api/ocr';   // OCR API

function DrugSearch() {
  const [itemName, setItemName] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false); // 약물 검색 로딩
  const [ocrLoading, setOcrLoading] = useState(false); // OCR 로딩
  const [ocrError, setOcrError] = useState('');
  const [searched, setSearched] = useState(false);
  const fileInputRef = useRef(null); // 파일 입력 참조

  const handleDrugSearch = async () => {
    if (!itemName.trim()) {
      setError('검색할 약물 이름을 입력해주세요.');
      setSearchResults([]);
      setSearched(true);
      return;
    }

    setLoading(true);
    setError('');
    setSearchResults([]);
    setSearched(true);

    try {
      const response = await fetch(`${API_DRUGS_URL}/name?itemName=${encodeURIComponent(itemName)}`);
      if (response.ok) {
        const data = await response.json();
        setSearchResults(data);
        if (data.length === 0) {
          setError('약물 검색 결과가 없습니다.');
        }
      } else if (response.status === 404) {
        setError('약물 검색 결과가 없습니다. (404 Not Found)');
        setSearchResults([]);
      } else {
        const errorData = await response.text();
        setError(`약물 검색 오류: ${response.status} ${response.statusText}. ${errorData}`);
        setSearchResults([]);
      }
    } catch (err) {
      setError(`약물 검색 중 네트워크 오류: ${err.message}`);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleImageUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) {
      return;
    }

    setOcrLoading(true);
    setOcrError('');
    setError(''); // 기존 약물 검색 에러 초기화
    setSearchResults([]); // 기존 약물 검색 결과 초기화
    setItemName(''); // 기존 약물 이름 초기화

    const formData = new FormData();
    formData.append('imageFile', file);

    try {
      const response = await fetch(`${API_OCR_URL}/upload`, {
        method: 'POST',
        body: formData,
        // headers: { 'Content-Type': 'multipart/form-data' } // FormData는 자동으로 설정해줌
      });

      if (response.ok) {
        const data = await response.json();
        if (data.extractedText) {
          setItemName(data.extractedText); // OCR 결과로 약물 이름 입력 필드 채우기
          setOcrError('');
          // 선택: OCR 결과로 바로 검색 실행
          // if (data.extractedText.trim()) {
          //   handleDrugSearch(); // 이 경우 setItemName이 반영된 후 실행되도록 조정 필요 (useEffect 등)
          // }
        } else if (data.error) {
           setOcrError(`OCR 오류: ${data.error}`);
        } else {
           setOcrError('OCR 결과에서 텍스트를 추출하지 못했습니다.');
        }
      } else {
        const errorData = await response.json(); // 서버에서 JSON 에러 응답을 보낸다고 가정
        setOcrError(`OCR 서버 오류: ${response.status} ${errorData.error || response.statusText}`);
      }
    } catch (err) {
      setOcrError(`OCR 요청 중 네트워크 오류: ${err.message}`);
    } finally {
      setOcrLoading(false);
      // 파일 입력 초기화 (같은 파일 다시 선택 가능하도록)
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif', maxWidth: '700px', margin: '30px auto', border: '1px solid #ddd', borderRadius: '8px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
      <h2 style={{ textAlign: 'center', color: '#333', borderBottom: '1px solid #eee', paddingBottom: '15px' }}>약물 정보 검색</h2>
      
      {/* OCR 섹션 */}
      <div style={{ marginBottom: '25px', padding: '15px', border: '1px dashed #ccc', borderRadius: '4px', backgroundColor: '#fdfdfd' }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px', fontSize: '1.1em', color: '#555' }}>이미지로 약물 이름 추출 (OCR)</h3>
        <input
          type="file"
          accept="image/*"
          onChange={handleImageUpload}
          ref={fileInputRef}
          disabled={ocrLoading}
          style={{ 
            display: 'block', 
            marginBottom: '10px',
            padding: '8px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        />
        {ocrLoading && <p style={{ color: '#007bff' }}>이미지 분석 중...</p>}
        {ocrError && <p style={{ color: 'red' }}>{ocrError}</p>}
      </div>

      {/* 약물 이름으로 검색 섹션 */}
      <div style={{ marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '10px' }}>
        <input
          type="text"
          value={itemName}
          onChange={(e) => {
            setItemName(e.target.value);
            setSearched(false);
            setError('');
          }}
          placeholder="약물 이름을 입력하거나 이미지에서 추출"
          style={{
            padding: '12px',
            fontSize: '16px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            flexGrow: 1,
          }}
        />
        <button
          onClick={handleDrugSearch}
          disabled={loading || ocrLoading} // OCR 중에도 검색 버튼 비활성화
          style={{
            padding: '12px 18px',
            fontSize: '16px',
            cursor: (loading || ocrLoading) ? 'not-allowed' : 'pointer',
            backgroundColor: (loading || ocrLoading) ? '#ccc' : '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            transition: 'background-color 0.2s'
          }}
        >
          {loading ? '검색 중...' : '약물 검색'}
        </button>
      </div>

      {error && (
        <div style={{ color: 'red', marginBottom: '15px', padding: '10px', border: '1px solid red', borderRadius: '4px', backgroundColor: '#ffebee' }}>
          {error}
        </div>
      )}

      {searched && !error && searchResults.length > 0 && (
        <div>
          <h3 style={{ fontSize: '1.2em', color: '#333', marginTop: '20px', borderTop: '1px solid #eee', paddingTop: '15px' }}>검색 결과:</h3>
          <ul style={{ listStyleType: 'none', padding: 0 }}>
            {searchResults.map((drug, index) => (
              <li key={drug.itemSeq || index} style={{ marginBottom: '12px', padding: '12px', border: '1px solid #eee', borderRadius: '4px', backgroundColor: '#f9f9f9' }}>
                <p><strong>제품명:</strong> {drug.itemName}</p>
                <p><strong>업체명:</strong> {drug.entpName}</p>
                <p><strong>효능:</strong> {drug.efcyQesitm}</p>
              </li>
            ))}
          </ul>
        </div>
      )}
      {searched && !error && searchResults.length === 0 && !loading && ( // 검색 시도 후 결과가 없을 때 (로딩 중 아닐 때)
         <p style={{ color: '#777', marginTop: '15px' }}>표시할 약물 정보가 없습니다.</p>
      )}
    </div>
  );
}

export default DrugSearch;