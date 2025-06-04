package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Quotes;
import com.vimainsurance.vimaadmin.service.IQuoteService;

public class QuotesServiceImpl implements IQuoteService{

    @Override
    public ResponseEntity<ResponseDto<String>> create(QuotesRequestDto requestDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public ResponseEntity<ResponseDto<String>> update(QuotesRequestDto requestDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public ResponseEntity<ResponseDto<String>> delete(QuotesRequestDto requestDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public ResponseEntity<ResponseDto<List<Quotes>>> findByCustomer(String customer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByCustomer'");
    }

    @Override
    public ResponseEntity<ResponseDto<List<Quotes>>> getAllQuotes(int page, int rec) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllQuotes'");
    }

}
