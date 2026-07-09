# SQL Assignment 2 — Queries

---

## Query 13 — Shipping Addresses for October 2023 Orders

**Business Problem:**
Customer Service might need to verify addresses for orders placed or completed in October 2023. This helps ensure shipments are delivered correctly and prevents address-related issues.

**Fields to Retrieve:**
- `ORDER_ID`
- `PARTY_ID` (Customer ID)
- `CUSTOMER_NAME` (FIRST_NAME / LAST_NAME)
- `STREET_ADDRESS`
- `CITY`
- `STATE_PROVINCE`
- `POSTAL_CODE`
- `COUNTRY_CODE`
- `ORDER_STATUS`
- `ORDER_DATE`

```sql
SELECT
    oh.order_id,
    orr.party_id,
    p.first_name,
    p.last_name,
    pa.address1 AS street_address,
    pa.city,
    pa.state_province_geo_id AS state_province,
    pa.postal_code,
    pa.country_geo_id AS country_code,
    oh.status_id AS order_status,
    oh.order_date
FROM order_header oh
JOIN order_role orr
    ON oh.order_id = orr.order_id
   AND orr.role_type_id = 'SHIP_TO_CUSTOMER'
JOIN person p
    ON p.party_id = orr.party_id
JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
   AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id
JOIN order_status os
    ON oh.order_id = os.order_id
   AND os.status_id = 'ORDER_COMPLETED'
WHERE
(
    oh.order_date >= '2023-10-01'
    AND oh.order_date < '2023-11-01'
)
OR
(
    os.status_datetime >= '2023-10-01'
    AND os.status_datetime < '2023-11-01'
);
```

---

## Query 14 — Orders from New York

**Business Problem:**
Companies often want region-specific analysis to plan local marketing, staffing, or promotions in certain areas — here, specifically, New York.

**Fields to Retrieve:**
- `ORDER_ID`
- `CUSTOMER_NAME`
- `STREET_ADDRESS`
- `CITY`
- `STATE_PROVINCE`
- `POSTAL_CODE`
- `TOTAL_AMOUNT`
- `ORDER_DATE`
- `ORDER_STATUS`

```sql
SELECT
    oh.order_id,
    CONCAT(p.first_name, ' ', p.last_name) AS customer_name,
    pa.address1 AS street_address,
    pa.city,
    pa.state_province_geo_id AS state_province,
    pa.postal_code,
    oh.grand_total AS total_amount,
    oh.order_date,
    oh.status_id AS order_status
FROM order_header oh
JOIN order_role orr
    ON oh.order_id = orr.order_id
   AND orr.role_type_id = 'SHIP_TO_CUSTOMER'
JOIN person p
    ON p.party_id = orr.party_id
JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
   AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id
WHERE pa.city = 'New York';
```

---

## Query 15 — Top-Selling Product in New York

**Business Problem:**
Merchandising teams need to identify the best-selling product(s) in a specific region (New York) for targeted restocking or promotions.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `INTERNAL_NAME`
- `TOTAL_QUANTITY_SOLD`
- `CITY` / `STATE` (within New York region)
- `REVENUE`

```sql
SELECT
    p.product_id,
    p.internal_name,
    SUM(oi.quantity) AS total_quantity_sold,
    'New York' AS city,
    SUM(oi.quantity * oi.unit_price) AS revenue
FROM order_header oh
JOIN order_item oi
    ON oh.order_id = oi.order_id
JOIN product p
    ON oi.product_id = p.product_id
JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
   AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id
WHERE pa.city = 'New York'
GROUP BY
    p.product_id,
    p.internal_name
ORDER BY total_quantity_sold DESC;
```

---

## Query 16 — Store-Specific (Facility-Wise) Revenue

**Business Problem:**
Different physical or online stores (facilities) may have varying levels of performance. The business wants to compare revenue across facilities for sales planning and budgeting.

**Fields to Retrieve:**
- `FACILITY_ID`
- `FACILITY_NAME`
- `TOTAL_ORDERS`
- `TOTAL_REVENUE`
- `DATE_RANGE`

```sql
SELECT
    f.facility_id,
    f.facility_name,
    COUNT(DISTINCT oh.order_id) AS total_orders,
    SUM(oi.quantity * oi.unit_price) AS total_revenue,
    MIN(oh.order_date) AS start_date,
    MAX(oh.order_date) AS end_date
FROM order_header oh
JOIN order_item oi
    ON oh.order_id = oi.order_id
JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id
   AND oisg.ship_group_seq_id = oi.ship_group_seq_id
JOIN facility f
    ON oisg.facility_id = f.facility_id
GROUP BY f.facility_id, f.facility_name;
```

---

## Query 17 — Lost and Damaged Inventory

**Business Problem:**
Warehouse managers need to track "shrinkage" such as lost or damaged inventory to reconcile physical vs. system counts.

**Fields to Retrieve:**
- `INVENTORY_ITEM_ID`
- `PRODUCT_ID`
- `FACILITY_ID`
- `QUANTITY_LOST_OR_DAMAGED`
- `REASON_CODE` (Lost, Damaged, Expired, etc.)
- `TRANSACTION_DATE`

