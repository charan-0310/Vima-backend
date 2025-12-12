package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeBatchService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Batch insert new Deals records using JdbcTemplate for optimal performance
     * 
     * @param dealsToInsert List of Deals entities to insert (must have individualId as null)
     * @param organization Organization entity
     * @param batchSize Batch size for processing
     * @return Number of records inserted
     */
    @Transactional
    public int batchInsertDeals(List<Deals> dealsToInsert, Organization organization, int batchSize) {
        
        if (dealsToInsert == null || dealsToInsert.isEmpty()) {
            log.warn("No deals provided for batch insert");
            return 0;
        }

        String sql = """
            INSERT INTO cpc.customers (
                individual_id, first_name, last_name, full_name, email, phone,
                date_of_birth, gender, pan_number, aadhaar_number, address, city,
                state, pincode, account_type, status, organization_id, employee_number,
                primary_individual_id, relationship, is_primary_member, designation,
                date_of_joining, username, password_hash, preferred_language, lead_id,
                cust_id, created_at, updated_at, marital_status, sum_insured
            ) VALUES (
                gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::cpc.account_type_enum,
                ?::cpc.account_status_enum, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """;

        int totalInserted = 0;

        for (int i = 0; i < dealsToInsert.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dealsToInsert.size());
            final List<Deals> batch = dealsToInsert.subList(i, end);

            int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    Deals deal = batch.get(index);
                    int paramIndex = 1;

                    // first_name
                    ps.setString(paramIndex++, deal.getFirstName());
                    // last_name
                    ps.setString(paramIndex++, deal.getLastName());
                    // full_name
                    ps.setString(paramIndex++, deal.getFullName());
                    // email
                    ps.setString(paramIndex++, deal.getEmail());
                    // phone
                    ps.setString(paramIndex++, deal.getPhone());
                    // date_of_birth
                    if (deal.getDateOfBirth() != null) {
                        ps.setDate(paramIndex++, java.sql.Date.valueOf(deal.getDateOfBirth()));
                    } else {
                        ps.setNull(paramIndex++, Types.DATE);
                    }
                    // gender
                    ps.setString(paramIndex++, deal.getGender());
                    // pan_number
                    ps.setString(paramIndex++, deal.getPanNumber());
                    // aadhaar_number
                    ps.setString(paramIndex++, deal.getAadhaarNumber());
                    // address
                    ps.setString(paramIndex++, deal.getAddress());
                    // city
                    ps.setString(paramIndex++, deal.getCity());
                    // state
                    ps.setString(paramIndex++, deal.getState());
                    // pincode
                    ps.setString(paramIndex++, deal.getPincode());
                    // account_type
                    ps.setString(paramIndex++, deal.getAccountType() != null ? 
                        deal.getAccountType().getValue() : AccountType.RETAIL_PRIMARY.getValue());
                    // status
                    ps.setString(paramIndex++, deal.getStatus() != null ? 
                        deal.getStatus().getValue() : AccountStatus.ACTIVE.getValue());
                    // organization_id
                    ps.setObject(paramIndex++, organization.getOrganizationId(), Types.OTHER);
                    // employee_number
                    ps.setString(paramIndex++, deal.getEmployeeNumber());
                    // primary_individual_id
                    if (deal.getPrimaryIndividual() != null) {
                        ps.setObject(paramIndex++, deal.getPrimaryIndividual().getIndividualId(), Types.OTHER);
                    } else {
                        ps.setNull(paramIndex++, Types.OTHER);
                    }
                    // relationship
                    ps.setString(paramIndex++, deal.getRelationship());
                    // is_primary_member
                    Boolean isPrimaryMember = deal.getIsPrimaryMember();
                    boolean isPrimary = (isPrimaryMember != null) ? Boolean.TRUE.equals(isPrimaryMember) : false;
                    ps.setBoolean(paramIndex++, isPrimary);
                    // designation
                    ps.setString(paramIndex++, deal.getDesignation());
                    // date_of_joining
                    if (deal.getDateOfJoining() != null) {
                        ps.setDate(paramIndex++, java.sql.Date.valueOf(deal.getDateOfJoining()));
                    } else {
                        ps.setNull(paramIndex++, Types.DATE);
                    }
                    // username
                    ps.setString(paramIndex++, deal.getUsername());
                    // password_hash
                    ps.setString(paramIndex++, deal.getPasswordHash());
                    // preferred_language
                    ps.setString(paramIndex++, deal.getPreferredLanguage() != null ? deal.getPreferredLanguage() : "en");
                    // lead_id
                    if (deal.getLeadId() != null) {
                        ps.setObject(paramIndex++, deal.getLeadId(), Types.OTHER);
                    } else {
                        ps.setNull(paramIndex++, Types.OTHER);
                    }
                    // cust_id
                    ps.setString(paramIndex++, deal.getCustId());
                    // created_at
                    if (deal.getCreatedAt() != null) {
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(deal.getCreatedAt()));
                    } else {
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                    }
                    // updated_at
                    if (deal.getUpdatedAt() != null) {
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(deal.getUpdatedAt()));
                    } else {
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                    }
                    // marital_status
                    ps.setString(paramIndex++, deal.getMaritalStatus());
                    // sum_insured
                    ps.setString(paramIndex++, deal.getSumInsured());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            totalInserted += results.length;
            log.debug("Batch inserted {} new deals (batch {}/{})", 
                batch.size(), (i / batchSize) + 1, (dealsToInsert.size() + batchSize - 1) / batchSize);
        }

        log.info("Successfully batch inserted {} deals using JdbcTemplate", totalInserted);
        return totalInserted;
    }

    /**
     * Batch update existing Deals records using JdbcTemplate
     * 
     * @param dealsToUpdate List of Deals entities to update (must have individualId set)
     * @param organization Organization entity
     * @param batchSize Batch size for processing
     * @return Number of records updated
     */
    @Transactional
    public int batchUpdateDeals(List<Deals> dealsToUpdate, Organization organization, int batchSize) {
        
        if (dealsToUpdate == null || dealsToUpdate.isEmpty()) {
            log.warn("No deals provided for batch update");
            return 0;
        }

        // Validate all deals have individualId
        for (Deals deal : dealsToUpdate) {
            if (deal.getIndividualId() == null) {
                log.warn("Skipping deal without individualId for update");
                throw new IllegalArgumentException("All deals must have individualId set for batch update");
            }
        }

        String sql = """
            UPDATE cpc.customers SET
                first_name = ?, last_name = ?, full_name = ?, email = ?, phone = ?,
                date_of_birth = ?, gender = ?, designation = ?, date_of_joining = ?,
                account_type = ?::cpc.account_type_enum, status = ?::cpc.account_status_enum,
                organization_id = ?, employee_number = ?, relationship = ?,
                is_primary_member = ?, updated_at = ?, marital_status = ?, sum_insured = ?
            WHERE individual_id = ?
            """;

        int totalUpdated = 0;

        for (int i = 0; i < dealsToUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dealsToUpdate.size());
            final List<Deals> batch = dealsToUpdate.subList(i, end);

            int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    Deals deal = batch.get(index);
                    int paramIndex = 1;

                    // first_name
                    ps.setString(paramIndex++, deal.getFirstName());
                    // last_name
                    ps.setString(paramIndex++, deal.getLastName());
                    // full_name
                    ps.setString(paramIndex++, deal.getFullName());
                    // email
                    ps.setString(paramIndex++, deal.getEmail());
                    // phone
                    ps.setString(paramIndex++, deal.getPhone());
                    // date_of_birth
                    if (deal.getDateOfBirth() != null) {
                        ps.setDate(paramIndex++, java.sql.Date.valueOf(deal.getDateOfBirth()));
                    } else {
                        ps.setNull(paramIndex++, Types.DATE);
                    }
                    // gender
                    ps.setString(paramIndex++, deal.getGender());
                    // designation
                    ps.setString(paramIndex++, deal.getDesignation());
                    // date_of_joining
                    if (deal.getDateOfJoining() != null) {
                        ps.setDate(paramIndex++, java.sql.Date.valueOf(deal.getDateOfJoining()));
                    } else {
                        ps.setNull(paramIndex++, Types.DATE);
                    }
                    // account_type
                    ps.setString(paramIndex++, deal.getAccountType() != null ? 
                        deal.getAccountType().getValue() : AccountType.RETAIL_PRIMARY.getValue());
                    // status
                    ps.setString(paramIndex++, deal.getStatus() != null ? 
                        deal.getStatus().getValue() : AccountStatus.ACTIVE.getValue());
                    // organization_id
                    ps.setObject(paramIndex++, organization.getOrganizationId(), Types.OTHER);
                    // employee_number
                    ps.setString(paramIndex++, deal.getEmployeeNumber());
                    // relationship
                    ps.setString(paramIndex++, deal.getRelationship());
                    // is_primary_member
                    Boolean isPrimaryMember = deal.getIsPrimaryMember();
                    boolean isPrimary = (isPrimaryMember != null) ? Boolean.TRUE.equals(isPrimaryMember) : false;
                    ps.setBoolean(paramIndex++, isPrimary);
                    // updated_at
                    ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                    // marital_status
                    ps.setString(paramIndex++, deal.getMaritalStatus());
                    // sum_insured
                    ps.setString(paramIndex++, deal.getSumInsured());
                    // WHERE individual_id
                    ps.setObject(paramIndex++, deal.getIndividualId(), Types.OTHER);

                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            totalUpdated += results.length;
            log.debug("Batch updated {} deals (batch {}/{})", 
                batch.size(), (i / batchSize) + 1, (dealsToUpdate.size() + batchSize - 1) / batchSize);
        }

        log.info("Successfully batch updated {} deals using JdbcTemplate", totalUpdated);
        return totalUpdated;
    }

}
