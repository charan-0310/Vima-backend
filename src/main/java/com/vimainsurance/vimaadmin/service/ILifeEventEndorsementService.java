package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.Endorsement;

/**
 * Handles life-event endorsement flow: when an endorsement has lifeEventType set and
 * adds/removes dependents, recalculates premium with current cost-sharing rules and
 * builds a new cost-sharing snapshot (e.g. on linked submission or endorsement).
 */
public interface ILifeEventEndorsementService {

    /**
     * Called after endorsement approval when the endorsement has a life event type.
     * Recalculates premium for affected employees and persists cost-sharing snapshot.
     *
     * @param endorsement the approved endorsement (with lifeEventType set)
     */
    void onLifeEventEndorsementApproved(Endorsement endorsement);

    /**
     * Called after endorsement approval when the endorsement has a life event type.
     *
     * @param endorsementId the approved endorsement id
     */
    void onLifeEventEndorsementApproved(UUID endorsementId);
}
