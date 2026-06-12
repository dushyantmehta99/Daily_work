# SQL Queries Reference

---

## Query 1 — New Customers Acquired in June 2023

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
  AND p.created_stamp >= '2025-06-01'
  AND p.created_stamp < '2026-07-01';
```

---

## Query 2 — List All Active Physical Products

```sql
SELECT
    product_id,
    product_type_id,
    internal_name
FROM product
WHERE product_type_id = 'FINISHED_GOOD'
  AND (
        sales_discontinuation_date IS NULL
        OR sales_discontinuation_date > CURRENT_TIMESTAMP
      );
```

---

## Query 3 — Products Missing NetSuite ID

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

```sql
SELECT
    p.product_id,
    gi_erp.id_value AS ns_id,
    gi_hs.id_value AS hs_id,
    gi_shop.id_value AS shopify_id
FROM product p
LEFT JOIN good_identification gi_erp 
    ON p.product_id = gi_erp.product_id AND gi_erp.good_identification_type_id = 'ERP_ID'
LEFT JOIN good_identification gi_hs 
    ON p.product_id = gi_hs.product_id AND gi_hs.good_identification_type_id = 'HS_CODE'
LEFT JOIN good_identification gi_shop 
    ON p.product_id = gi_shop.product_id AND gi_shop.good_identification_type_id = 'SHOPIFY_PROD_ID';
```

---

## Query 5 — Completed Orders in August 2023

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
    os.order_status_id AS order_history_id,
    oh.order_id,
    oi.order_item_seq_id
FROM order_header oh

JOIN order_item oi
    ON oh.order_id = oi.order_id

JOIN product p
    ON oi.product_id = p.product_id

LEFT JOIN order_status os
    ON oh.order_id = os.order_id

LEFT JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id

LEFT JOIN facility f
    ON oisg.facility_id = f.facility_id

WHERE oh.status_id = 'ORDER_COMPLETED'
  AND oh.order_date >= '2023-08-01'
  AND oh.order_date < '2023-09-01';
```

---

## Query 7 — Newly Created Sales Orders and Payment Methods

```sql
SELECT
    opp.payment_method_type_id,
    opp.order_id,
    opp.max_amount,
    oh.external_id
FROM order_payment_preference opp
JOIN order_header oh
    ON oh.order_id = opp.order_id
WHERE oh.order_type_id = 'SALES_ORDER'
ORDER BY opp.order_id;
```

---

## Query 8 — Payment Captured but Not Shipped

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

WHERE opp.status_id IN (
        'PAYMENT_RECEIVED',
        'PAYMENT_SETTLED',
        'PAYMENT_CAPTURED'
      )
  AND (
        s.shipment_id IS NULL
        OR s.status_id <> 'SHIPMENT_SHIPPED'
      );
```

---

## Query 9 — Orders Completed Hourly

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

```sql
SELECT
    COUNT(DISTINCT oh.order_id) AS total_orders,
    SUM(oh.grand_total) AS total_revenue
FROM order_header oh
JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id
JOIN shipment s
    ON s.primary_order_id = oh.order_id
WHERE oisg.shipment_method_type_id = 'STOREPICKUP'
  AND s.status_id = 'SHIPMENT_SHIPPED'
  AND s.estimated_ship_date >= '2025-01-01'
  AND s.estimated_ship_date < '2026-01-01';
```

---

## Query 11 — Canceled Orders (Last Month)

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

```sql
SELECT
    product_id,
    SUM(minimum_stock) AS total_minimum_stock
FROM product_facility
GROUP BY product_id;
```

---

## Query 13 — Shipping Addresses for October 2023 Orders

```sql
SELECT
    oh.order_id,
    p.party_id,
    p.first_name,
    p.last_name,
    pa.address1,
    pa.city,
    pa.state_province_geo_id,
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

```sql
SELECT
    oh.order_id,
    p.first_name,
    p.last_name,
    oh.grand_total,
    pa.address1,
    pa.city,
    pa.state_province_geo_id,
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
WHERE oh.status_id = 'ORDER_COMPLETED'
  AND pa.state_province_geo_id = 'NY';
```

---

## Query 15 — Top-Selling Product in New York

