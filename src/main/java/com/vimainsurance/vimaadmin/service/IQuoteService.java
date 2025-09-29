package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.QuoteStatusUpdateDto;
import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Quotes;

public interface IQuoteService {
  ResponseEntity<ResponseDto<String>> create(QuotesRequestDto requestDto);
  ResponseEntity<ResponseDto<String>> createBulk(List<QuotesRequestDto> requestDtos);
  ResponseEntity<ResponseDto<String>> update(QuotesRequestDto requestDto);
  ResponseEntity<ResponseDto<String>> delete(QuotesRequestDto requestDto);
  ResponseEntity<ResponseDto<List<Quotes>>> findByCustomer(String customer);
  ResponseEntity<ResponseDto<List<Quotes>>> getAllQuotes(int page, int rec);
  ResponseEntity<ResponseDto<String>> updateQuoteStatus(QuoteStatusUpdateDto statusUpdateDto);
}
