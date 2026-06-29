# Technical Integration Checklist: Shipping Provider API Matrix
**Providers:** FedEx · ShipHawk · Canada Post

---

## 1. Authentication Mechanisms

| Dimension | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Mechanism** | OAuth 2.0 — Client Credentials flow | Static API Key | HTTP Basic Authentication |
| **Credentials** | `client_id` + `client_secret` | Single API key per environment | `userid:password` (Base64-encoded) |
| **Token endpoint** | `POST /oauth/token` | N/A | N/A |
| **Credential lifetime** | Access token expires in 3 600 s | Key does not expire | Credentials do not expire |
| **How to pass** | `Authorization: Bearer <token>` | `X-Api-Key: <key>` header | `Authorization: Basic <Base64(userid:password)>` |
| **Env-specific keys** | No — base URL selects environment | Yes — separate key per environment | Yes — separate dev vs. production key pairs |

---

## 2. Communication Protocols

| Dimension | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Style** | REST | REST | REST |
| **Serialization** | JSON | JSON | XML (versioned vendor MIME types) |
| **HTTP methods** | POST, PUT, GET | GET, POST, DELETE | GET, POST, DELETE |
| **Production base URL** | `https://apis.fedex.com` | `https://shiphawk.com/api/v4` | `https://soa-gw.canadapost.ca` |
| **Sandbox base URL** | `https://apis-sandbox.fedex.com` | `https://sandbox.shiphawk.com/api/v4` | `https://ct.soa-gw.canadapost.ca` |
| **Content-Type** | `application/json` | `application/json` | `application/vnd.cpc.{service}-v{N}+xml` |
| **Accept header** | `application/json` | `application/json` | Same versioned MIME type — wildcard `*/*` causes 406 |
| **API versioning** | URL path (`/v1/`) per endpoint | URL prefix (`/api/v4/`) | Embedded in MIME type suffix (`-v4+xml`, `-v8+xml`) |

### Key Endpoints

| Capability | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Get rates** | `POST /rate/v1/rates/quotes` | `POST /rates` | `POST /rs/ship/price` |
| **Create shipment** | `POST /ship/v1/shipments` | `POST /shipments` | `POST /rs/{customer}/{mobo}/shipment` |
| **Cancel shipment** | `PUT /ship/v1/shipments/cancel` | `POST /shipments/:id/cancel` | `DELETE /rs/{customer}/{mobo}/shipment/{id}` |
| **Get label** | Inline in create response | `GET /shipments/:id/label` | Dynamic href from create response |

---

## 3. Request / Response Data Schemas

### 3a. Rate Request

| Field | FedEx (JSON) | ShipHawk (JSON) | Canada Post (XML) |
|---|---|---|---|
| **Origin postal code** | `shipper.address.postalCode` | `origin.zip` | `<origin-postal-code>` |
| **Destination postal code** | `recipients[0].address.postalCode` | `destination.zip` | `<destination><domestic><postal-code>` |
| **Destination country** | `recipients[0].address.countryCode` | `destination.country` | `<destination><international><country-code>` |
| **Weight** | `weight.value` + `weight.units` (LB/KG) | `items[0].weight` (lbs) | `<parcel-characteristics><weight>` (kg) |
| **Dimensions** | `dimensions.length/width/height` + `units` | `items[0].length/width/height` (in) | `<dimensions><length/width/height>` (cm) |
| **Account number** | Required: `accountNumber.value` | Tied to API key | Optional: `<customer-number>` |

### 3b. Rate Response

| Field | FedEx (JSON) | ShipHawk (JSON) | Canada Post (XML) |
|---|---|---|---|
| **Service code** | `serviceType` | `service` | `<service-code>` |
| **Total cost** | `ratedShipmentDetails[0].totalNetFedExCharge.amount` | `cost` | `<price-details><due>` |
| **Currency** | `totalNetCharge.currency` | USD (implied) | CAD (implied) |
| **Transit days** | `commit.transitDays.description` | `transit_days` | `<service-standard><expected-transit-time>` |
| **Rate ID** | N/A | `id` — **required to book; valid 2 hours** | N/A |

### 3c. Shipment Create Request

| Field | FedEx (JSON) | ShipHawk (JSON) | Canada Post (XML) |
|---|---|---|---|
| **Shipper name / address** | `shipper.contact.personName`, `shipper.address.*` | `origin.name`, `origin.street1/city/state/zip` | `<sender><name>`, `<sender><address-details>*` |
| **Recipient name / address** | `recipients[0].contact.personName`, `recipients[0].address.*` | `destination.name`, `destination.*` | `<destination><name>`, `<destination><address-details>*` |
| **Service code** | `requestedShipment.serviceType` | `service` + `rate_id` (both required) | `<delivery-spec><service-code>` |
| **Weight** | `weight.value` + `units` | `packages[0].weight` (lbs) | `<parcel-characteristics><weight>` (kg) |
| **Dimensions** | `dimensions.length/width/height` + `units` | `packages[0].length/width/height` (in) | `<dimensions><length/width/height>` (cm) |
| **Label format** | `labelSpecification.imageType` (PDF/PNG/ZPL) | Set via Accept header on label GET | `<print-preferences><encoding>` (PDF or ZPL) |
| **Ship date** | `requestedShipment.shipDatestamp` | Not required | `<delivery-spec><mailing-date>` |

