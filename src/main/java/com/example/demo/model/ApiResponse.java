package com.example.demo.model;

import lombok.Data;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@Data
@XmlRootElement(name = "apiResponse")
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    private Map<String, Object> metadata;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, Object data, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.metadata = metadata;
    }
}