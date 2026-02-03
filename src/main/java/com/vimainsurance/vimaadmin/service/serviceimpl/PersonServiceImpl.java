package com.vimainsurance.vimaadmin.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.repository.IPersonRepository;
import com.vimainsurance.vimaadmin.service.IPersonService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.entity.Person;
import com.vimainsurance.vimaadmin.mapper.PersonMapper;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PersonResponseDto;
import com.vimainsurance.vimaadmin.dto.PersonRequestDto;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.UUID;

@Service
public class PersonServiceImpl implements IPersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonServiceImpl.class);

    @Autowired
    private IPersonRepository personRepository;

    @Override
    public ResponseEntity<ResponseDto<List<PersonResponseDto>>> getAllPersons() {
        logger.info("[correlationId:{}] getAllPersons called", MDC.get("correlationId"));
        BaseResponse<List<PersonResponseDto>> responseObj = new BaseResponse<>();
        try{
            List<Person> persons = personRepository.findAll();
            List<PersonResponseDto> personResponseDtos = PersonMapper.mapToResponseDtoList(persons);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, personResponseDtos, personResponseDtos.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getAllPersons: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PersonResponseDto>> getPersonById(UUID id) {
        logger.info("[correlationId:{}] getPersonById called with id: {}", MDC.get("correlationId"), id);
        BaseResponse<PersonResponseDto> responseObj = new BaseResponse<>();
        try{
            Person person = personRepository.findById(id).orElseThrow(() -> new RuntimeException("Person not found"));
            PersonResponseDto personResponseDto = PersonMapper.mapToResponseDto(person);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, personResponseDto));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPersonById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PersonResponseDto>> createPerson(PersonRequestDto personRequestDto) {
        logger.info("[correlationId:{}] createPerson called", MDC.get("correlationId"));
        BaseResponse<PersonResponseDto> responseObj = new BaseResponse<>();
        try{
            Person person = PersonMapper.mapToEntity(personRequestDto);
            Person savedPerson = personRepository.save(person);
            PersonResponseDto personResponseDto = PersonMapper.mapToResponseDto(savedPerson);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, personResponseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in createPerson: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PersonResponseDto>> updatePerson(UUID id, PersonRequestDto personRequestDto) {
        logger.info("[correlationId:{}] updatePerson called with id: {}", MDC.get("correlationId"), id);
        BaseResponse<PersonResponseDto> responseObj = new BaseResponse<>();
        try{
            Person person = personRepository.findById(id).orElseThrow(() -> new RuntimeException("Person not found"));
            PersonMapper.mapToEntity(personRequestDto);
            Person savedPerson = personRepository.save(person);
            PersonResponseDto personResponseDto = PersonMapper.mapToResponseDto(savedPerson);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, personResponseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in updatePerson: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deletePerson(UUID id) {
        logger.info("[correlationId:{}] deletePerson called with id: {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try{
            personRepository.deleteById(id);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deletePerson: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PersonResponseDto>>> getPersonsByCity(String city) {
        logger.info("[correlationId:{}] getPersonsByCity called with city: {}", MDC.get("correlationId"), city);
        BaseResponse<List<PersonResponseDto>> responseObj = new BaseResponse<>();
        try{
            List<Person> persons = personRepository.findAllByCity(city);
            List<PersonResponseDto> personResponseDtos = PersonMapper.mapToResponseDtoList(persons);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, personResponseDtos, personResponseDtos.size()));
        }   
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPersonsByCity: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
}
