package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.entity.Quotes;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.IQuoteRepository;
import com.vimainsurance.vimaadmin.service.IQuoteService;
import com.vimainsurance.vimaadmin.util.Constants;

public class QuotesServiceImpl implements IQuoteService{

    private static final Logger logger = LoggerFactory.getLogger(QuotesServiceImpl.class);

    @Autowired
    private IQuoteRepository quoteRepository;

    @Autowired
    private ICustomerRepository customerRepository;

    @Override
    public ResponseEntity<ResponseDto<String>> create(QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            String quoteId = generateQuoteId();
            Optional<Quotes> existByQuoteId = quoteRepository.findByQuoteId(quoteId);
            if(existByQuoteId.isPresent()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Already Existed"));
            }
            Optional<Customer> customer = customerRepository.findByCustId(requestDto.getCustomer());
            Quotes quote = new Quotes();
            quote.setQuoteId(generateQuoteId());
            quote.setCoverageAmount(requestDto.getCoverageAmount());
            quote.setBestPremium(requestDto.getBestPremium());
            quote.setStatus(requestDto.getStatus());
            quote.setCreatedDate(requestDto.getCreatedDate());
            quote.setCustomer(customer.get());

            quoteRepository.save(quote);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (Exception e) {
            logger.error("Exception in create", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }        
    }

    @Override
    public ResponseEntity<ResponseDto<String>> update(QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Quotes> existQuote = quoteRepository.findByQuoteId(requestDto.getQuoteId());
            if(existQuote.isPresent()){
                Quotes quote = existQuote.get();
                Optional<Customer> customer = customerRepository.findByCustId(requestDto.getCustomer());
                quote.setCoverageAmount(requestDto.getCoverageAmount());
                quote.setBestPremium(requestDto.getBestPremium());
                quote.setStatus(requestDto.getStatus());
                quote.setCreatedDate(requestDto.getCreatedDate());
                quote.setCustomer(customer.get());
                quoteRepository.save(quote);
            } else {
                return responseObj.render(responseObj.formErrorResponse(Constants.UPDATE_FAILED));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("Exception in update", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> delete(QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] delete called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Quotes> existQuote = quoteRepository.findByQuoteId(requestDto.getQuoteId());
            if(existQuote.isPresent()){
                Quotes quote = existQuote.get();
                quote.setStatus("INACTIVE");
                quoteRepository.save(quote);
            }
            else{
                return responseObj.render(responseObj.formErrorResponse(Constants.DELETE_FAILED));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (Exception e) {
            logger.error("Exception in delete", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<Quotes>>> findByCustomer(String customer) {
        logger.info("[correlationId:{}] findByCustomer called", MDC.get("correlationId"));
        BaseResponse<List<Quotes>> responseObj = new BaseResponse<>();
        try {
            Optional<Customer> optionalCustomer = customerRepository.findByCustId(customer);
            if(optionalCustomer.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            List<Quotes> quotes = optionalCustomer.get().getQuotes();
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, quotes, quotes.size()));
        } catch (Exception e) {
            logger.error("Exception in findByCustomer", e);
            throw new UnsupportedOperationException("Unimplemented method 'findByCustomer'");
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<Quotes>>> getAllQuotes(int page, int rec) {
        logger.info("[correlationId:{}] getAllQuotes called", MDC.get("correlationId"));
        BaseResponse<List<Quotes>> responseObj = new BaseResponse<>();
        try {
            if (page == -1 && rec == -1) {
                List<Quotes> customerList = quoteRepository.findAll();
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, customerList, customerList.size()));
            } else {
                 Pageable pageable = PageRequest.of(page, rec);
                 Page<Quotes> customerPage = quoteRepository.findAll(pageable);
                 return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, customerPage.getContent(), customerPage.getTotalPages()));
            }
        } catch (Exception e) {
            logger.error("Exception in getAllQuotes", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    public String generateQuoteId() {
        String maxId = quoteRepository.findMaxQuoteId(); 
        if (maxId == null) {
            return "Q001";
        }
        int num = Integer.parseInt(maxId.substring(1));
        num++;
        return String.format("Q%03d", num);
    }


}
