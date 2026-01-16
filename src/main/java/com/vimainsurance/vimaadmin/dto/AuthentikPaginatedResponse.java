package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class AuthentikPaginatedResponse<T> {
    private List<T> results;
    private PaginationInfo pagination;

    @Data
    public static class PaginationInfo {
        private Integer next;
        private Integer previous;
        private Integer count;
        private Integer current;
        private Integer totalPages;
        private Integer startIndex;
        private Integer endIndex;
    }
}

