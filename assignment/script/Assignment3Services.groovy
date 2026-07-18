import org.moqui.entity.EntityCondition

Map getA3Q23CompletedSalesOrders() {

    def orderFind = ec.entity.find("assignment.CompletedSalesOrdersPhysicalView")

    orderFind.condition("isPhysical", "Y")
    orderFind.condition("orderTypeId", "SALES_ORDER")
    orderFind.condition("itemStatusId", EntityCondition.NOT_EQUAL, "ITEM_CANCELLED")
    orderFind.orderBy("orderId")

    List orderList = orderFind.list()

    return [orderList: orderList]
}
