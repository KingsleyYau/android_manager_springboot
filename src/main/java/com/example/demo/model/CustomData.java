package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Map;

@Data
@XmlRootElement(name = "customData")
public class CustomData {
    @XmlElement(required = true)
    private String id;
    private String name;
    
    @XmlElement(required = true)
    private String dataType;
    
    private Map<String, Object> attributes;
    
    // 自定义验证方法
    public boolean isValid() {
        return id != null && !id.isEmpty() && dataType != null;
    }
}