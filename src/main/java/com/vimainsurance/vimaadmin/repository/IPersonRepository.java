package com.vimainsurance.vimaadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vimainsurance.vimaadmin.entity.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByName(String name);
    List<Person> findAllByAge(Integer age);

    @Query(value = "SELECT * FROM person WHERE city = :city", nativeQuery = true)
    List<Person> findAllByCity(@Param("city") String city);

}
