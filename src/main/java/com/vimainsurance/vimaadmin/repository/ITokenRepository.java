package com.vimainsurance.vimaadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ZohoToken;
public interface  ITokenRepository extends JpaRepository<ZohoToken, Long> {

}