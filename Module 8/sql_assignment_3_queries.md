# SQL Assignment 3 — Queries

---

## Query 23 — Completed Sales Orders (Physical Items)

**Business Problem:**
Merchants need to track only physical items (requiring shipping and fulfillment) for logistics and shipping-cost analysis.

**Fields to Retrieve:**
- `ORDER_ID`
- `ORDER_ITEM_SEQ_ID`
- `PRODUCT_ID`
- `PRODUCT_TYPE_ID`
- `SALES_CHANNEL_ENUM_ID`
- `ORDER_DATE`
- `ENTRY_DATE`
- `STATUS_ID`
- `STATUS_DATETIME`
- `ORDER_TYPE_ID`
- `PRODUCT_STORE_ID`

```sql
SELECT OH.order_id,
       OI.order_item_seq_id,
       OI.product_id,
       P.product_type_id,
       OH.sales_channel_enum_id,
       OH.order_date,
       OH.entry_date,
       OH.status_id,
       OS.status_datetime,
       OH.order_type_id,
       OH.product_store_id
FROM order_header OH
JOIN order_item OI ON OH.order_id = OI.order_id
JOIN order_status OS
    ON OH.order_id = OS.order_id
   AND OS.status_id = 'ORDER_COMPLETED'
JOIN product P ON OI.product_id = P.product_id
JOIN product_type pt ON P.product_type_id = pt.product_type_id
WHERE OH.status_id = 'ORDER_COMPLETED'
  AND pt.is_physical = 'Y'
  AND OH.order_type_id = 'SALES_ORDER';
```

---

## Query 24 — Completed Return Items

**Business Problem:**
Customer service and finance often need insights into returned items to manage refunds, replacements, and inventory restocking.

**Fields to Retrieve:**
- `RETURN_ID`
- `ORDER_ID`
- `PRODUCT_STORE_ID`
- `STATUS_DATETIME`
- `ORDER_NAME`
- `FROM_PARTY_ID`
- `RETURN_DATE`
- `ENTRY_DATE`
- `RETURN_CHANNEL_ENUM_ID`

```sql
SELECT
rh.return_id,
ri.order_id,
oh.product_store_id,
rs.status_datetime,
oh.order_name,
rh.from_party_id,
rh.return_date,
rh.entry_date,
rh.return_channel_enum_id
FROM return_header rh
JOIN return_item ri
ON rh.return_id = ri.return_id
JOIN return_status rs
ON rh.return_id = rs.return_id
JOIN order_header oh
ON ri.order_id = oh.order_id
WHERE rh.return_header_type_id='CUSTOMER_RETURN'
AND rh.status_id='RETURN_COMPLETED';
```

---

## Query 25 — Single-Return Orders (Last Month)

**Business Problem:**
The merchandising team needs a list of orders that only have one return.

**Fields to Retrieve:**
- `PARTY_ID`
- `FIRST_NAME`

```sql
SELECT
p.party_id,
p.first_name
FROM person p
JOIN return_header rh
ON p.party_id = rh.from_party_id
GROUP BY
p.party_id,
p.first_name
HAVING COUNT(DISTINCT rh.return_id)=1;
```

---

## Query 26 — Returns and Appeasements

**Business Problem:**
The retailer needs the total amount of items that were returned as well as how many appeasements were issued.

**Fields to Retrieve:**
- `TOTAL RETURNS`
- `RETURN $ TOTAL`
- `TOTAL APPEASEMENTS`
- `APPEASEMENTS $ TOTAL`

```sql
SELECT
SUM(ri.RETURN_QUANTITY * ri.RETURN_PRICE) 
AS TOTAL_RETURN_AMOUNT,
COUNT(DISTINCT ri.RETURN_ID)
AS TOTAL_RETURNS,
SUM(ra.AMOUNT)
AS TOTAL_APPEASEMENTS_AMOUNT,
COUNT(DISTINCT ra.RETURN_ADJUSTMENT_ID)
AS TOTAL_APPEASEMENTS
FROM return_header rh
JOIN return_item ri
ON rh.RETURN_ID = ri.RETURN_ID
JOIN return_adjustment ra
ON rh.RETURN_ID = ra.RETURN_ID
WHERE RETURN_ADJUSTMENT_TYPE_ID='APPEASEMENTS';
```

