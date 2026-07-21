## MySQL Database Design

### Table: appointments
- id: INT, Primary Key, Auto Increment, Not Null
- doctor_id: INT, Foreign Key → doctors(id), Not Null
- patient_id: INT, Foreign Key → patients(id), Not Null
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)

### Table: doctors
- id: INT, Primary Key, Auto Increment, Not Null
- doctor_first_name: VARCHAR(50)
- doctor_surname: VARCHAR(50)
- doctor_email: VARCHAR(100), Not Null
- doctor_password: VARCHAR(100), Not Null
- doctor_telno: INT
- specialism: VARCHAR(100)
- availableTimes: VARCHAR(100)

### Table: patients
- id: INT, Primary Key, Auto Increment, Not Null
- patient_first_name: VARCHAR(5)
- patient_surname: VARCHAR(50)
- patient_email: VARCHAR(100), Not Null
- patient_telno: INT
- patient_password: VARCHAR(100), Not Null

### Table: admin
- id: INT, Primary Key, Auto Increment
- admin_email: VARCHAR(100), Not Null
- admin_password: VARCHAR(100), Not Null

What happens if a patient is deleted? Should appointments also be deleted? Yes, it makes sense to delete their appointments.
Should a doctor be allowed to have overlapping appointments? No.
Should each doctor have their own available time slots? Yes.
Should a patient's past appointment history be retained forever? No, it should be deleted after 5 years of them leaving the practice, if they leave. Otherwise, it should be kept permanently.
Is a prescription tied to a specific appointment or can it exist independently? It can exist independently, as sometimes a patient might ask for an emergency prescription over the phone or at the reception desk, without an official appointment.

## MongoDB Collection Design

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "doctorNotes": "Take 1 tablet every 6 hours.",
  "refillCount": 2,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  }
}


