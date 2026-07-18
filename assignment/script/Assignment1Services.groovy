import org.moqui.entity.EntityCondition
import java.sql.Timestamp

Map getA1Q01NewCustomersUsingFind() {

    Timestamp fromDate = Timestamp.valueOf("2026-06-01 00:00:00")
    Timestamp thruDate = Timestamp.valueOf("2026-07-01 00:00:00")

    List partyList = ec.entity.find("Party")
            .condition("createdStamp", EntityCondition.GREATER_THAN_EQUAL_TO, fromDate)
            .condition("createdStamp", EntityCondition.LESS_THAN, thruDate)
            .list()

    List customerList = []

    for (party in partyList) {

        def role = ec.entity.find("PartyRole")
                .condition("partyId", party.partyId)
                .condition("roleTypeId", "CUSTOMER")
                .one()

        if (!role) continue

        def person = ec.entity.find("Person")
                .condition("partyId", party.partyId)
                .one()

        String email = null
        String phone = null

        List pcmList = ec.entity.find("PartyContactMech")
                .condition("partyId", party.partyId)
                .list()

        for (pcm in pcmList) {

            def contactMech = ec.entity.find("ContactMech")
                    .condition("contactMechId", pcm.contactMechId)
                    .one()

            if (!contactMech) continue

            // If TelecomNumber exists then it is phone
            def telecom = ec.entity.find("TelecomNumber")
                    .condition("contactMechId", pcm.contactMechId)
                    .one()

            if (telecom) {
                phone = telecom.contactNumber
            } else {
                // Otherwise assume email
                email = contactMech.infoString
            }
        }

        customerList.add([
                partyId      : party.partyId,
                firstName    : person?.firstName,
                lastName     : person?.lastName,
                email        : email,
                phone        : phone,
                createdStamp : party.createdStamp
        ])
    }

    return [customerList: customerList]
}

Map getA1Q01NewCustomers(){
 Timestamp fromDate = Timestamp.valueOf("2026-06-01 00:00:00")
 Timestamp thruDate = Timestamp.valueOf("2026-07-01 00:00:00")

 def customerFind = ec.entity.find("assignment.CustomerSignupView")
     customerFind.condition("roleTypeId","CUSTOMER")
     customerFind.condition("createdStamp",EntityCondition.GREATER_THAN_EQUAL_TO,fromDate)
     customerFind.condition("createdStamp",EntityCondition.LESS_THAN,thruDate)

    List customerList = customerFind.list()
    return [customerList : customerList]
}

Map getA1Q05CompletedOrders() {

    Timestamp fromDate = Timestamp.valueOf("2023-08-01 00:00:00")
    Timestamp thruDate = Timestamp.valueOf("2023-09-01 00:00:00")

    def orderFind = ec.entity.find("assignment.CompletedOrdersList")

    orderFind.condition("statusId", "ORDER_COMPLETED")
    orderFind.condition("orderDate", EntityCondition.GREATER_THAN_EQUAL_TO, fromDate)
    orderFind.condition("orderDate", EntityCondition.LESS_THAN, thruDate)

    List orderList = orderFind.list()

    return [orderList: orderList]

}
    Map getOrdersWithoutShipmentView(){
    Timestamp last10Days =
            new Timestamp(System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000))

    def orders = ec.entity.find("assignment.OrdersWithoutShipmentView")
            .condition("statusId", "ORDER_APPROVED")
            .condition("shipmentId", null)
            .condition("orderDate", EntityCondition.GREATER_THAN_EQUAL_TO, last10Days)
            .orderBy("-orderDate")
            .list()
        return [orderList: orderList]
}