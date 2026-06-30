package org.apache.ofbiz.productmgmt

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate

Map findProduct() {
    Map result = success()
    List conditions = []

    String productId = parameters.productId
    String productName = parameters.productName
    BigDecimal minPrice = parameters.minPrice
    BigDecimal maxPrice = parameters.maxPrice
    String productFeatureTypeId = parameters.productFeatureTypeId
    String featureDescription = parameters.featureDescription

    if (UtilValidate.isNotEmpty(productId)) {
        conditions << EntityCondition.makeCondition('productId', EntityOperator.LIKE, '%' + productId + '%')
    }
    if (UtilValidate.isNotEmpty(productName)) {
        conditions << EntityCondition.makeCondition(
            EntityCondition.makeCondition('productName', EntityOperator.LIKE, '%' + productName.toLowerCase() + '%')
        )
    }
    if (minPrice != null) {
        conditions << EntityCondition.makeCondition('price', EntityOperator.GREATER_THAN_EQUAL_TO, minPrice)
    }
    if (maxPrice != null) {
        conditions << EntityCondition.makeCondition('price', EntityOperator.LESS_THAN_EQUAL_TO, maxPrice)
    }
    if (UtilValidate.isNotEmpty(productFeatureTypeId)) {
        conditions << EntityCondition.makeCondition('productFeatureTypeId', EntityOperator.EQUALS, productFeatureTypeId)
    }
    if (UtilValidate.isNotEmpty(featureDescription)) {
        conditions << EntityCondition.makeCondition('featureDescription', EntityOperator.LIKE, '%' + featureDescription + '%')
    }
    conditions << EntityCondition.makeCondition('productPriceTypeId', EntityOperator.EQUALS, 'LIST_PRICE')

    EntityCondition condition = conditions.size() > 1
        ? EntityCondition.makeCondition(conditions, EntityOperator.AND)
        : (conditions.size() == 1 ? conditions[0] : null)

    def query = from('FindProductView')
        .select('productId', 'productName', 'price', 'currencyUomId',
                'productFeatureId', 'featureDescription', 'productFeatureTypeId',
                'productCategoryId')
        .orderBy('productId')

    if (condition) query = query.where(condition)

    result.productList = query.queryList()
    return result
}

Map createProduct() {
    Map result = success()

    String productName = parameters.productName
    String productCategoryId = parameters.productCategoryId
    BigDecimal price = parameters.price
    String currencyUomId = parameters.currencyUomId ?: 'USD'
    String productTypeId = parameters.productTypeId ?: 'FINISHED_GOOD'

    List existing = from('Product')
        .where(EntityCondition.makeCondition('internalName', EntityOperator.EQUALS, productName))
        .queryList()

    if (existing) {
        return error("Product with name '${productName}' already exists (ID: ${existing[0].productId})")
    }

    String productId = delegator.getNextSeqId('Product')

    GenericValue product = delegator.makeValue('Product', [
        productId    : productId,
        internalName : productName,
        productTypeId: productTypeId,
        isVirtual    : 'N',
        isVariant    : 'N'
    ])
    delegator.create(product)

    GenericValue price1 = delegator.makeValue('ProductPrice', [
        productId           : productId,
        productPriceTypeId  : 'LIST_PRICE',
        productPricePurposeId: 'PURCHASE',
        currencyUomId       : currencyUomId,
        productStoreGroupId : '_NA_',
        fromDate            : new java.sql.Timestamp(System.currentTimeMillis()),
        price               : price
    ])
    delegator.create(price1)

    GenericValue catMember = delegator.makeValue('ProductCategoryMember', [
        productCategoryId: productCategoryId,
        productId        : productId,
        fromDate         : new java.sql.Timestamp(System.currentTimeMillis())
    ])
    delegator.create(catMember)

    result.productId = productId
    return result
}

