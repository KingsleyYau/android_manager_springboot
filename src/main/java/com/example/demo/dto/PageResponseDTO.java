package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageResponseDTO<T> {
    private List<T> datalist;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}