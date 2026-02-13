package com.vimainsurance.vimaadmin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.service.IPersonService;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.PersonResponseDto;
import com.vimainsurance.vimaadmin.dto.PersonRequestDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    @Autowired
    private IPersonService personService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<PersonResponseDto>>> getAllPersons() {
        return personService.getAllPersons();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<PersonResponseDto>> getPersonById(@PathVariable UUID id) {
        return personService.getPersonById(id);
    }
    @PostMapping
    public ResponseEntity<ResponseDto<PersonResponseDto>> createPerson(@RequestBody PersonRequestDto personRequestDto) {
        return personService.createPerson(personRequestDto);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<PersonResponseDto>> updatePerson(@PathVariable UUID id, @RequestBody PersonRequestDto personRequestDto) {
        return personService.updatePerson(id, personRequestDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<String>> deletePerson(@PathVariable UUID id) {
        return personService.deletePerson(id);
    }
    @GetMapping("/city/{city}")
    public ResponseEntity<ResponseDto<List<PersonResponseDto>>> getPersonsByCity(@PathVariable String city) {
        return personService.getPersonsByCity(city);
    }
}