```sql
SELECT 
    sales.product_id,
    sales.internal_name,
    sales.city,
    sales.state_province_geo_id,
    sales.quantity,
    sales.revenue
FROM (
    SELECT
        oi.product_id,
        p.internal_name,
        pa.city,
        pa.state_province_geo_id,
        SUM(oi.quantity) AS quantity,
        SUM(oi.quantity * oi.unit_price) AS revenue
    FROM order_header oh
    LEFT JOIN order_item oi ON oh.order_id = oi.order_id
    LEFT JOIN product p ON p.product_id = oi.product_id
    LEFT JOIN order_role orr ON oh.order_id = orr.order_id AND orr.role_type_id = 'SHIP_TO_CUSTOMER'
    LEFT JOIN order_contact_mech ocm ON oh.order_id = ocm.order_id AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
    LEFT JOIN postal_address pa ON pa.contact_mech_id = ocm.contact_mech_id
    WHERE pa.state_province_geo_id = 'NY'
    GROUP BY oi.product_id, p.internal_name, pa.city, pa.state_province_geo_id
) sales
JOIN (
    SELECT city, MAX(quantity) AS max_qty
    FROM (
        SELECT
            pa.city,
            oi.product_id,
            SUM(oi.quantity) AS quantity
        FROM order_header oh
        LEFT JOIN order_item oi ON oh.order_id = oi.order_id
        LEFT JOIN order_role orr ON oh.order_id = orr.order_id AND orr.role_type_id = 'SHIP_TO_CUSTOMER'
        LEFT JOIN order_contact_mech ocm ON oh.order_id = ocm.order_id AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'
        LEFT JOIN postal_address pa ON pa.contact_mech_id = ocm.contact_mech_id
        WHERE pa.state_province_geo_id = 'NY'
        GROUP BY oi.product_id, pa.city
    ) t
    GROUP BY city
) max_sales ON sales.city = max_sales.city AND sales.quantity = max_sales.max_qty;
```

---

## Query 16 — Store-Specific (Facility-Wise) Revenue

```sql
SELECT
    f.facility_id,
    f.facility_name,
    COUNT(DISTINCT oi.order_id) AS total_orders,
    SUM(oi.quantity * oi.unit_price) AS total_revenue,
    MIN(oi.created_stamp) AS start_date,
    MAX(oi.created_stamp) AS end_date
FROM order_item oi
JOIN product_facility pf ON pf.product_id = oi.product_id
JOIN facility f ON f.facility_id = pf.facility_id
GROUP BY f.facility_id, f.facility_name;
```

---

## Query 17 — Lost and Damaged Inventory

```sql
SELECT
    ii.product_id,
    ii.facility_id,
    SUM(iid.quantity_on_hand_diff) AS total_diff,
    iid.reason_enum_id
FROM inventory_item_detail iid
LEFT JOIN inventory_item ii ON ii.inventory_item_id = iid.inventory_item_id
WHERE iid.reason_enum_id IN ('VAR_DAMAGED', 'VAR_LOST')
GROUP BY ii.product_id, ii.facility_id, iid.reason_enum_id;
```

---

## Query 18 — Low Stock or Out of Stock Items Report

```sql
SELECT
    pf.product_id,
    p.product_name,
    pf.facility_id,
    ii.quantity_on_hand_total AS qoh,
    ii.available_to_promise AS atp,
    pf.minimum_stock AS reorder_threshold
FROM product_facility pf
JOIN product p ON pf.product_id = p.product_id
LEFT JOIN inventory_item ii
    ON ii.product_id = pf.product_id
    AND ii.facility_id = pf.facility_id
WHERE ii.available_to_promise < pf.minimum_stock
   OR ii.available_to_promise <= 0;
```

---

## Query 19 — Retrieve the Current Facility (Physical or Virtual) of Open Orders

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

```sql
SELECT
    product_id,
    facility_id,
    quantity_on_hand_total AS qoh,
    available_to_promise_total AS atp,
    (quantity_on_hand_total - available_to_promise_total) AS difference
FROM inventory_item;
```

---

## Query 21 — Order Item Current Status Changed Date-Time

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

---

## Query 23 — Completed Sales Orders (Physical Items)

```sql
SELECT
    oi.order_id,
    oi.order_item_seq_id,
    p.product_id,
    p.product_type_id,
    oh.sales_channel_enum_id,
    oh.order_date,
    oh.entry_date,
    oh.status_id,
    oh.order_type_id,
    oh.product_store_id
FROM order_header oh
JOIN order_item oi ON oh.order_id = oi.order_id
LEFT JOIN product p ON p.product_id = oi.product_id
JOIN product_type pt ON p.product_type_id = pt.product_type_id
WHERE pt.is_physical = 'Y'
  AND oi.status_id <> 'ITEM_CANCELLED'
ORDER BY oi.order_id ASC;
```

