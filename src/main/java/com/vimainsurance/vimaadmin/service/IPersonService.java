package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.PersonRequestDto;
import com.vimainsurance.vimaadmin.dto.PersonResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

import org.springframework.http.ResponseEntity;

public interface IPersonService {

    ResponseEntity<ResponseDto<List<PersonResponseDto>>> getAllPersons();
    ResponseEntity<ResponseDto<PersonResponseDto>> getPersonById(UUID id);
    ResponseEntity<ResponseDto<PersonResponseDto>> createPerson(PersonRequestDto personRequestDto);
    ResponseEntity<ResponseDto<PersonResponseDto>> updatePerson(UUID id, PersonRequestDto personRequestDto);
    ResponseEntity<ResponseDto<String>> deletePerson(UUID id);
    ResponseEntity<ResponseDto<List<PersonResponseDto>>> getPersonsByCity(String city);
}
