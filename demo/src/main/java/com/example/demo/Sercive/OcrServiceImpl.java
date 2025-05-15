package com.example.demo.Sercive;

import org.springframework.beans.factory.annotation.Value; // Value 어노테이션 추가
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Sercive.Interface.OcrService;

import org.springframework.http.*; // HttpHeaders, HttpEntity, ResponseEntity 추가
import org.springframework.web.client.RestTemplate; // RestTemplate 추가
import org.springframework.util.LinkedMultiValueMap; // 추가
import org.springframework.util.MultiValueMap; // 추가
import org.json.JSONObject; // JSON 객체 사용
import org.json.JSONArray; // JSON 배열 사용

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64; // Base64 인코딩
import java.util.UUID;

@Service
public class OcrServiceImpl implements OcrService {

    // application.properties 또는 yml 파일에서 설정값 주입
    @Value("${ncp.ocr.api.url}") // 예: https://ocr.apigw.ntruss.com/custom/v1/xxxxxxxx/general
    private String apiUrl;

    @Value("${ncp.ocr.secret}")
    private String secretKey;

    private final RestTemplate restTemplate;

    public OcrServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String extractTextFromImageRegion(MultipartFile imageFile, int x, int y, int width, int height) throws IOException {
        try {
            // 1. MultipartFile을 BufferedImage로 변환
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

            // 2. BufferedImage에서 (x, y, width, height) 영역 잘라내기
            // 좌표계 및 영역 유효성 검사 로직 추가 필요
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (x + width > originalImage.getWidth()) width = originalImage.getWidth() - x;
            if (y + height > originalImage.getHeight()) height = originalImage.getHeight() - y;
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid crop dimensions.");
            }
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);

            // 3. 잘라낸 이미지를 byte[]로 변환 (CLOVA OCR은 Base64 인코딩된 이미지 또는 multipart/form-data 지원)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = getImageFormat(imageFile); // 원본 파일 포맷 가져오기
            ImageIO.write(croppedImage, formatName, baos);
            byte[] imageBytes = baos.toByteArray();

            // 4. CLOVA OCR API 요청 준비
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // CLOVA API 스펙에 따라 변경될 수 있음 (multipart/form-data일 수도 있음)
            headers.set("X-OCR-SECRET", secretKey);

            // 요청 본문 생성 (CLOVA OCR API 문서 참조)
            // 아래는 일반적인 OCR (General OCR)의 예시입니다.
            // 요청 형식은 API 종류 및 버전에 따라 다를 수 있으니 공식 문서를 꼭 확인하세요.
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("version", "V2"); // 또는 V1, API 스펙에 따라
            jsonRequest.put("requestId", UUID.randomUUID().toString()); // 고유한 요청 ID
            jsonRequest.put("timestamp", System.currentTimeMillis());
            
            JSONObject imageJson = new JSONObject();
            imageJson.put("format", formatName); // 예: "png", "jpeg"
            imageJson.put("name", "cropped_image"); 
            imageJson.put("data", Base64.getEncoder().encodeToString(imageBytes)); // 이미지를 Base64로 인코딩
            // 만약 API가 이미지 URL을 지원한다면, 이미지를 임시 저장 후 URL을 전달할 수도 있습니다.

            JSONArray imagesArray = new JSONArray();
            imagesArray.put(imageJson);
            jsonRequest.put("images", imagesArray);
            // 언어 설정 (필요시)
            // jsonRequest.put("lang", "ko");


            HttpEntity<String> entity = new HttpEntity<>(jsonRequest.toString(), headers);

            // 5. API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            // 6. 응답 파싱 및 텍스트 추출
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                // CLOVA OCR 응답 구조에 따라 텍스트를 추출합니다.
                // 예시: response.images[0].fields[0].inferText
                JSONArray responseImages = jsonResponse.getJSONArray("images");
                if (responseImages.length() > 0) {
                    JSONObject firstImage = responseImages.getJSONObject(0);
                    if (firstImage.has("inferResult") && "SUCCESS".equals(firstImage.getString("inferResult"))) {
                        JSONArray fields = firstImage.getJSONArray("fields");
                        StringBuilder extractedText = new StringBuilder();
                        for (int i = 0; i < fields.length(); i++) {
                            extractedText.append(fields.getJSONObject(i).getString("inferText")).append(" ");
                        }
                        return extractedText.toString().trim();
                    } else if (firstImage.has("message")) {
                         throw new IOException("OCR API Error: " + firstImage.getString("message"));
                    }
                }
                return ""; // 텍스트 추출 실패 또는 없음
            } else {
                throw new IOException("OCR API request failed with status: " + response.getStatusCode() + " and body: " + response.getBody());
            }

        } catch (Exception e) {
            // 실제 프로덕션 코드에서는 더 상세한 로깅 및 예외 처리가 필요합니다.
            e.printStackTrace();
            throw new IOException("Error processing OCR request: " + e.getMessage(), e);
        }
    }

    // 파일 확장자로부터 이미지 포맷을 가져오는 간단한 헬퍼 메소드
    private String getImageFormat(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "png"; // 기본값 또는 에러 처리
    }
}