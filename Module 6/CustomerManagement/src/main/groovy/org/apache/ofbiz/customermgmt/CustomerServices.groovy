package org.apache.ofbiz.customermgmt

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

import java.sql.Timestamp

/**
 * Find customers matching the given criteria.
 * Filters use partial, case-insensitive matching (LIKE %value%).
 * Phone and address are looked up per-party after the initial view query.
 */
Map findCustomer() {
    List conditions = [
        EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "EmailPrimary")
    ]

    if (parameters.partyId) {
        conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.LIKE, "%${parameters.partyId}%"))
    }
    if (parameters.firstName) {
        conditions.add(EntityCondition.makeCondition("firstName", EntityOperator.LIKE, "%${parameters.firstName}%"))
    }
    if (parameters.lastName) {
        conditions.add(EntityCondition.makeCondition("lastName", EntityOperator.LIKE, "%${parameters.lastName}%"))
    }
    if (parameters.emailAddress) {
        conditions.add(EntityCondition.makeCondition("emailAddress", EntityOperator.LIKE, "%${parameters.emailAddress}%"))
    }

    def viewRows = from("FindCustomerView")
            .where(EntityCondition.makeCondition(conditions, EntityOperator.AND))
            .queryList()

    List customerList = []
    viewRows.each { row ->
        Map customer = [
            partyId      : row.partyId,
            firstName    : row.firstName,
            lastName     : row.lastName,
            emailAddress : row.emailAddress
        ]

        // Lookup phone and address for this party
        def pcmList = from("PartyContactMech")
                .where("partyId", row.partyId)
                .filterByDate()
                .queryList()

        pcmList.each { pcm ->
            def tn = from("TelecomNumber").where("contactMechId", pcm.contactMechId).queryOne()
            if (tn && !customer.contactNumber) customer.contactNumber = tn.contactNumber

            def pa = from("PostalAddress").where("contactMechId", pcm.contactMechId).queryOne()
            if (pa && !customer.address1) {
                customer.address1        = pa.address1
                customer.city            = pa.city
                customer.zipOrPostalCode = pa.zipOrPostalCode
            }
        }

        // Apply phone/address filters post-lookup (partial, case-insensitive)
        boolean include = true
        if (parameters.contactNumber && !customer.contactNumber?.contains(parameters.contactNumber)) include = false
        if (parameters.address1 && !customer.address1?.toLowerCase()?.contains(parameters.address1.toLowerCase())) include = false

        if (include) customerList.add(customer)
    }

    return [customerList: customerList]
}

/**
 * Create a new customer.
 * Requires email (unique identifier), firstName, lastName.
 * Optionally creates phone and postal address contact mechs.
 */
Map createCustomer() {
    String email = parameters.emailAddress

    // Duplicate check by primary email
    def existing = from("FindCustomerView")
            .where(EntityCondition.makeCondition([
                EntityCondition.makeCondition("emailAddress", EntityOperator.EQUALS, email),
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "EmailPrimary")
            ], EntityOperator.AND))
            .queryFirst()
    if (existing) {
        return [errorMessageList: ["Customer with email '${email}' already exists (partyId: ${existing.partyId})"]]
    }

    Timestamp now = new Timestamp(System.currentTimeMillis())

    // Party
    String partyId = delegator.getNextSeqId("Party")
    delegator.create("Party", [partyId: partyId, partyTypeId: "PERSON"])

    // Person
    delegator.create("Person", [partyId: partyId, firstName: parameters.firstName, lastName: parameters.lastName])

    // Email ContactMech
    String cmId = delegator.getNextSeqId("ContactMech")
    delegator.create("ContactMech", [contactMechId: cmId, contactMechTypeId: "EMAIL_ADDRESS", infoString: email])
    delegator.create("PartyContactMech", [partyId: partyId, contactMechId: cmId, fromDate: now, allowSolicitation: "Y"])
    delegator.create("PartyContactMechPurpose", [
        partyId                  : partyId,
        contactMechId            : cmId,
        contactMechPurposeTypeId : "EmailPrimary",
        fromDate                 : now
    ])

    // Optional phone
    if (parameters.contactNumber) {
        String phoneCmId = delegator.getNextSeqId("ContactMech")
        delegator.create("ContactMech",    [contactMechId: phoneCmId, contactMechTypeId: "TELECOM_NUMBER"])
        delegator.create("TelecomNumber",  [contactMechId: phoneCmId, contactNumber: parameters.contactNumber])
        delegator.create("PartyContactMech", [partyId: partyId, contactMechId: phoneCmId, fromDate: now])
    }

    // Optional postal address
    if (parameters.address1) {
        String addrCmId = delegator.getNextSeqId("ContactMech")
        delegator.create("ContactMech",  [contactMechId: addrCmId, contactMechTypeId: "POSTAL_ADDRESS"])
        delegator.create("PostalAddress", [
            contactMechId    : addrCmId,
            address1         : parameters.address1        ?: "",
            city             : parameters.city            ?: "",
            zipOrPostalCode  : parameters.zipOrPostalCode ?: ""
        ])
        delegator.create("PartyContactMech", [partyId: partyId, contactMechId: addrCmId, fromDate: now])
    }

    return [partyId: partyId]
}

