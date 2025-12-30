package com.ssg9th2team.geharbang.domain.holiday.service;

import com.ssg9th2team.geharbang.domain.holiday.dto.HolidayItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HolidayServiceImpl implements HolidayService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_ROWS = 50;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${holiday.api-base-url:https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService}")
    private String apiBaseUrl;

    @Value("${holiday.service-key:}")
    private String serviceKey;

    @Override
    public List<HolidayItemResponse> getHolidays(int year, int month) {
        validateRequest(year, month);
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("Holiday service key is not configured.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path("/getRestDeInfo")
                .queryParam("ServiceKey", serviceKey)
                .queryParam("solYear", String.format("%04d", year))
                .queryParam("solMonth", String.format("%02d", month))
                .queryParam("numOfRows", MAX_ROWS)
                .queryParam("pageNo", 1)
                .build()
                .encode()
                .toUriString();

        String xml = restTemplate.getForObject(url, String.class);
        return parseHolidayXml(xml);
    }

    private void validateRequest(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("year must be between 1900 and 2100");
        }
    }

    private List<HolidayItemResponse> parseHolidayXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            NodeList items = builder.parse(inputSource).getElementsByTagName("item");

            List<HolidayItemResponse> results = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Node itemNode = items.item(i);
                if (!(itemNode instanceof Element item)) {
                    continue;
                }

                String locdate = getChildText(item, "locdate");
                if (locdate == null || locdate.isBlank()) {
                    continue;
                }

                LocalDate date = LocalDate.parse(locdate.trim(), DATE_FORMAT);
                String name = getChildText(item, "dateName");
                String holidayFlag = getChildText(item, "isHoliday");
                boolean isHoliday = "Y".equalsIgnoreCase(holidayFlag);

                results.add(HolidayItemResponse.builder()
                        .date(date.toString())
                        .name(name)
                        .isHoliday(isHoliday)
                        .build());
            }

            return results;
        } catch (Exception ex) {
            log.error("Failed to parse holiday response", ex);
            return List.of();
        }
    }

    private String getChildText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }
}
