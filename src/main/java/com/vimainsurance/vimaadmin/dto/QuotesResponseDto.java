package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotesResponseDto {
  private String coverageAmount;
  private String bestPremium;
  private String status;
  private LocalDate createdDate;
  private String customer;
  private String[] companies;
}
