package com.something;

/**
 * Dummy comment.
 */
public class PartnerAccount {
    private PartnerAccountData partnerAccountData;

    public PartnerAccount() {
        // intentionally empty
    }

    public PartnerAccount(final PartnerAccountData partnerAccountData) {
        this.partnerAccountData = partnerAccountData;
    }

    public PartnerAccountData getPartnerAccountData() {
        return partnerAccountData;
    }

    public void setPartnerAccountData(final PartnerAccountData partnerAccountData) {
        this.partnerAccountData = partnerAccountData;
    }
}