/**
 * Update a customer's phone and/or postal address, identified by email.
 */
Map updateCustomer() {
    String email = parameters.emailAddress
    Timestamp now = new Timestamp(System.currentTimeMillis())

    def existing = from("FindCustomerView")
            .where(EntityCondition.makeCondition([
                EntityCondition.makeCondition("emailAddress", EntityOperator.EQUALS, email),
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "EmailPrimary")
            ], EntityOperator.AND))
            .queryFirst()
    if (!existing) {
        return [errorMessageList: ["No customer found with email: ${email}"]]
    }
    String partyId = existing.partyId

    def pcmList = from("PartyContactMech").where("partyId", partyId).filterByDate().queryList()

    if (parameters.contactNumber != null) {
        def existingTn = null
        pcmList.each { pcm ->
            def tn = from("TelecomNumber").where("contactMechId", pcm.contactMechId).queryOne()
            if (tn && !existingTn) existingTn = tn
        }
        if (existingTn) {
            existingTn.set("contactNumber", parameters.contactNumber)
            existingTn.store()
        } else {
            String phoneCmId = delegator.getNextSeqId("ContactMech")
            delegator.create("ContactMech",   [contactMechId: phoneCmId, contactMechTypeId: "TELECOM_NUMBER"])
            delegator.create("TelecomNumber", [contactMechId: phoneCmId, contactNumber: parameters.contactNumber])
            delegator.create("PartyContactMech", [partyId: partyId, contactMechId: phoneCmId, fromDate: now])
        }
    }

    if (parameters.address1 != null) {
        def existingPa = null
        pcmList.each { pcm ->
            def pa = from("PostalAddress").where("contactMechId", pcm.contactMechId).queryOne()
            if (pa && !existingPa) existingPa = pa
        }
        if (existingPa) {
            if (parameters.address1)        existingPa.set("address1",        parameters.address1)
            if (parameters.city)            existingPa.set("city",            parameters.city)
            if (parameters.zipOrPostalCode) existingPa.set("zipOrPostalCode", parameters.zipOrPostalCode)
            existingPa.store()
        } else {
            String addrCmId = delegator.getNextSeqId("ContactMech")
            delegator.create("ContactMech",  [contactMechId: addrCmId, contactMechTypeId: "POSTAL_ADDRESS"])
            delegator.create("PostalAddress", [
                contactMechId    : addrCmId,
                address1         : parameters.address1        ?: "",
                city             : parameters.city            ?: "",
                zipOrPostalCode  : parameters.zipOrPostalCode ?: ""
            ])
            delegator.create("PartyContactMech", [partyId: partyId, contactMechId: addrCmId, fromDate: now])
        }
    }

    return [:]
}

/**
 * Create a PartyRelationship between two parties.
 */
Map createCustomerRelationship() {
    String partyIdFrom = parameters.partyIdFrom
    String partyIdTo   = parameters.partyIdTo
    String relType     = parameters.partyRelationshipTypeId

    if (!from("Party").where("partyId", partyIdFrom).queryOne()) {
        return [errorMessageList: ["Party not found: ${partyIdFrom}"]]
    }
    if (!from("Party").where("partyId", partyIdTo).queryOne()) {
        return [errorMessageList: ["Party not found: ${partyIdTo}"]]
    }

    delegator.create("PartyRelationship", [
        partyIdFrom             : partyIdFrom,
        partyIdTo               : partyIdTo,
        roleTypeIdFrom          : "CUSTOMER",
        roleTypeIdTo            : "CONTACT",
        partyRelationshipTypeId : relType,
        fromDate                : new Timestamp(System.currentTimeMillis()),
        statusId                : "PARTY_REL_ACTIVE"
    ])

    return [:]
}

/**
 * Update the status of an existing active party relationship.
 */
Map updateCustomerRelationship() {
    String partyIdFrom = parameters.partyIdFrom
    String partyIdTo   = parameters.partyIdTo
    String relType     = parameters.partyRelationshipTypeId

    def relList = from("PartyRelationship")
            .where("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "partyRelationshipTypeId", relType)
            .filterByDate()
            .queryList()

    if (!relList) {
        return [errorMessageList: ["No active relationship found between '${partyIdFrom}' and '${partyIdTo}' of type '${relType}'"]]
    }

    def rel = relList[0]
    if (parameters.statusId) rel.set("statusId", parameters.statusId)
    rel.store()

    return [:]
}
