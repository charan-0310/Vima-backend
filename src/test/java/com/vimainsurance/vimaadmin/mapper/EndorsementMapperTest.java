package com.vimainsurance.vimaadmin.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.ProductType;

class EndorsementMapperTest {

    @Test
    void mapToResponseDto_includesSplitMetadata() {
        Endorsement parent = new Endorsement();
        parent.setEndorsementId(UUID.randomUUID());

        Policy policy = new Policy();
        policy.setPolicyId(101L);
        policy.setPolicyNumber("POL-101");
        policy.setProductType(ProductType.GMC);

        Endorsement endorsement = new Endorsement();
        endorsement.setEndorsementId(UUID.randomUUID());
        endorsement.setPolicy(policy);
        endorsement.setSplitGroupId(UUID.randomUUID());
        endorsement.setParentEndorsement(parent);

        EndorsementResponseDto dto = EndorsementMapper.mapToResponseDto(endorsement);

        assertEquals(101L, dto.getPolicyId());
        assertEquals("GMC", dto.getPolicyType());
        assertEquals("POL-101", dto.getPolicyNumber());
        assertEquals(endorsement.getSplitGroupId(), dto.getSplitGroupId());
        assertEquals(parent.getEndorsementId(), dto.getParentEndorsementId());
    }
}
