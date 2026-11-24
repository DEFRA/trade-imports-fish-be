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

**Notification reference**:
The unique human-readable reference allocated when a notification is first saved. It remains the same when the notification changes.
