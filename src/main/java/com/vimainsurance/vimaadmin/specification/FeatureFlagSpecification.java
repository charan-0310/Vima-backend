package com.vimainsurance.vimaadmin.specification;

import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

public class FeatureFlagSpecification {

    public static Specification<FeatureFlag> fetchRelations() {
        return (root, query, cb) -> {
            // Avoid duplicates when using fetch joins
            root.fetch("roles", JoinType.LEFT);
            root.fetch("companies", JoinType.LEFT);
            query.distinct(true);
            return cb.conjunction();
        };
    }

}

