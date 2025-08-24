package com.something;

/**
 * Dummy comment.
 */
public class PartnerAccountData {
    private boolean dummyAccount;

    public PartnerAccountData() {
        // intentionally empty
    }

    public PartnerAccountData(final boolean dummyAccount) {
        this.dummyAccount = dummyAccount;
    }

    public boolean isDummyAccount() {
        return dummyAccount;
    }

    public void setDummyAccount(final boolean dummyAccount) {
        this.dummyAccount = dummyAccount;
    }
}
