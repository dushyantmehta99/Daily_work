// Groovy service script for Party component
// ec (ExecutionContext) and all in-parameters are available as direct variables

// --- Validate required parameters ---
if (!partyId) {
    ec.message.addError("partyId is required")
    return
}
if (!firstName) {
    ec.message.addError("firstName is required")
    return
}
if (!lastName) {
    ec.message.addError("lastName is required")
    return
}

// --- Verify that the Party record exists ---
def party = ec.entity.find("party.Party").condition("partyId", partyId).one()
if (!party) {
    ec.message.addError("Party with ID '${partyId}' does not exist. Please create the Party record first.")
    return
}

// --- Create the Person record ---
def personValue = ec.entity.makeValue("party.Person")
personValue.partyId   = partyId
personValue.firstName = firstName
personValue.lastName  = lastName
if (dateOfBirth) personValue.dateOfBirth = dateOfBirth
personValue.create()

// --- Return success message ---
result = "Person ${firstName} ${lastName} created successfully!"
