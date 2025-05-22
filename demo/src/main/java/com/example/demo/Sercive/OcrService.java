package com.example.demo.Sercive;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation; // 상세 텍스트 정보를 위해 사용
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.TextAnnotation; // TextAnnotation 사용
import com.google.cloud.vision.v1.BoundingPoly; // BoundingPoly 사용
import com.google.cloud.vision.v1.Vertex; // Vertex 사용
import com.google.protobuf.ByteString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OcrService {

    private static final String KEY_FILE_PATH = "gcp-credentials.json";

    // 기존: 전체 이미지에서 텍스트 추출
    public String extractTextFromImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        // ... (이전 extractTextFromImage 메소드의 나머지 부분은 동일하게 유지)
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());

        Image img = Image.newBuilder().setContent(imgBytes).build();
        // TEXT_DETECTION은 전체 텍스트 블록을 주지만, DOCUMENT_TEXT_DETECTION이 더 구조화된 정보를 제공합니다.
        // 여기서는 기존 로직을 유지하되, ROI 처리를 위해 상세 정보 접근이 필요하면 DOCUMENT_TEXT_DETECTION 고려.
        // 우선 TEXT_DETECTION으로도 boundingPoly 정보를 활용해 보겠습니다.
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        ImageAnnotatorClient client = null;
        try {
            client = createImageAnnotatorClient(); // 클라이언트 생성 로직 분리
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty() || responses.get(0).getTextAnnotationsList().isEmpty()) {
                return "No text found in image.";
            }
            // 전체 텍스트 반환 (첫 번째 TextAnnotation이 보통 전체 텍스트임)
            return responses.get(0).getTextAnnotationsList().get(0).getDescription().trim().replaceAll("\\s*\\n\\s*", " ");
        } catch (Exception e) {
            System.err.println("Failed to process image with Google Cloud Vision API: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process image with Google Cloud Vision API.", e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    // 새로운 메소드: 지정된 ROI(Region of Interest) 내의 텍스트만 추출
    public String extractTextFromImageRoi(MultipartFile file, int roiX, int roiY, int roiWidth, int roiHeight) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (roiWidth <= 0 || roiHeight <= 0) {
            throw new IllegalArgumentException("ROI width and height must be positive.");
        }

        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());

        Image img = Image.newBuilder().setContent(imgBytes).build();
        // ROI 처리를 위해서는 개별 단어/블록의 위치 정보가 필요하므로 TEXT_DETECTION 또는 DOCUMENT_TEXT_DETECTION 사용
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        ImageAnnotatorClient client = null;
        try {
            client = createImageAnnotatorClient();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) {
                return ""; // 텍스트 없음
            }

            AnnotateImageResponse res = responses.get(0);
            if (res.hasError()) {
                System.err.println("Error during OCR: " + res.getError().getMessage());
                throw new RuntimeException("Error during OCR: " + res.getError().getMessage());
            }

            List<EntityAnnotation> annotations = res.getTextAnnotationsList();
            if (annotations.isEmpty()) {
                return ""; // 텍스트 없음
            }

            // 첫 번째 TextAnnotation은 전체 이미지에 대한 텍스트이므로 제외하고,
            // 나머지 개별 단어/텍스트 블록들에 대해 ROI와 겹치는지 확인합니다.
            // (Vision API 응답에서 첫 번째 annotation은 전체 텍스트, 나머지는 단어/기호 단위)
            List<EntityAnnotation> roiAnnotations = new ArrayList<>();
            for (int i = 1; i < annotations.size(); i++) { // i = 0 은 전체 텍스트이므로 건너뜀
                EntityAnnotation annotation = annotations.get(i);
                BoundingPoly poly = annotation.getBoundingPoly();
                if (isOverlapping(poly, roiX, roiY, roiWidth, roiHeight)) {
                    roiAnnotations.add(annotation);
                }
            }

            if (roiAnnotations.isEmpty()) {
                return "";
            }
            
            // ROI 내 텍스트들을 읽기 좋은 순서로 정렬 (y좌표 우선, 그 다음 x좌표)
            // Google Vision API가 이미 어느정도 순서를 맞춰주지만, 명시적으로 정렬할 수도 있습니다.
            // 여기서는 API가 제공하는 순서를 신뢰하고 그대로 조합합니다.
            // 좀 더 정교하게 하려면, 텍스트 블록을 라인별로 그룹화하고, 라인 내에서 x 순서로 정렬하는 로직이 필요합니다.
            // 여기서는 간단히 발견된 순서대로 조합합니다.
            // 또는 y좌표, x좌표 순으로 정렬하여 조합할 수 있습니다.
             roiAnnotations.sort(Comparator.comparingInt((EntityAnnotation a) -> getTopLeftY(a.getBoundingPoly()))
                                    .thenComparingInt(a -> getTopLeftX(a.getBoundingPoly())));


            return roiAnnotations.stream()
                                 .map(EntityAnnotation::getDescription)
                                 .collect(Collectors.joining(" ")) // 공백으로 단어 구분
                                 .trim()
                                 .replaceAll("\\s*\\n\\s*", " "); // 혹시 모를 개행문자 정리

        } catch (Exception e) {
            System.err.println("Failed to process image with Google Cloud Vision API (ROI): " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process image with Google Cloud Vision API (ROI).", e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    // ImageAnnotatorClient 생성 로직 분리 (재사용 위함)
    private ImageAnnotatorClient createImageAnnotatorClient() throws IOException {
        ClassPathResource resource = new ClassPathResource(KEY_FILE_PATH);
        if (!resource.exists()) {
            throw new IOException("Credential file not found in classpath: " + KEY_FILE_PATH);
        }
        try (InputStream credentialsStream = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            ImageAnnotatorSettings imageAnnotatorSettings =
                    ImageAnnotatorSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build();
            return ImageAnnotatorClient.create(imageAnnotatorSettings);
        }
    }

    // BoundingPoly가 주어진 ROI와 겹치는지 확인하는 헬퍼 메소드
    private boolean isOverlapping(BoundingPoly poly, int roiX, int roiY, int roiWidth, int roiHeight) {
        if (poly == null || poly.getVerticesList().isEmpty()) {
            return false;
        }

        // BoundingPoly의 최소/최대 x, y 좌표를 구합니다.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Vertex vertex : poly.getVerticesList()) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
        }

        // ROI의 끝 좌표
        int roiEndX = roiX + roiWidth;
        int roiEndY = roiY + roiHeight;

        // 두 사각형이 겹치는지 확인 (AABB 충돌 감지)
        // 한 사각형이 다른 사각형의 외부에 완전히 있지 않으면 겹치는 것임
        boolean noOverlap = minX >= roiEndX ||  // BoundingPoly가 ROI의 오른쪽에 있음
                            maxX <= roiX ||      // BoundingPoly가 ROI의 왼쪽에 있음
                            minY >= roiEndY ||  // BoundingPoly가 ROI의 아래쪽에 있음
                            maxY <= roiY;       // BoundingPoly가 ROI의 위쪽에 있음
        
        return !noOverlap;
        // 또는, 특정 비율 이상 겹치는 경우만 true로 할 수도 있습니다. (더 복잡한 로직)
    }
     private int getTopLeftY(BoundingPoly poly) {
        return poly.getVerticesList().stream().mapToInt(Vertex::getY).min().orElse(0);
    }

    private int getTopLeftX(BoundingPoly poly) {
        return poly.getVerticesList().stream().mapToInt(Vertex::getX).min().orElse(0);
    }
}