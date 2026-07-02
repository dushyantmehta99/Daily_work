# SQL Assignment 1 — Queries

---

## Query 1 — New Customers Acquired in June 2023

**Business Problem:**
The marketing team ran a campaign in June 2023 and wants to see how many new customers signed up during that period.

**Fields to Retrieve:**
- `PARTY_ID`
- `FIRST_NAME`
- `LAST_NAME`
- `EMAIL`
- `PHONE`
- `ENTRY_DATE`

```sql
SELECT
    p.party_id,
    pp.first_name,
    pp.last_name,
    p.created_stamp AS entry_date,
    cm_email.info_string AS email,
    tn.contact_number AS phone
FROM party p

JOIN party_role pr
    ON p.party_id = pr.party_id

JOIN person pp
    ON pp.party_id = p.party_id

LEFT JOIN party_contact_mech pcm_email
    ON p.party_id = pcm_email.party_id

LEFT JOIN contact_mech cm_email
    ON pcm_email.contact_mech_id = cm_email.contact_mech_id
   AND cm_email.contact_mech_type_id = 'EMAIL_ADDRESS'

LEFT JOIN party_contact_mech pcm_phone
    ON p.party_id = pcm_phone.party_id

LEFT JOIN telecom_number tn
    ON pcm_phone.contact_mech_id = tn.contact_mech_id

WHERE pr.role_type_id = 'CUSTOMER'
  AND p.created_stamp >= '2023-06-01'
  AND p.created_stamp < '2023-07-01';
```

---

## Query 2 — List All Active Physical Products

**Business Problem:**
Merchandising teams often need a list of all physical products to manage logistics, warehousing, and shipping.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `PRODUCT_TYPE_ID`
- `INTERNAL_NAME`

```sql
SELECT
    p.product_id,
    p.product_type_id,
    p.internal_name
FROM product p
JOIN product_type pt ON p.product_type_id = pt.product_type_id
WHERE pt.is_physical = 'Y'
  AND (
        p.sales_discontinuation_date IS NULL
        OR p.sales_discontinuation_date > CURRENT_TIMESTAMP
      );
```

---

## Query 3 — Products Missing NetSuite ID

**Business Problem:**
A product cannot sync to NetSuite unless it has a valid NetSuite ID. The OMS needs a list of all products that still need to be created or updated in NetSuite.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `INTERNAL_NAME`
- `PRODUCT_TYPE_ID`
- `NETSUITE_ID` (may be `NULL` if missing)

```sql
SELECT
    p.product_id,
    p.internal_name,
    p.product_type_id,
    gi.id_value AS netsuite_id
FROM product p

LEFT JOIN good_identification gi
       ON p.product_id = gi.product_id
      AND gi.good_identification_type_id = 'ERP_ID'

WHERE gi.id_value IS NULL;
```

---

## Query 4 — Product IDs Across Systems

**Business Problem:**
To sync an order or product across multiple systems (e.g., Shopify, HotWax, ERP/NetSuite), the OMS needs to know each system's unique identifier for that product. This query retrieves the Shopify ID, HotWax ID, and ERP ID (NetSuite ID) for all products.

**Fields to Retrieve:**
- `PRODUCT_ID` (internal OMS ID)
- `SHOPIFY_ID`
- `HOTWAX_ID`
- `ERP_ID` / `NETSUITE_ID`

```sql
SELECT
    p.product_id,
    gi_erp.id_value AS netsuite_id,
    gi_hc.id_value AS hotwax_id,
    gi_shop.id_value AS shopify_id
FROM product p
LEFT JOIN good_identification gi_erp
    ON p.product_id = gi_erp.product_id AND gi_erp.good_identification_type_id = 'ERP_ID'
LEFT JOIN good_identification gi_hc
    ON p.product_id = gi_hc.product_id AND gi_hc.good_identification_type_id = 'HOTWAX_ID'
LEFT JOIN good_identification gi_shop
    ON p.product_id = gi_shop.product_id AND gi_shop.good_identification_type_id = 'SHOPIFY_PROD_ID';
```

---

## Query 5 — Completed Orders in August 2023

**Business Problem:**
After running similar reports for a previous month, you now need all completed orders in August 2023 for analysis.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `PRODUCT_TYPE_ID`
- `PRODUCT_STORE_ID`
- `TOTAL_QUANTITY`
- `INTERNAL_NAME`
- `FACILITY_ID`
- `EXTERNAL_ID`
- `FACILITY_TYPE_ID`
- `ORDER_HISTORY_ID`
- `ORDER_ID`
- `ORDER_ITEM_SEQ_ID`
- `SHIP_GROUP_SEQ_ID`

