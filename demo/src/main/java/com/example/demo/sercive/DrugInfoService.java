package com.example.demo.sercive;

import com.example.demo.dto.DrugInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DrugInfoService {
    @Value("${drug-api.service-key}")
    private String serviceKey;
    @Value("${drug-api.base-url}")
    private String BASE_URL;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DrugInfoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<DrugInfoDto> getDrugInfoByName(String itemName) {
        try {
            String rawJsonResponse = fetchRawDataFromApi(itemName);
            if (rawJsonResponse == null) {
                return Collections.emptyList(); // API 호출 실패 시
            }

            JsonNode itemsNode = extractItemsNode(rawJsonResponse);
            if (itemsNode == null) {
                return Collections.emptyList(); // JSON 파싱 또는 응답 유효성 검사 실패 시
            }

            return mapItemsToDto(itemsNode);

        } catch (RestClientException e) {
            System.err.println("Error fetching data from API for " + itemName + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON for " + itemName + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } catch (Exception e) { // 그 외 예외 처리
            System.err.println("Unexpected error processing drug info for " + itemName + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String fetchRawDataFromApi(String itemName) throws RestClientException {
        String encodedItemName = UriUtils.encodeQueryParam(itemName, StandardCharsets.UTF_8);
        URI uri = UriComponentsBuilder
                .fromUriString(BASE_URL)
                .queryParam("serviceKey", serviceKey)
                .queryParam("itemName", encodedItemName)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "100") // 필요에 따라 조절
                .queryParam("type", "json")
                .build(true)
                .toUri();

        System.out.println("Requesting API URL: " + uri);
        // RestClientException은 여기서 발생하여 getDrugInfoByName에서 처리될 수 있습니다.
        String response = restTemplate.getForObject(uri, String.class);
        System.out.println("API Response for " + itemName + " received.");
        // System.out.println(response); // 응답이 길 수 있으므로, 필요시에만 활성화
        return response;
    }

    private JsonNode extractItemsNode(String jsonResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonResponse);

        JsonNode header = root.path("header");
        String resultCode = header.path("resultCode").asText();
        String resultMsg = header.path("resultMsg").asText();

        if (!"00".equals(resultCode)) {
            System.err.println("API Error: " + resultMsg + " (Code: " + resultCode + ")");
            return null; // API 자체에서 에러 응답을 준 경우
        }
        if (!"NORMAL SERVICE.".equals(resultMsg) && !"OK".equalsIgnoreCase(resultMsg) && !"SUCCESS".equalsIgnoreCase(resultMsg) ) {
             // "NORMAL SERVICE." 외에 다른 성공 메시지가 있을 수 있으므로, resultCode가 "00"이면 일단 진행하도록 할 수 있습니다.
             // 혹은 좀 더 엄격하게 "NORMAL SERVICE."만 허용할 수도 있습니다.
             // 여기서는 resultCode가 "00"이면 body.items를 반환하도록 단순화합니다.
             System.out.println("API Success with message: " + resultMsg + " (Code: " + resultCode + ")");
        }


        return root.path("body").path("items");
    }

    private List<DrugInfoDto> mapItemsToDto(JsonNode itemsNode) throws JsonProcessingException {
        List<DrugInfoDto> drugInfoList = new ArrayList<>();
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                DrugInfoDto dto = objectMapper.treeToValue(itemNode, DrugInfoDto.class);
                drugInfoList.add(dto);
            }
        } else {
            System.out.println("No items found in JSON or items is not an array.");
        }
        return drugInfoList;
    }
}