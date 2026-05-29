package com.beadalondo.api.holiday.client;

import com.beadalondo.api.holiday.entity.Holiday;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

@Component
public class HolidayClient {

    private static final DateTimeFormatter LOCDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final String serviceKey;

    public HolidayClient(
            RestClient.Builder restClientBuilder,
            @Value("${kasi.api.base-url:${dataportal.api.holiday-base-url:https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService}}") String baseUrl,
            @Value("${kasi.api.service-key:${dataportal.api.holiday-auth-key:${dataportal.api.auth-key}}}") String serviceKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.serviceKey = serviceKey;
    }

    public List<Holiday> fetchHolidays(int year, int month) {
        validateMonth(month);

        try {
            String xml = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getRestDeInfo")
                            .queryParam("solYear", year)
                            .queryParam("solMonth", String.format("%02d", month))
                            .queryParam("ServiceKey", serviceKey)
                            .queryParam("numOfRows", 20)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseHolidays(xml);
        } catch (RestClientException | ParserConfigurationException | IOException | SAXException |
                 DateTimeParseException | NumberFormatException e) {
            throw new IllegalStateException("공휴일 API 호출 또는 응답 처리 중 오류가 발생했습니다.", e);
        }
    }

    private List<Holiday> parseHolidays(String xml)
            throws ParserConfigurationException, IOException, SAXException {
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("공휴일 API 응답이 비어있습니다.");
        }

        Document document = createDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        document.getDocumentElement().normalize();

        String resultCode = getFirstText(document, "resultCode");
        if (!"00".equals(resultCode)) {
            String resultMsg = getFirstText(document, "resultMsg");
            throw new IllegalStateException(
                    "공휴일 API 에러, resultCode=" + resultCode + ", resultMsg=" + resultMsg
            );
        }

        int totalCount = parseOptionalInt(getFirstText(document, "totalCount"), 0);
        NodeList itemNodes = document.getElementsByTagName("item");
        if (totalCount == 0 || itemNodes.getLength() == 0) {
            return List.of();
        }

        List<Holiday> holidays = new ArrayList<>();
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Node itemNode = itemNodes.item(i);
            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                holidays.add(parseItem((Element) itemNode));
            }
        }

        return holidays;
    }

    private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder();
    }

    private Holiday parseItem(Element item) {
        LocalDate date = LocalDate.parse(getText(item, "locdate"), LOCDATE_FORMATTER);
        String name = getText(item, "dateName");
        String dateKind = getText(item, "dateKind");
        Boolean holiday = "Y".equalsIgnoreCase(getText(item, "isHoliday"));
        Integer seq = parseOptionalInt(getText(item, "seq"), null);

        return new Holiday(date, name, dateKind, holiday, seq);
    }

    private String getFirstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }

        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent().trim();
    }

    private String getText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }

        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent().trim();
    }

    private Integer parseOptionalInt(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return parseInt(value);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("월은 1부터 12 사이여야 합니다. month=" + month);
        }
    }
}
