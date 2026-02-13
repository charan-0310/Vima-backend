package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.entity.Person;
import com.vimainsurance.vimaadmin.dto.PersonResponseDto;
import com.vimainsurance.vimaadmin.dto.PersonRequestDto;

import java.util.List;
import java.util.stream.Collectors;

public class PersonMapper {

    public static PersonResponseDto mapToResponseDto(Person person) {
        return PersonResponseDto.builder()
            .id(person.getId())
            .name(person.getName())
            .age(person.getAge())
            .city(person.getCity())
            .build();
    }
    public static List<PersonResponseDto> mapToResponseDtoList(List<Person> persons) {
        return persons.stream()
            .map(PersonMapper::mapToResponseDto)
            .collect(Collectors.toList());
    }

    public static Person mapToEntity(PersonRequestDto personRequestDto) {
        return Person.builder()
            .name(personRequestDto.getName())
            .age(personRequestDto.getAge())
            .city(personRequestDto.getCity())
            .build();
    }

}
