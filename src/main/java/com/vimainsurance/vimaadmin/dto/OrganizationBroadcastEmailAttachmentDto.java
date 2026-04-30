package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBroadcastEmailAttachmentDto {
    private String fileName;
    private String contentType;
    private String contentBase64;
}
