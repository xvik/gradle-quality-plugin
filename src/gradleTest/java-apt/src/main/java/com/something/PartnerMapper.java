package com.something;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


/**
 * Dummy comment.
 */
@Mapper
public abstract class PartnerMapper {

    @Mapping(target = "dummyAccount", source = "partnerAccount.partnerAccountData.dummyAccount")
    public abstract Partner toPartner(PartnerAccount partnerAccount);
}
