package com.vimainsurance.vimaadmin.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotesRequestDto {
  private String coverageAmount;
  private String bestPremium;
  private String status;
  private Date createdDate;
  private String customer;
  private String[] companies;
}