Map updateProduct() {
    Map result = success()

    String productId = parameters.productId

    GenericValue product = from('Product').where('productId', productId).queryOne()
    if (!product) {
        return error("Product not found: ${productId}")
    }

    if (parameters.price != null) {
        String currencyUomId = parameters.currencyUomId ?: 'USD'
        List prices = from('ProductPrice')
            .where('productId', productId, 'productPriceTypeId', 'LIST_PRICE', 'currencyUomId', currencyUomId)
            .orderBy('-fromDate')
            .queryList()

        if (prices) {
            GenericValue priceRecord = prices[0]
            priceRecord.set('price', parameters.price)
            priceRecord.store()
        } else {
            GenericValue newPrice = delegator.makeValue('ProductPrice', [
                productId           : productId,
                productPriceTypeId  : 'LIST_PRICE',
                productPricePurposeId: 'PURCHASE',
                currencyUomId       : currencyUomId,
                productStoreGroupId : '_NA_',
                fromDate            : new java.sql.Timestamp(System.currentTimeMillis()),
                price               : parameters.price
            ])
            delegator.create(newPrice)
        }
    }

    if (parameters.productFeatureId) {
        GenericValue existing = from('ProductFeatureAppl')
            .where('productId', productId, 'productFeatureId', parameters.productFeatureId)
            .queryFirst()

        if (!existing) {
            delegator.create(delegator.makeValue('ProductFeatureAppl', [
                productId       : productId,
                productFeatureId: parameters.productFeatureId,
                fromDate        : new java.sql.Timestamp(System.currentTimeMillis()),
                productFeatureApplTypeId: 'STANDARD_FEATURE'
            ]))
        }
    }

    return result
}

Map assocProductToVirtual() {
    Map result = success()

    String productId = parameters.productId
    String virtualProductId = parameters.virtualProductId

    GenericValue variant = from('Product').where('productId', productId).queryOne()
    if (!variant) return error("Variant product not found: ${productId}")

    GenericValue virtual = from('Product').where('productId', virtualProductId).queryOne()
    if (!virtual) return error("Virtual product not found: ${virtualProductId}")

    if (virtual.isVirtual != 'Y') {
        return error("Product ${virtualProductId} is not marked as virtual")
    }

    GenericValue existing = from('ProductAssoc')
        .where('productId', virtualProductId, 'productIdTo', productId, 'productAssocTypeId', 'PRODUCT_VARIANT')
        .queryFirst()

    if (existing) {
        return error("Variant relationship already exists between ${virtualProductId} and ${productId}")
    }

    delegator.create(delegator.makeValue('ProductAssoc', [
        productId          : virtualProductId,
        productIdTo        : productId,
        productAssocTypeId : 'PRODUCT_VARIANT',
        fromDate           : new java.sql.Timestamp(System.currentTimeMillis())
    ]))

    variant.set('isVariant', 'Y')
    variant.store()

    return result
}

Map updateProductVariant() {
    Map result = success()

    String productId = parameters.productId
    String virtualProductId = parameters.virtualProductId
    String newVirtualProductId = parameters.newVirtualProductId

    GenericValue existing = from('ProductAssoc')
        .where('productId', virtualProductId, 'productIdTo', productId, 'productAssocTypeId', 'PRODUCT_VARIANT')
        .queryFirst()

    if (!existing) {
        return error("No virtual-variant relationship found between ${virtualProductId} and ${productId}")
    }

    GenericValue newVirtual = from('Product').where('productId', newVirtualProductId).queryOne()
    if (!newVirtual) return error("New virtual product not found: ${newVirtualProductId}")
    if (newVirtual.isVirtual != 'Y') return error("Product ${newVirtualProductId} is not marked as virtual")

    existing.set('thruDate', new java.sql.Timestamp(System.currentTimeMillis()))
    existing.store()

    delegator.create(delegator.makeValue('ProductAssoc', [
        productId          : newVirtualProductId,
        productIdTo        : productId,
        productAssocTypeId : 'PRODUCT_VARIANT',
        fromDate           : new java.sql.Timestamp(System.currentTimeMillis())
    ]))

    return result
}
