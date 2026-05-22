package org.apache.ofbiz.relationshipmgr

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

partyId = parameters.partyId
if (partyId) {
    context.partyRoles = from("DmPartyRole").where("partyId", partyId).queryList()
    
    partyContactMechs = from("DmPartyContactMech").where("partyId", partyId).queryList()
    contactMechIds = partyContactMechs*.contactMechId
    
    if (contactMechIds) {
        context.contactMechs = from("DmContactMech").where(EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds)).queryList()
    } else {
        context.contactMechs = []
    }
}
