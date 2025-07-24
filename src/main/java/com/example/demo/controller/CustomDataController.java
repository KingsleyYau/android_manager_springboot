package com.example.demo.controller;

import com.example.demo.model.CustomData;
import com.example.demo.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-data")
public class CustomDataController {

    /**
     * 解析自定义数据结构接口
     * @param customData 待解析的自定义数据
     * @return 解析结果或错误信息
     */
    @PostMapping(value = "/parse", 
                 consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ApiResponse> parseCustomData(@RequestBody CustomData customData) {
        // 验证数据有效性
        if (!customData.isValid()) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("ID和数据类型(data_type)为必填项");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 模拟数据解析处理
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("数据解析成功");
        response.setData(customData);
        response.setMetadata(Map.of(
            "id", customData.getId(),
            "type", customData.getDataType(),
            "attribute_count", customData.getAttributes() != null ? customData.getAttributes().size() : 0
        ));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}