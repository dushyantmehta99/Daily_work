package org.apache.ofbiz.relationshipmgr

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

def createDmPerson() {
    Map result = ServiceUtil.returnSuccess()
    String partyId = parameters.partyId

    if (!partyId) {
        partyId = delegator.getNextSeqId("DmParty")
    }

    GenericValue dmParty = delegator.findOne("DmParty", [partyId: partyId], false)
    if (!dmParty) {
        dmParty = delegator.makeValue("DmParty", [partyId: partyId, partyTypeId: "PERSON"])
        dmParty.create()
    }

    GenericValue dmPerson = delegator.makeValue("DmPerson", [partyId: partyId])
    dmPerson.setNonPKFields(parameters)
    dmPerson.create()

    result.partyId = partyId
    return result
}
