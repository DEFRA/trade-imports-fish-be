# Fish Import Notifications

Notifications describe consignments of fish or fish products being imported into Great Britain.

## Language

**Notification**:
A record of a planned import, with a stable identity, an owning organisation and a lifecycle from draft to submission.
_Avoid_: Application

**Notification content**:
The details of the consignment, including its transport, arrival, goods, parties and supporting documents.

**Draft**:
A notification being completed before submission. Its content may be incomplete.
_Avoid_: Submission

**Transport mode**:
The means by which the consignment arrives in Great Britain: vessel, air freight or road vehicle.
_Avoid_: Transport method

**Arrival**:
When and where the consignment is expected to arrive in Great Britain: its port of entry and expected arrival.

**Port of entry**:
The port or airport in Great Britain where the consignment arrives.
_Avoid_: Destination port, entry point

**Expected arrival**:
The date and time the consignment is expected to arrive at the port of entry, as given in UK local time. It must not be in the past when entered.
_Avoid_: ETA, arrival time (when meaning the date as well)

**Border control post**:
The place serving a port of entry where border control checks are carried out. It is not chosen by the importer.
_Avoid_: BCP (in user-facing text)

**Country of export**:
The country the consignment is exported from.
_Avoid_: Country of origin, origin country

**Export region**:
Whether the country of export is in the EU or the rest of the world. It follows from the country of export and is never chosen by the importer.

**Rest of the world**:
Any country of export outside the EU-27.
_Avoid_: Third country, non-EU (in user-facing text)

**CHED reference**:
The reference of the Common Health Entry Document (CHED-P) already issued for the consignment. It is needed only when the country of export is in the rest of the world.
_Avoid_: CHED number, certificate number

**Notification reference**:
The unique human-readable reference allocated when a notification is first saved. It remains the same when the notification changes.
