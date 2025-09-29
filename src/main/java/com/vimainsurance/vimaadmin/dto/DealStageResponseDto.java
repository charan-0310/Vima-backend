package com.vimainsurance.vimaadmin.dto;

public class DealStageResponseDto {
    private String pipeline;
    private String stage;

    public DealStageResponseDto() {}
    public DealStageResponseDto(String pipeline, String stage) {
        this.pipeline = pipeline;
        this.stage = stage;
    }
    public String getPipeline() { return pipeline; }
    public void setPipeline(String pipeline) { this.pipeline = pipeline; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
} 