package org.apache.ofbiz.relationshipmgr

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.base.util.UtilDateTime

def createDmPerson() {
    Map parameters = UtilHttp.getParameterMap(request)
    String partyId = parameters.partyId
    
    if (!partyId) {
        partyId = delegator.getNextSeqId("DmParty")
    }
    
    try {
        // Create DmParty if it doesn't exist
        GenericValue dmParty = delegator.findOne("DmParty", [partyId: partyId], false)
        if (!dmParty) {
            dmParty = delegator.makeValue("DmParty", [partyId: partyId, partyTypeId: "PERSON"])
            dmParty.create()
        }
        
        // Create DmPerson
        GenericValue dmPerson = delegator.makeValue("DmPerson", [partyId: partyId])
        dmPerson.setNonPKFields(parameters)
        dmPerson.create()
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createDmPartyRole() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    
    String partyId = parameters.partyId
    String roleTypeId = parameters.roleTypeId
    
    if (!partyId || !roleTypeId) {
        request.setAttribute("_ERROR_MESSAGE_", "Missing partyId or roleTypeId")
        return "error"
    }

    try {
        // Check if role already exists
        GenericValue existingRole = delegator.findOne("DmPartyRole", [partyId: partyId, roleTypeId: roleTypeId], false)
        if (existingRole) {
            org.apache.ofbiz.base.util.Debug.logInfo("Role ${roleTypeId} already exists for party ${partyId}, skipping creation", "DmEvents")
            return "success"
        }

        org.apache.ofbiz.base.util.Debug.logInfo("Creating Role with parameters: " + parameters, "DmEvents")
        dispatcher.runSync("createDmPartyRole", parameters + [userLogin: userLogin])
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def deleteDmPartyRole() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        dispatcher.runSync("deleteDmPartyRole", parameters + [userLogin: userLogin])
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createDmContactMechEmail() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create DmContactMech
        Map contactMechResult = dispatcher.runSync("createDmContactMech", [contactMechTypeId: "EMAIL_ADDRESS", infoString: parameters.infoString, userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Link to Party
        dispatcher.runSync("createDmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createDmContactMechPhone() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create DmContactMech
        Map contactMechResult = dispatcher.runSync("createDmContactMech", [contactMechTypeId: parameters.contactMechTypeId, infoString: parameters.infoString, userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Link to Party
        dispatcher.runSync("createDmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createDmPostalAddress() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create DmContactMech
        Map contactMechResult = dispatcher.runSync("createDmContactMech", [contactMechTypeId: "POSTAL_ADDRESS", userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Create DmPostalAddress
        dispatcher.runSync("createDmPostalAddress", [contactMechId: contactMechId, address1: parameters.address1, city: parameters.city, postalCode: parameters.postalCode, userLogin: userLogin])
        
        // Link to Party
        dispatcher.runSync("createDmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}