```sql
SELECT
ii.inventory_item_id,
ii.product_id,
ii.facility_id,
iid.quantity_on_hand_diff AS quantity_lost_or_damaged,
iid.reason_enum_id AS reason_code,
iid.effective_date AS transaction_date
FROM inventory_item ii
JOIN inventory_item_detail iid
ON ii.inventory_item_id = iid.inventory_item_id
WHERE iid.reason_enum_id IN ('VAR_LOST','VAR_DAMAGED','EXPIRED');
```

---

## Query 18 — Low Stock or Out of Stock Items Report

**Business Problem:**
Avoiding out-of-stock situations is critical. This report flags items that have fallen below a certain reorder threshold or have zero available stock.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `PRODUCT_NAME`
- `FACILITY_ID`
- `QOH` (Quantity on Hand)
- `ATP` (Available to Promise)
- `REORDER_THRESHOLD`

```sql
SELECT
    pf.product_id,
    p.internal_name AS product_name,
    pf.facility_id,
    ii.quantity_on_hand_total AS qoh,
    ii.available_to_promise_total AS atp,
    pf.minimum_stock AS reorder_threshold
FROM product_facility pf
JOIN product p ON pf.product_id = p.product_id
JOIN inventory_item ii
    ON ii.product_id = pf.product_id
    AND ii.facility_id = pf.facility_id
WHERE ii.available_to_promise_total < pf.minimum_stock;
```

---

## Query 19 — Retrieve the Current Facility (Physical or Virtual) of Open Orders

**Business Problem:**
The business wants to know where open orders are currently assigned, whether in a physical store or a virtual facility (e.g., a distribution center or online fulfillment location).

**Fields to Retrieve:**
- `ORDER_ID`
- `ORDER_STATUS`
- `FACILITY_ID`
- `FACILITY_NAME`
- `FACILITY_TYPE_ID`

```sql
SELECT DISTINCT
    f.facility_name,
    f.facility_type_id,
    f.facility_id,
    oh.status_id AS order_status,
    oh.order_id
FROM order_item oi
JOIN order_header oh ON oh.order_id = oi.order_id
JOIN order_item_ship_group oisg
    ON oisg.order_id = oi.order_id
    AND oisg.ship_group_seq_id = oi.ship_group_seq_id
JOIN facility f ON oisg.facility_id = f.facility_id
WHERE oh.status_id NOT IN ('ORDER_COMPLETED', 'ORDER_CANCELLED');
```

---

## Query 20 — Items Where QOH and ATP Differ

**Business Problem:**
Sometimes the Quantity on Hand (QOH) doesn't match the Available to Promise (ATP) due to pending orders, reservations, or data discrepancies. This needs review for accurate fulfillment planning.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `FACILITY_ID`
- `QOH` (Quantity on Hand)
- `ATP` (Available to Promise)
- `DIFFERENCE` (QOH - ATP)

```sql
SELECT
    product_id,
    facility_id,
    quantity_on_hand_total AS qoh,
    available_to_promise_total AS atp,
    (quantity_on_hand_total - available_to_promise_total) AS difference
FROM inventory_item
WHERE quantity_on_hand_total <> available_to_promise_total;
```

---

## Query 21 — Order Item Current Status Changed Date-Time

**Business Problem:**
Operations teams need to audit when an order item's status (e.g., from "Approved" to "Completed") was last changed, for shipment tracking or dispute resolution.

**Fields to Retrieve:**
- `ORDER_ID`
- `ORDER_ITEM_SEQ_ID`
- `CURRENT_STATUS_ID`
- `STATUS_CHANGE_DATETIME`
- `CHANGED_BY`

```sql
SELECT
oh.order_id,
oi.order_item_seq_id,
os.status_id AS current_status_id,
os.status_datetime AS status_change_datetime,
os.status_user_login AS changed_by
FROM order_header oh
JOIN order_item oi
ON oh.order_id = oi.order_id
JOIN order_status os
ON oh.order_id = os.order_id
AND oi.order_item_seq_id = os.order_item_seq_id;
```

---

## Query 22 — Total Orders by Sales Channel

**Business Problem:**
Marketing and sales teams want to see how many orders come from each channel (e.g., web, mobile app, in-store POS, marketplace) to allocate resources effectively.

**Fields to Retrieve:**
- `SALES_CHANNEL`
- `TOTAL_ORDERS`
- `TOTAL_REVENUE`
- `REPORTING_PERIOD`

```sql
SELECT
    oh.sales_channel_enum_id AS sales_channel,
    COUNT(DISTINCT oh.order_id) AS total_orders,
    SUM(oi.quantity * oi.unit_price) AS total_revenue,
    YEAR(oh.order_date) AS reporting_period
FROM order_header oh
JOIN order_item oi
    ON oh.order_id = oi.order_id
WHERE oh.order_type_id = 'SALES_ORDER'
GROUP BY
    oh.sales_channel_enum_id,
    YEAR(oh.order_date);
```
