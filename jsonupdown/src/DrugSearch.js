// src/DrugSearch.js
import React, { useState, useRef, useEffect } from 'react';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css'; // react-image-crop CSS import

const API_OCR_URL = 'http://localhost:8080/api/ocr';   // OCR API
const API_DRUGS_URL = 'http://localhost:8080/api/drugs'; // 약물 검색 API

// Helper function to get cropped image data (optional, for client-side preview)
function getCroppedImg(image, crop, fileName) {
  const canvas = document.createElement('canvas');
  const scaleX = image.naturalWidth / image.width;
  const scaleY = image.naturalHeight / image.height;
  canvas.width = crop.width;
  canvas.height = crop.height;
  const ctx = canvas.getContext('2d');

  ctx.drawImage(
    image,
    crop.x * scaleX,
    crop.y * scaleY,
    crop.width * scaleX,
    crop.height * scaleY,
    0,
    0,
    crop.width,
    crop.height
  );

  return new Promise((resolve) => {
    canvas.toBlob(blob => {
      if (!blob) {
        console.error('Canvas is empty');
        return;
      }
      blob.name = fileName;
      resolve(blob);
    }, 'image/jpeg'); // 또는 'image/png'
  });
}


function DrugSearchWithOCR() {
  const [itemName, setItemName] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false); // 약물 직접 검색 로딩
  const [ocrLoading, setOcrLoading] = useState(false); // OCR 및 연관 검색 로딩

  const [upImg, setUpImg] = useState(null); // 업로드된 이미지 파일의 src (Data URL)
  const imgRef = useRef(null); // <img /> 태그 참조
  const fileInputRef = useRef(null); // 파일 입력 참조
  const [crop, setCrop] = useState(); // Crop 상태 객체 (x, y, width, height, unit)
  const [completedCrop, setCompletedCrop] = useState(null); // 완료된 crop 정보 (픽셀 단위)
  // const [croppedImageBlob, setCroppedImageBlob] = useState(null); // 잘린 이미지 Blob (선택적: 서버에 잘린 이미지를 보낼 경우)

  const [ocrText, setOcrText] = useState('');
  const [searchedTermsFromOcr, setSearchedTermsFromOcr] = useState([]);


  const onSelectFile = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setCrop(undefined); // 이전 crop 상태 초기화
      setCompletedCrop(null);
      // setCroppedImageBlob(null);
      setOcrText('');
      setSearchedTermsFromOcr([]);
      setSearchResults([]);
      setError('');
      const reader = new FileReader();
      reader.addEventListener('load', () => setUpImg(reader.result?.toString() || ''));
      reader.readAsDataURL(e.target.files[0]);
    }
  };

  // ReactCrop의 onLoad 핸들러: 이미지 로드 시 crop 영역 초기화 (선택적)
  function onImageLoad(e) {
    imgRef.current = e.currentTarget; // imgRef에 현재 이미지 요소 저장
    // const { width, height } = e.currentTarget;
    // 기본 crop 영역을 중앙에 설정 (예시)
    // setCrop(centerCrop(makeAspectCrop({ unit: '%', width: 50 }, 16 / 9, width, height), width, height));
    // 또는 crop 영역을 설정하지 않아 사용자가 자유롭게 선택하도록 함
  }
  
  // OCR 요청 핸들러 (잘린 영역 좌표와 원본 이미지를 서버로 전송)
  const handleOcrWithCrop = async () => {
    if (!completedCrop || !imgRef.current || !upImg) {
      setError('이미지를 업로드하고, 검색할 영역을 선택해주세요.');
      return;
    }

    // 원본 이미지 파일 가져오기 (Data URL에서 File 객체로 변환 또는 fileInputRef 사용)
    // fileInputRef.current.files[0] 를 사용하는 것이 더 간단하고 원본 파일임.
    const originalImageFile = fileInputRef.current?.files?.[0];
    if (!originalImageFile) {
        setError('원본 이미지 파일을 찾을 수 없습니다.');
        return;
    }

    setOcrLoading(true);
    setError('');
    setSearchResults([]);
    setOcrText('');
    setSearchedTermsFromOcr([]);

    const formData = new FormData();
    formData.append('imageFile', originalImageFile); // 원본 이미지 전송
    // completedCrop에는 픽셀 단위의 x, y, width, height가 들어있음
    formData.append('x', Math.round(completedCrop.x));
    formData.append('y', Math.round(completedCrop.y));
    formData.append('width', Math.round(completedCrop.width));
    formData.append('height', Math.round(completedCrop.height));
    // 서버에서 원본 이미지 크기 대비 비율을 계산할 수 있도록 원본 이미지 크기도 전달 (선택적)
    // formData.append('naturalWidth', imgRef.current.naturalWidth);
    // formData.append('naturalHeight', imgRef.current.naturalHeight);


    try {
      // 서버의 새 엔드포인트 호출 (예: /api/ocr/upload-with-roi)
      // 이 엔드포인트는 백엔드에서 이미지와 좌표를 받아 해당 영역 OCR 후 약물 검색까지 수행
      const response = await fetch(`${API_OCR_URL}/upload-with-roi-and-search`, {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        setOcrText(data.ocrTextInRoi || "선택 영역에서 텍스트를 추출하지 못했습니다.");
        setSearchedTermsFromOcr(data.searchedTerms || []);

        if (data.drugInfos && data.drugInfos.length > 0) {
          setSearchResults(data.drugInfos);
        } else if (data.message) {
          setError(data.message);
        } else {
          setError("OCR 처리 후 약물 정보를 찾지 못했습니다.");
        }
      } else {
        const errorData = await response.json();
        setError(`OCR(영역) 및 검색 서버 오류: ${response.status} ${errorData.error || response.statusText}`);
      }
    } catch (err) {
      setError(`OCR(영역) 및 검색 요청 중 네트워크 오류: ${err.message}`);
    } finally {
      setOcrLoading(false);
    }
  };


  // (선택적) 수동으로 이름 입력 후 검색하는 기능
  const handleManualDrugSearch = async () => {
    if (!itemName.trim()) {
      setError('검색할 약물 이름을 입력해주세요.');
      setSearchResults([]);
      return;
    }
    setLoading(true);
    setError('');
    setSearchResults([]);
    setOcrText('');
    setSearchedTermsFromOcr([]);
    setUpImg(null); // 수동 검색 시 이미지 관련 정보 초기화
    setCrop(undefined);
    setCompletedCrop(null);


    try {
      const response = await fetch(`${API_DRUGS_URL}/name?itemName=${encodeURIComponent(itemName)}`);
      if (response.ok) {
        const data = await response.json();
        setSearchResults(data);
        if (data.length === 0) setError('약물 검색 결과가 없습니다.');
      } else if (response.status === 404) {
        setError('약물 검색 결과가 없습니다. (404 Not Found)');
      } else {
        const errorData = await response.text();
        setError(`약물 검색 오류: ${response.status} ${errorData}`);
      }
    } catch (err) {
      setError(`약물 검색 중 네트워크 오류: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };


  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif', maxWidth: '800px', margin: '30px auto', border: '1px solid #ddd', borderRadius: '8px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
      <h2 style={{ textAlign: 'center', color: '#333', borderBottom: '1px solid #eee', paddingBottom: '15px' }}>약물 정보 검색 (영역 지정 OCR 지원)</h2>
      
      {/* 이미지 업로드 섹션 */}
      <div style={{ marginBottom: '20px' }}>
        <input type="file" accept="image/*" onChange={onSelectFile} ref={fileInputRef} style={{ display: 'block', marginBottom: '10px' }}/>
      </div>

      {/* ReactCrop 컴포넌트 및 OCR 실행 버튼 */}
      {upImg && (
        <div style={{ marginBottom: '20px' }}>
          <ReactCrop
            crop={crop}
            onChange={(_, percentCrop) => setCrop(percentCrop)}
            onComplete={(c) => setCompletedCrop(c)} // c는 픽셀 단위의 crop 정보
            // aspect={16 / 9} // 필요시 종횡비 고정
          >
            <img
              ref={imgRef}
              alt="Crop me"
              src={upImg}
              onLoad={onImageLoad}
              style={{ maxHeight: '400px', maxWidth: '100%' }} // 이미지 크기 제한
            />
          </ReactCrop>
          <button
            onClick={handleOcrWithCrop}
            disabled={ocrLoading || !completedCrop || !completedCrop.width || !completedCrop.height}
            style={{ marginTop: '10px', padding: '10px 15px', fontSize: '16px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            {ocrLoading ? '선택 영역 OCR 및 검색 중...' : '선택 영역으로 약물 검색'}
          </button>
        </div>
      )}
      {ocrLoading && !upImg && <p style={{color: '#007bff'}}>이미지 분석 중...</p>}


      {/* OCR 결과 텍스트 표시 */}
      {ocrText && (
        <div style={{ marginBottom: '15px', padding: '10px', backgroundColor: '#e9f5ff', border: '1px solid #b3d7ff', borderRadius: '4px' }}>
          <p style={{ margin: '0 0 5px 0', fontWeight: 'bold', color: '#0056b3' }}>선택 영역 OCR 추출 텍스트:</p>
          <p style={{ margin: 0, fontSize: '0.9em', color: '#333' }}>{ocrText}</p>
           {searchedTermsFromOcr.length > 0 && (
            <p style={{margin: '5px 0 0 0', fontSize: '0.8em', color: '#555'}}>
                (검색 시도 단어: {searchedTermsFromOcr.join(', ')})
            </p>
          )}
        </div>
      )}

      {/* 수동 검색 입력 필드 */}
      <div style={{ marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '10px', padding: '15px', borderTop: '1px solid #eee', marginTop: '20px' }}>
        <input
          type="text"
          value={itemName}
          onChange={(e) => setItemName(e.target.value)}
          placeholder="또는 약물 이름을 직접 입력하세요"
          style={{ padding: '12px', fontSize: '16px', border: '1px solid #ccc', borderRadius: '4px', flexGrow: 1 }}
        />
        <button
          onClick={handleManualDrugSearch}
          disabled={loading || ocrLoading}
          style={{ padding: '12px 18px', fontSize: '16px', cursor: (loading || ocrLoading) ? 'not-allowed' : 'pointer', backgroundColor: (loading || ocrLoading) ? '#ccc' : '#28a745', color: 'white', border: 'none', borderRadius: '4px' }}
        >
          {loading ? '수동 검색 중...' : '수동 검색'}
        </button>
      </div>
      
      {/* 에러 메시지 표시 */}
      {error && (
        <div style={{ color: 'red', marginBottom: '15px', padding: '10px', border: '1px solid red', borderRadius: '4px', backgroundColor: '#ffebee' }}>
          {error}
        </div>
      )}

      {/* 검색 결과 표시 */}
      {searchResults.length > 0 && (
        <div>
          <h3 style={{ fontSize: '1.2em', color: '#333', marginTop: '20px', borderTop: '1px solid #eee', paddingTop: '15px' }}>검색 결과:</h3>
          <ul style={{ listStyleType: 'none', padding: 0 }}>
            {searchResults.map((drug, index) => (
              <li key={`${drug.itemSeq}-${index}`} style={{ marginBottom: '12px', padding: '12px', border: '1px solid #eee', borderRadius: '4px', backgroundColor: '#f9f9f9' }}>
                <p><strong>제품명:</strong> {drug.itemName}</p>
                <p><strong>업체명:</strong> {drug.entpName}</p>
                <p><strong>효능:</strong> {drug.efcyQesitm}</p>
              </li>
            ))}
          </ul>
        </div>
      )}
      {!loading && !ocrLoading && !error && searchResults.length === 0 && (ocrText || itemName || upImg) && (
         <p style={{ color: '#777', marginTop: '15px', textAlign: 'center' }}>표시할 약물 정보가 없습니다.</p>
      )}
    </div>
  );
}

export default DrugSearchWithOCR;
