import org.moqui.entity.EntityCondition

Map getshippedorders() {
    def ecf = ec.entity.conditionFactory

    def orderDateRange = ecf.makeCondition([
            ecf.makeCondition("orderDate", EntityCondition.GREATER_THAN_EQUAL_TO, fromDate),
            ecf.makeCondition("orderDate", EntityCondition.LESS_THAN, thruDate)
    ], EntityCondition.AND)

    def statusDateRange = ecf.makeCondition([
            ecf.makeCondition("statusDatetime", EntityCondition.GREATER_THAN_EQUAL_TO, fromDate),
            ecf.makeCondition("statusDatetime", EntityCondition.LESS_THAN, thruDate)
    ], EntityCondition.AND)

    def dateOr = ecf.makeCondition([orderDateRange, statusDateRange], EntityCondition.OR)

    def shippedordersList = ec.entity.find("assignment.shippedorders")
            .condition("roleTypeId", "SHIP_TO_CUSTOMER")
            .condition("contactMechPurposeTypeId", "SHIPPING_LOCATION")
            .condition("statusId", "ORDER_COMPLETED")
            .condition(dateOr)
            .list()

    return [shippedordersList: shippedordersList]
}