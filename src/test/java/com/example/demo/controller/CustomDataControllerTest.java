package com.example.demo.controller;

import com.example.demo.model.ApiResponse;
import com.example.demo.model.CustomData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomDataController.class)
public class CustomDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testParseJsonData() throws Exception {
        CustomData testData = new CustomData();
        testData.setId("1");

        testData.setName("Test JSON Data");

        mockMvc.perform(post("/api/custom-data/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testData)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Data parsed successfully"))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.name").value("Test JSON Data"));
    }

    @Test
    public void testParseXmlData() throws Exception {
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><customData><id>2</id><name>Test XML Data</name></customData>";

        mockMvc.perform(post("/api/custom-data/parse")
                .contentType(MediaType.APPLICATION_XML)
                .content(xmlData))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/ApiResponse/success").string("true"))
                .andExpect(xpath("/ApiResponse/message").string("Data parsed successfully"))
                .andExpect(xpath("/ApiResponse/data/id").string("2"))
                .andExpect(xpath("/ApiResponse/data/name").string("Test XML Data"));
    }

    @Test
    public void testInvalidJsonRequest() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/custom-data/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
}