### 3d. Shipment Create Response

| Field | FedEx (JSON) | ShipHawk (JSON) | Canada Post (XML) |
|---|---|---|---|
| **Tracking number** | `output.transactionShipments[0].masterTrackingNumber` | `tracking_number` | `<shipment-info><tracking-pin>` |
| **Shipment ID** | `transactionShipments[0].shipmentDocuments[0].shipmentId` | `id` | `<shipment-info><shipment-id>` |
| **Label** | Base64 in `pieceResponses[0].packageDocuments[0].encodedLabel` | Not inline — `GET /shipments/:id/label` | Not inline — follow `<links><link rel="label">` href |
| **Total cost** | `shipmentRating.shipmentRateDetails[0].totalNetCharge.amount` | `cost` | `<price-details><due>` |
| **Status** | Implied by HTTP 200 | `status` string | `<shipment-info><shipment-status>` |

---

## 4. Document Rendering Variations

| Dimension | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Supported formats** | PDF, PNG, ZPL, EPL2 | PDF, ZPL (carrier-dependent) | PDF, ZPL II |
| **Label size** | 4×6 thermal or 8.5×11 laser | Carrier-determined | 4×6 thermal **or** 8.5×11 paper |
| **ZPL restriction** | Any size configurable | Available; not always configurable | ZPL II **only** valid with `output-format=4x6` |
| **Label delivery** | Base64 inline in create response | Separate `GET /shipments/:id/label` | Separate GET to dynamic href from create response |
| **Format selection** | `labelSpecification.imageType` in create request | Accept header on label GET | `<print-preferences><encoding>` in create request |
| **Label expiry** | Not specified | Not documented | 90 days (standard); 5 days (authorized return) |
| **Manifest** | `POST /manifest/v1/manifests` | Via batch API | Required before drop-off: `POST /rs/{customer}/{mobo}/transmit` |

---

## 5. Error-Handling Paradigms

### 5a. HTTP Status Codes

| Code | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **200** | Success | Success | Success **or** business failure — inspect `<messages>` |
| **400** | Malformed request | Invalid params | Schema validation failure |
| **401** | Token expired / invalid | N/A | Auth failure |
| **403** | Permission denied | Wrong key or wrong env | Authorization denied |
| **406 / 415** | N/A | N/A | Wrong MIME type in Accept / Content-Type |
| **429** | Rate limit exceeded | Not documented | N/A |
| **500** | System error | Server error | Server error |

### 5b. Error Response Structure

| Dimension | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Format** | JSON: `{ "errors": [{ "code": "...", "message": "..." }] }` | JSON: `{ "errors": ["..."] }` | XML `<messages><message><code>/<description>` |
| **Error code style** | Dot-separated alphanumeric (`VALIDATION.ERROR`) | HTTP status communicates category | Numeric (`7050`) or alpha-prefix (`AA001`) |
| **Partial success** | `output.alerts[]` alongside success | N/A | HTTP 200 + `<messages>` = partial failure |
| **Key rule** | Match on `code` — message strings change dynamically | Match on HTTP status | Always check `<messages>` element even on HTTP 200 |

### 5c. Retry Strategy

| Trigger | FedEx | ShipHawk | Canada Post |
|---|---|---|---|
| **Token / auth failure** | 401 → refresh token → retry once | 403 = config error, do not retry | N/A |
| **Rate limit** | 429 → wait 10 s | Not documented | `Server` error → back off |
| **Transient server error** | Exponential backoff on 5xx | Exponential backoff on 500 | Validate XML; retry on 500 |
| **Resource not ready** | N/A | N/A | HTTP 202 → wait ~1 s; retry |
| **Do NOT retry** | `VALIDATION.ERROR`, `FORBIDDEN.ERROR` | HTTP 403, 422 | HTTP 400/401/403, AA-series errors |

---

## Sources
- [FedEx Authorization API](https://developer.fedex.com/api/en-us/catalog/authorization/docs.html)
- [FedEx Ship API](https://developer.fedex.com/api/en-us/catalog/ship/docs.html)
- [FedEx Rate API](https://developer.fedex.com/api/en-us/catalog/rate/docs.html)
- [ShipHawk API Documentation](https://docs.shiphawk.com/)
- [Canada Post — Developer Fundamentals](https://www.canadapost-postescanada.ca/info/mc/business/productsservices/developers/services/fundamentals.jsf)
- [Canada Post — Get Rates Service](https://www.canadapost-postescanada.ca/info/mc/business/productsservices/developers/services/rating/getrates/default.jsf)
- [Canada Post — Create Shipment](https://www.canadapost-postescanada.ca/info/mc/business/productsservices/developers/services/shippingmanifest/createshipment.jsf)
- [Canada Post — Errors & Code Tables](https://www.canadapost-postescanada.ca/info/mc/business/productsservices/developers/messagescodetables.jsf)
