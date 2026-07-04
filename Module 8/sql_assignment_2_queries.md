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
    p.party_id,
    p.first_name,
    p.last_name,
    pa.address1,
    pa.city,
    pa.state_province_geo_id,
    pa.postal_code,
    pa.country_geo_id,
    oh.status_id,
    oh.order_date
FROM order_header oh
LEFT JOIN order_role orr
    ON oh.order_id = orr.order_id
    AND orr.role_type_id = 'SHIP_TO_CUSTOMER'
LEFT JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
    AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
LEFT JOIN postal_address pa ON pa.contact_mech_id = ocm.contact_mech_id
LEFT JOIN person p ON p.party_id = orr.party_id
WHERE (oh.order_date >= '2023-10-01' AND oh.order_date < '2023-11-01')
   OR (oh.status_id = 'ORDER_COMPLETED' AND oh.last_updated_stamp >= '2023-10-01' AND oh.last_updated_stamp < '2023-11-01');
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

```
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
    iid.inventory_item_id,
    ii.product_id,
    ii.facility_id,
    SUM(iid.quantity_on_hand_diff) AS total_diff,
    iid.reason_enum_id,
    iid.effective_date AS transaction_date
FROM inventory_item_detail iid
LEFT JOIN inventory_item ii ON ii.inventory_item_id = iid.inventory_item_id
WHERE iid.reason_enum_id IN ('VAR_DAMAGED', 'VAR_LOST')
GROUP BY iid.inventory_item_id, ii.product_id, ii.facility_id, iid.reason_enum_id, iid.effective_date;
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
LEFT JOIN inventory_item ii
    ON ii.product_id = pf.product_id
    AND ii.facility_id = pf.facility_id
WHERE ii.available_to_promise_total < pf.minimum_stock
   OR ii.available_to_promise_total <= 0;
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
INNER JOIN order_header oh ON oh.order_id = oi.order_id
LEFT JOIN order_item_ship_group oisg
    ON oisg.order_id = oi.order_id
    AND oisg.ship_group_seq_id = oi.ship_group_seq_id
LEFT JOIN facility f ON oisg.facility_id = f.facility_id
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
    os1.order_id,
    os2.order_item_seq_id,
    os2.status_id,
    os2.status_user_login,
    os2.status_datetime
FROM order_status os1
JOIN order_status os2
    ON os1.order_id = os2.order_id
    AND os1.order_item_seq_id = os2.order_item_seq_id
WHERE os1.status_id = 'ITEM_APPROVED'
  AND os2.status_id = 'ITEM_COMPLETED';
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
    SUM(oh.grand_total - IFNULL(ad.adjustments, 0)) AS total_revenue,
    oh.sales_channel_enum_id,
    COUNT(oh.order_id) AS total_orders,
    MIN(oh.entry_date) AS start_date,
    MAX(oh.entry_date) AS end_date
FROM order_header oh
LEFT JOIN (
    SELECT
        order_id,
        SUM(amount) AS adjustments
    FROM order_adjustment
    GROUP BY order_id
) ad ON ad.order_id = oh.order_id
WHERE oh.status_id = 'ORDER_COMPLETED'
GROUP BY oh.sales_channel_enum_id;
```
