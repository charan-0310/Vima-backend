package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.service.ILifeEventEndorsementService;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles life-event endorsement flow after approval: when endorsement has lifeEventType set,
 * can recalculate premium with current cost-sharing rules and persist cost-sharing snapshot
 * on linked enrollment submission. Currently logs; full integration with PremiumCalculationService
 * and submission cost_sharing_snapshot update can be added here.
 */
@Service
@Slf4j
public class LifeEventEndorsementServiceImpl implements ILifeEventEndorsementService {

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Override
    @Transactional(readOnly = true)
    public void onLifeEventEndorsementApproved(Endorsement endorsement) {
        if (endorsement == null || endorsement.getLifeEventType() == null || endorsement.getLifeEventType().isBlank()) {
            return;
        }
        log.info("Life event endorsement approved: endorsementId={}, lifeEventType={}; premium/snapshot recalculation can be wired here",
                endorsement.getEndorsementId(), endorsement.getLifeEventType());
        // TODO: load affected employees from endorsement deals; for each, call premium calculation
        // and persist cost_sharing_snapshot on linked EnrollmentSubmission or new snapshot on endorsement
    }

    @Override
    @Transactional(readOnly = true)
    public void onLifeEventEndorsementApproved(UUID endorsementId) {
        if (endorsementId == null) {
            return;
        }
        Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
        opt.ifPresent(this::onLifeEventEndorsementApproved);
    }
}