---

## Query 27 — Detailed Return Information

**Business Problem:**
Certain teams need granular return data (reason, date, refund amount) for analyzing return rates, identifying recurring issues, or updating policies.

**Fields to Retrieve:**
- `RETURN_ID`
- `ENTRY_DATE`
- `RETURN_ADJUSTMENT_TYPE_ID` (refund type, store credit, etc.)
- `AMOUNT`
- `COMMENTS`
- `ORDER_ID`
- `ORDER_DATE`
- `RETURN_DATE`
- `PRODUCT_STORE_ID`

```sql
SELECT
    rh.return_id,
    oh.entry_date,
    ra.return_adjustment_type_id,
    ra.amount,
    ra.comments,
    oh.order_id,
    oh.order_date,
    rh.return_date,
    oh.product_store_id
FROM return_header rh
JOIN return_item ri ON rh.return_id = ri.return_id
LEFT JOIN order_header oh ON ri.order_id = oh.order_id
LEFT JOIN return_adjustment ra ON ra.return_id = ri.return_id;
```

---

## Query 28 — Orders with Multiple Returns

**Business Problem:**
Analyzing orders with multiple returns can identify potential fraud, chronic issues with certain items, or inconsistent shipping processes.

**Fields to Retrieve:**
- `ORDER_ID`
- `RETURN_ID`
- `RETURN_DATE`
- `RETURN_REASON`
- `RETURN_QUANTITY`

```sql
SELECT
ri.ORDER_ID,
ri.RETURN_ID,
rh.RETURN_DATE,
ri.RETURN_REASON_ID AS RETURN_REASON,
ri.RETURN_QUANTITY
FROM return_header rh
JOIN return_item ri
ON rh.RETURN_ID = ri.RETURN_ID
WHERE ri.ORDER_ID IN
(
SELECT ORDER_ID
FROM return_item
GROUP BY ORDER_ID
HAVING COUNT(ORDER_ID)>1
);
```

---

## Query 29 — Store with Most One-Day Shipped Orders (Last Month)

**Business Problem:**
Identify which facility (store) handled the highest volume of "one-day shipping" orders in the previous month, useful for operational benchmarking.

**Fields to Retrieve:**
- `FACILITY_ID`
- `FACILITY_NAME`
- `TOTAL_ONE_DAY_SHIP_ORDERS`
- `REPORTING_PERIOD`

```sql
SELECT
f.facility_id,
f.facility_name,
COUNT(DISTINCT oh.order_id)
AS total_one_day_ship_orders,
DATE_FORMAT(
DATE_SUB(CURDATE(),INTERVAL 1 MONTH),
'%Y-%m'
)
AS reporting_period
FROM facility f
JOIN shipment s
ON f.facility_id=s.origin_facility_id
JOIN order_header oh
ON s.primary_order_id=oh.order_id
GROUP BY
f.facility_id,
f.facility_name
ORDER BY total_one_day_ship_orders DESC
LIMIT 1;
```

---

## Query 30 — List of Warehouse Pickers

**Business Problem:**
Warehouse managers need a list of employees responsible for picking and packing orders to manage shifts, productivity, and training needs.

**Fields to Retrieve:**
- `PARTY_ID` (Employee ID)
- `NAME` (First/Last)
- `ROLE_TYPE_ID`
- `FACILITY_ID` (assigned warehouse)
- `STATUS` (active or inactive employee)

