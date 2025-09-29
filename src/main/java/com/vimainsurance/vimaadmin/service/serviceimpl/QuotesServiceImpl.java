package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.QuoteStatusUpdateDto;
import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.entity.Quotes;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.IQuoteRepository;
import com.vimainsurance.vimaadmin.service.IQuoteService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.QuoteStatusConstants;
import com.vimainsurance.vimaadmin.util.IdGenerator;


@Service
public class QuotesServiceImpl implements IQuoteService{

    private static final Logger logger = LoggerFactory.getLogger(QuotesServiceImpl.class);

    @Autowired
    private IQuoteRepository quoteRepository;

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public ResponseEntity<ResponseDto<String>>  create(QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            String quoteId = idGenerator.generateQuoteId();
            Optional<Quotes> existByQuoteId = quoteRepository.findByQuoteId(quoteId);
            if(existByQuoteId.isPresent()){
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Already Existed"));
            }
            Optional<Customer> customer = customerRepository.findByCustId(requestDto.getCustomer());
            if (customer.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Customer not found"));
            }
            Quotes quote = new Quotes();
            quote.setQuoteId(requestDto.getQuoteId());
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
    @Transactional
    public ResponseEntity<ResponseDto<String>> createBulk(List<QuotesRequestDto> requestDtos) {
        logger.info("[correlationId:{}] createBulk called with {} quotes", MDC.get("correlationId"), requestDtos.size());
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            int successCount = 0;
            int errorCount = 0;
            StringBuilder errorMessages = new StringBuilder();
            
            for (QuotesRequestDto requestDto : requestDtos) {
                try {
                    Optional<Quotes> existByQuoteId = quoteRepository.findByQuoteId(requestDto.getQuoteId());
                    if(existByQuoteId.isPresent()){
                        errorCount++;
                        errorMessages.append("Quote ID ").append(requestDto.getQuoteId()).append(" already exists. ");
                        continue;
                    }
                    
                    Optional<Customer> customer = customerRepository.findByCustId(requestDto.getCustomer());
                    if (customer.isEmpty()) {
                        errorCount++;
                        errorMessages.append("Customer ").append(requestDto.getCustomer()).append(" not found for quote ").append(requestDto.getQuoteId()).append(". ");
                        continue;
                    }
                    
                    Quotes quote = new Quotes();
                    quote.setQuoteId(idGenerator.generateQuoteId());
                    quote.setCoverageAmount(requestDto.getCoverageAmount());
                    quote.setBestPremium(requestDto.getBestPremium());
                    quote.setStatus(requestDto.getStatus());
                    quote.setCreatedDate(requestDto.getCreatedDate());
                    quote.setCustomer(customer.get());
                    quote.setCompanies(requestDto.getCompanies());

                    quoteRepository.save(quote);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.append("Error processing quote ").append(requestDto.getQuoteId()).append(": ").append(e.getMessage()).append(". ");
                    logger.error("Error processing quote {}: {}", requestDto.getQuoteId(), e.getMessage());
                }
            }
            
            String message = String.format("Bulk operation completed. Success: %d, Errors: %d", successCount, errorCount);
            if (errorCount > 0) {
                message += ". Errors: " + errorMessages.toString();
            }
            
            if (successCount > 0) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, message));
            } else {
                return responseObj.render(responseObj.formErrorResponse(message));
            }
        } catch (Exception e) {
            logger.error("Exception in createBulk", e);
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

    @Override
    public ResponseEntity<ResponseDto<String>> updateQuoteStatus(QuoteStatusUpdateDto statusUpdateDto) {
        logger.info("[correlationId:{}] updateQuoteStatus called for quoteId: {} to status: {}", 
                MDC.get("correlationId"), statusUpdateDto.getQuoteId(), statusUpdateDto.getStatus());
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Find the quote by quoteId (convert string to UUID)
            Optional<Quotes> optionalQuote = quoteRepository.findById(UUID.fromString(statusUpdateDto.getQuoteId()));
            if (optionalQuote.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Quote not found with ID: " + statusUpdateDto.getQuoteId()));
            }
            
            Quotes quote = optionalQuote.get();
            String currentStatus = quote.getStatus();
            String newStatus = QuoteStatusConstants.normalizeStatus(statusUpdateDto.getStatus());
            
            // Validate status transition
            // if (!QuoteStatusConstants.isValidTransition(currentStatus, newStatus)) {
            //     String errorMessage = String.format("Invalid status transition from %s to %s", 
            //             currentStatus != null ? currentStatus : "null", 
            //             newStatus);
            //     logger.warn("[correlationId:{}] {}", MDC.get("correlationId"), errorMessage);
            //     return responseObj.render(responseObj.formErrorResponse(errorMessage));
            // }
            
            // Update the status
            quote.setStatus(newStatus);
            quoteRepository.save(quote);
            
            String successMessage = String.format("Quote status updated successfully from %s to %s", 
                    currentStatus != null ? currentStatus : "null", 
                    newStatus);
            
            logger.info("[correlationId:{}] {}", MDC.get("correlationId"), successMessage);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, successMessage));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in updateQuoteStatus", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to update quote status"));
        }
    }


}