```sql
SELECT
    oi.product_id,
    p.product_type_id,
    oh.product_store_id,
    oi.quantity AS total_quantity,
    p.internal_name,
    f.facility_id,
    f.external_id,
    f.facility_type_id,
    ohis.order_history_id,
    oh.order_id,
    oi.order_item_seq_id,
    oisg.ship_group_seq_id
FROM order_header oh
JOIN order_item oi
    ON oh.order_id = oi.order_id
JOIN product p
    ON oi.product_id = p.product_id
LEFT JOIN order_history ohis
    ON oh.order_id = ohis.order_id
LEFT JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id
   AND oi.ship_group_seq_id = oisg.ship_group_seq_id
LEFT JOIN facility f
    ON oisg.facility_id = f.facility_id
WHERE oh.status_id = 'ORDER_COMPLETED'
  AND oh.order_date >= '2023-08-01'
  AND oh.order_date < '2023-09-01';
```

---

## Query 7 — Newly Created Sales Orders and Payment Methods

**Business Problem:**
Finance teams need to see new orders and their payment methods for reconciliation and fraud checks.

**Fields to Retrieve:**
- `ORDER_ID`
- `TOTAL_AMOUNT`
- `PAYMENT_METHOD`
- `Shopify Order ID` (if applicable)

```sql
SELECT
    opp.payment_method_type_id,
    opp.order_id,
    oh.grand_total AS total_amount,
    oh.external_id
FROM order_payment_preference opp
JOIN order_header oh
    ON oh.order_id = opp.order_id
WHERE oh.order_type_id = 'SALES_ORDER'
  AND oh.order_date >= NOW() - INTERVAL 30 DAY
ORDER BY opp.order_id;
```

---

## Query 8 — Payment Captured but Not Shipped

**Business Problem:**
Finance teams want to ensure revenue is recognized properly. If payment is captured but no shipment has occurred, it warrants further review.

**Fields to Retrieve:**
- `ORDER_ID`
- `ORDER_STATUS`
- `PAYMENT_STATUS`
- `SHIPMENT_STATUS`

```sql
SELECT DISTINCT
    oh.order_id,
    oh.status_id AS order_status,
    opp.status_id AS payment_status,
    s.status_id AS shipment_status
FROM order_header oh

JOIN order_payment_preference opp
    ON oh.order_id = opp.order_id

LEFT JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id

LEFT JOIN shipment s
    ON oisg.ship_group_seq_id = s.primary_ship_group_seq_id
   AND oisg.order_id = s.primary_order_id

WHERE opp.status_id = 'PAYMENT_SETTLED'
  AND (
        s.shipment_id IS NULL
        OR s.status_id <> 'SHIPMENT_SHIPPED'
      );
```

---

## Query 9 — Orders Completed Hourly

**Business Problem:**
Operations teams may want to see how orders complete across the day to schedule staffing.

**Fields to Retrieve:**
- `TOTAL ORDERS`
- `HOUR`

```sql
SELECT
    EXTRACT(HOUR FROM status_datetime) AS hour,
    COUNT(*)
FROM order_status
WHERE status_id = 'ORDER_COMPLETED'
GROUP BY EXTRACT(HOUR FROM status_datetime)
ORDER BY hour;
```

---

## Query 10 — BOPIS Orders Revenue (Last Year)

**Business Problem:**
BOPIS (Buy Online, Pickup In Store) is a key retail strategy. Finance wants to know the revenue from BOPIS orders for the previous year.

**Fields to Retrieve:**
- `TOTAL ORDERS`
- `TOTAL REVENUE`

```sql
SELECT
    COUNT(*) AS total_orders,
    SUM(oh.grand_total) AS total_revenue
FROM order_header oh
WHERE oh.status_id = 'ORDER_COMPLETED'
  AND oh.order_date >= '2025-01-01'
  AND oh.order_date < '2026-01-01'
  AND EXISTS (
        SELECT 1
        FROM order_item_ship_group oisg
        JOIN shipment s
             ON s.primary_order_id = oisg.order_id
            AND s.primary_ship_group_seq_id = oisg.ship_group_seq_id
        WHERE oisg.order_id = oh.order_id
          AND oisg.shipment_method_type_id = 'STOREPICKUP'
          AND s.status_id = 'SHIPMENT_SHIPPED'
    );
```

---

## Query 11 — Canceled Orders (Last Month)

**Business Problem:**
The merchandising team needs to know how many orders were canceled in the previous month and their reasons.

**Fields to Retrieve:**
- `TOTAL ORDERS`
- `CANCELLATION REASON`

```sql
SELECT
    COUNT(order_id) AS cancelled_orders_count,
    change_reason
FROM order_status
WHERE status_id = 'ITEM_CANCELLED'
  AND status_datetime BETWEEN '2026-05-01' AND '2026-06-01'
GROUP BY change_reason;
```

---

## Query 12 — Product Threshold Value

**Business Problem:**
The retailer has set a threshold value for products that are sold online, in order to avoid overselling.

**Fields to Retrieve:**
- `PRODUCT ID`
- `THRESHOLD`

```sql
SELECT
    pf.product_id,
    pf.minimum_stock AS threshold
FROM product_facility pf
JOIN facility f
    ON pf.facility_id = f.facility_id
   AND f.facility_type_id = 'CONFIGURATION';
```