```sql
SELECT
    p.party_id,
    per.first_name,
    per.last_name,
    pr.role_type_id,
    p.status_id,
    fp.facility_id
FROM party p
LEFT JOIN person per ON p.party_id = per.party_id
JOIN party_role pr
    ON p.party_id = pr.party_id
    AND pr.role_type_id = 'WAREHOUSE_PICKER'
LEFT JOIN facility_party fp
    ON fp.party_id = p.party_id
    AND fp.role_type_id = 'WAREHOUSE_PICKER';
```

---

## Query 31 — Total Facilities That Sell the Product

**Business Problem:**
Retailers want to see how many (and which) facilities (stores, warehouses, virtual sites) currently offer a product for sale.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `PRODUCT_NAME` / `INTERNAL_NAME`
- `FACILITY_COUNT` (number of facilities selling the product)

```sql
SELECT
    p.product_id,
    p.internal_name,
    COUNT(DISTINCT pf.facility_id) AS facility_count
FROM product p
JOIN product_price pp
    ON pp.product_id = p.product_id
    AND pp.product_price_type_id = 'LIST_PRICE'
LEFT JOIN product_facility pf ON pf.product_id = p.product_id
GROUP BY p.product_id, p.internal_name;
```

---

## Query 32 — Total Items in Various Virtual Facilities

**Business Problem:**
Retailers need to study the relation of inventory levels of products to the type of facility it's stored at. Retrieve all inventory levels for products at locations and include the facility type ID. Do not retrieve facilities that are of type Virtual.

**Fields to Retrieve:**
- `PRODUCT_ID`
- `FACILITY_ID`
- `FACILITY_TYPE_ID`
- `QOH` (Quantity on Hand)
- `ATP` (Available to Promise)

```sql
SELECT
    pf.product_id,
    pf.facility_id,
    f.facility_type_id,
    ii.quantity_on_hand_total AS qoh,
    ii.available_to_promise_total AS atp
FROM product_facility pf
LEFT JOIN inventory_item ii
    ON ii.product_id = pf.product_id
    AND ii.facility_id = pf.facility_id
JOIN facility f ON f.facility_id = pf.facility_id
WHERE f.facility_type_id NOT IN (
    SELECT facility_type_id
    FROM facility_type
    WHERE facility_type_id = 'VIRTUAL_FACILITY'
       OR parent_type_id = 'VIRTUAL_FACILITY'
);
```

---

## Query 33 — Transfer Orders Without Inventory Reservation

**Business Problem:**
When transferring stock between facilities, the system should reserve inventory. If it isn't reserved, the transfer may fail or oversell.

**Fields to Retrieve:**
- `TRANSFER_ORDER_ID`
- `FROM_FACILITY_ID`
- `TO_FACILITY_ID`
- `PRODUCT_ID`
- `REQUESTED_QUANTITY`
- `RESERVED_QUANTITY`
- `TRANSFER_DATE`
- `STATUS`

```
```

---

## Query 34 — Orders Without Picklist

**Business Problem:**
A picklist is necessary for warehouse staff to gather items. Orders missing a picklist might be delayed and need attention.

**Fields to Retrieve:**
- `ORDER_ID`
- `ORDER_DATE`
- `ORDER_STATUS`
- `FACILITY_ID`
- `DURATION` (How long has the order been assigned at the facility)

```sql
SELECT DISTINCT
    oh.order_id,
    oh.order_date,
    oh.status_id AS order_status,
    ois.facility_id,
    DATEDIFF(NOW(), oh.order_date) AS duration_days
FROM order_header oh
JOIN order_item_ship_group ois ON oh.order_id = ois.order_id
LEFT JOIN shipment s ON ois.order_id = s.primary_order_id
LEFT JOIN facility f ON f.facility_id = ois.facility_id
WHERE s.shipment_id IS NULL
  AND oh.status_id = 'ORDER_APPROVED'
  AND f.facility_type_id NOT IN (
      SELECT facility_type_id
      FROM facility_type
      WHERE parent_type_id = 'VIRTUAL_FACILITY'
  );
```
