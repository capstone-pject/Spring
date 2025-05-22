package com.example.demo.Sercive;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
// import com.google.cloud.vision.v1.EntityAnnotation; // 사용되지 않음
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings; // 설정 추가
import com.google.protobuf.ByteString;
import org.springframework.core.io.ClassPathResource; // 리소스 로딩용
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    // !!!주의: 이 파일 경로는 임시 테스트용입니다. 실제 배포 시에는 환경 변수를 사용하세요.!!!
    private static final String KEY_FILE_PATH = "gcp-credentials.json"; // src/main/resources 아래 경로

    public String extractTextFromImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        ImageAnnotatorClient client = null;
        try {
            // 클래스패스에서 인증 파일을 읽어옵니다.
            ClassPathResource resource = new ClassPathResource(KEY_FILE_PATH);
            if (!resource.exists()) {
                throw new IOException("Credential file not found in classpath: " + KEY_FILE_PATH);
            }
            InputStream credentialsStream = resource.getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

            // 인증 정보를 사용하여 클라이언트 설정 생성
            ImageAnnotatorSettings imageAnnotatorSettings =
                    ImageAnnotatorSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build();

            // 설정과 함께 클라이언트 생성
            client = ImageAnnotatorClient.create(imageAnnotatorSettings);

            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) {
                return "No text found in image.";
            }

            AnnotateImageResponse res = responses.get(0);
            if (res.hasError()) {
                System.err.println("Error during OCR: " + res.getError().getMessage());
                throw new RuntimeException("Error during OCR: " + res.getError().getMessage());
            }

            if (!res.getTextAnnotationsList().isEmpty()) {
                return res.getTextAnnotationsList().get(0).getDescription().trim().replaceAll("\\n", " ");
            } else {
                return "No text annotations found.";
            }
        } catch (Exception e) {
            System.err.println("Failed to process image with Google Cloud Vision API: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process image with Google Cloud Vision API.", e);
        } finally {
            if (client != null) {
                client.close(); // 클라이언트 사용 후 반드시 닫아주세요.
            }
        }
    }
}