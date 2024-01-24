package com.batch.process.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MapToPropertiesConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Properties getRequestBodyProperties(Map<String, Object> jobRequest) throws JsonProcessingException {

        if (MapUtils.isEmpty(jobRequest)) {
            return new Properties();
        }
        return getRequestBodyProperties(MAPPER.writeValueAsString(jobRequest));
    }

    public Properties getRequestBodyProperties(String reportJobInput) throws JsonProcessingException {

        if (StringUtils.isEmpty(reportJobInput)) {
            return new Properties();
        }
        JsonNode input = MAPPER.readTree(reportJobInput);
        Iterator<String> fieldNames = input.fieldNames();
        Properties properties = new Properties();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode curNode = input.get(fieldName);
            extractAttributeValue(properties, fieldName, curNode);
        }
        return properties;
    }

    private void extractAttributeValue(Properties properties, String fieldName, JsonNode curNode) {

        if (curNode == null) {
            return;
        }

        if (curNode.isNumber()) {
            properties.setProperty(fieldName, curNode.asText());
        } else if (curNode.isBoolean()) {
            properties.setProperty(fieldName, curNode.asText());
        } else if (curNode.isTextual()) {
            properties.setProperty(fieldName, curNode.asText());
        } else if (curNode.isArray()) {
            properties.setProperty(fieldName, asCsv(curNode.elements()));
        } else if (curNode.isObject()) {
            Iterator<String> fieldNames = curNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fName = fieldNames.next();
                JsonNode node = curNode.get(fName);
                String propertyKey = fieldName + "." + fName;
                extractAttributeValue(properties, propertyKey, node);
            }
        }
    }

    private String asCsv(Iterator<JsonNode> elements) {
        List<String> items = new ArrayList<>();
        elements.forEachRemaining(e -> items.add(e.asText()));
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i) + (i == items.size() - 1 ? "" : ","));
        }
        return sb.toString();
    }

}
