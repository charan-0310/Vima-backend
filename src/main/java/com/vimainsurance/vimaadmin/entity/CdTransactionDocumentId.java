package com.vimainsurance.vimaadmin.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CdTransactionDocumentId implements Serializable {

    private UUID transactionId;
    private UUID documentId;

    public CdTransactionDocumentId(UUID transactionId, UUID documentId) {
        this.transactionId = transactionId;
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CdTransactionDocumentId other)) return false;
        return Objects.equals(transactionId, other.transactionId)
                && Objects.equals(documentId, other.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, documentId);
    }
}
