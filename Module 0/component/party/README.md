### Step 1 — Configure MySQL Database

Create the database in MySQL:

```sql
CREATE DATABASE moqui_party CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update `runtime/conf/MoquiDevConf.xml` to point to MySQL:

```xml
<default-property name="entity_ds_db_conf"  value="mysql8"/>
<default-property name="entity_ds_user"     value="root"/>
<default-property name="entity_ds_password" value="root123"/>
<default-property name="entity_ds_database" value="moqui_party"/>
```

> The `entity_empty_db_load = "all"` property (already set in `MoquiDevConf.xml`) ensures all seed data is loaded automatically on first run.

---

### Step 3 — Add MySQL JDBC Driver

Download `mysql-connector-j-8.x.jar` and place it in:

```
moqui-framework/runtime/lib/
```

---

### Step 4 — Run Moqui

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew run
```

Moqui will:
- Connect to `moqui_party` MySQL database
- Create all party tables automatically (`PARTY`, `PERSON`, `PARTY_GROUP`, `CONTACT_MECH`, `PARTY_CONTACT_MECH`)
- Load seed data (sample parties, contact mechs, enum types)
- Register the Party App in the top navigation
- Mount REST API at `/rest/s1/Party/`

---

### Step 5 — Access the App

| What | URL |
|------|-----|
| Login | `http://localhost:8080` (john.doe / moqui) |
| Party Screen | `http://localhost:8080/qapps/party/PartyScreen` |
| REST API | `POST http://localhost:8080/rest/s1/Party/Person/create` |

---

## Component Structure

```
party/
├── component.xml               # Component registration
├── MoquiConf.xml               # Screen + REST API mount config
├── README.md                   # This file
├── entity/
│   └── Party.xml               # 5 entity definitions
├── data/
│   └── PartyData.xml           # Seed data + enum types
├── screen/
│   ├── PartyScreen.xml         # Party list + create form
│   └── webroot/apps/
│       └── party.xml           # App entry point
└── service/
    ├── PartyServices.xml       # Service definition
    ├── PartyServices.groovy    # Groovy implementation
    └── Person.rest.xml         # REST API definition
```

---

## Entities

| Entity | Primary Key | Description |
|--------|-------------|-------------|
| `party.Party` | `partyId` | Base party record |
| `party.Person` | `partyId` | Individual person details |
| `party.PartyGroup` | `partyId` | Organization/group details |
| `party.ContactMech` | `contactMechId` | Contact mechanism (email, phone, address) |
| `party.PartyContactMech` | `partyId + contactMechId + fromDate` | Links party to contact mech |

---

## REST API

**Create a Person**

```
POST http://localhost:8080/rest/s1/Party/Person/create
Content-Type: application/json

{
  "partyId": "P010",
  "firstName": "Jane",
  "lastName": "Smith",
  "dateOfBirth": "1995-03-20"
}
```
