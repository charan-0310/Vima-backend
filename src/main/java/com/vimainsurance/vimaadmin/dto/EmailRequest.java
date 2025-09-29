package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String to;
    private List<String> toList;
    private String cc;
    private List<String> ccList;
    private String bcc;
    private List<String> bccList;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, Object> templateVariables;
    private boolean isHtml;
    private List<EmailAttachment> attachments;
}