---

## Query 24 — Completed Return Items

```sql
SELECT
    p.party_id,
    ri.order_id,
    p.first_name,
    p.last_name
FROM return_header rh
JOIN return_item ri ON ri.return_id = rh.return_id
LEFT JOIN person p ON p.party_id = rh.from_party_id
WHERE ri.order_id IN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(return_item_id) = 1
)
  AND rh.return_date BETWEEN '2026-05-01' AND '2026-06-01';
```

---

## Query 25 — Single-Return Orders (Last Month)

```sql
SELECT
    p.party_id,
    ri.order_id,
    p.first_name,
    p.last_name
FROM return_header rh
JOIN return_item ri ON ri.return_id = rh.return_id
LEFT JOIN person p ON p.party_id = rh.from_party_id
WHERE ri.order_id IN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(return_item_id) = 1
);
```

---

## Query 26 — Returns and Appeasements

```sql
SELECT
    COUNT(ri.return_item_id) AS total_returned_items,
    SUM(CASE WHEN ra.return_adjustment_type_id = 'Appeasement' THEN 1 ELSE 0 END) AS appeasement_count
FROM return_item ri
LEFT JOIN return_adjustment ra ON ri.return_id = ra.return_id;
```

---

## Query 27 — Detailed Return Information

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

```sql
SELECT DISTINCT
    ri.order_id,
    ri.return_id,
    rh.return_date,
    ri.return_reason_id,
    ri.return_quantity
FROM return_item ri
JOIN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(DISTINCT return_id) >= 2
) fo ON fo.order_id = ri.order_id
LEFT JOIN return_header rh ON rh.return_id = ri.return_id;
```

---

## Query 29 — Store with Most One-Day Shipped Orders (Last Month)

```sql
SELECT
    ois.facility_id,
    f.facility_name,
    COUNT(oi.order_id) AS total_order_items
FROM order_item oi
JOIN order_item_ship_group ois
    ON ois.order_id = oi.order_id
    AND ois.ship_group_seq_id = oi.ship_group_seq_id
LEFT JOIN facility f ON ois.facility_id = f.facility_id
JOIN order_status os
    ON os.order_id = oi.order_id
    AND os.status_id = 'ITEM_COMPLETED'
WHERE ois.shipment_method_type_id = 'NEXT_DAY'
  AND f.facility_type_id NOT IN (
      SELECT facility_type_id
      FROM facility_type
      WHERE parent_type_id = 'VIRTUAL_FACILITY'
  )
  AND oi.status_id = 'ITEM_COMPLETED'
  AND os.status_datetime >= NOW() - INTERVAL 30 DAY
GROUP BY ois.facility_id, f.facility_name;
```

---

## Query 30 — List of Warehouse Pickers

```sql
SELECT
    p.party_id,
    per.first_name,
    per.last_name,
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
    WHERE parent_type_id = 'VIRTUAL_FACILITY'
);
```

---

## Query 33 — Transfer Orders Without Inventory Reservation

```sql
SELECT
    oh.order_id AS transfer_order_id,
    oh.origin_facility_id AS from_facility_id,
    ois.facility_id AS to_facility_id,
    oi.product_id,
    oi.quantity AS requested_quantity,
    0 AS reserved_quantity,
    oh.order_date AS transfer_date,
    oh.status_id AS status
FROM order_header oh
JOIN order_item oi ON oi.order_id = oh.order_id
JOIN order_item_ship_group ois ON ois.order_id = oh.order_id
LEFT JOIN order_item_ship_grp_inv_res otshir
    ON otshir.order_id = ois.order_id
    AND otshir.ship_group_seq_id = ois.ship_group_seq_id
    AND otshir.order_item_seq_id = oi.order_item_seq_id
WHERE oh.order_type_id = 'TRANSFER_ORDER'
  AND otshir.reserved_datetime IS NULL
  AND oh.status_id = 'ORDER_APPROVED'
ORDER BY oh.order_id;
```

---

## Query 34 — Orders Without Picklist

```sql
SELECT DISTINCT
    oh.order_id,
    oh.order_date,
    oh.status_id AS order_status,
    ois.facility_id
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
