## MySQL Database Design

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id)
- patient_id: INT, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)

### Table: doctors
- id: INT, Primary Key, Auto Increment
- doctor_first_name: VARCHAR(50)
- doctor_surname: VARCHAR(50)
- doctor_email: VARCHAR(100)
- doctor_password: VARCHAR(100)
- doctor_telno: INT
- specialism: VARCHAR(100)

### Table: patients
- id: INT, Primary Key, Auto Increment
- patient_first_name: VARCHAR(5)
- patient_surname: VARCHAR(50)
- patient_email: VARCHAR(100)
- patient_telno: INT
- patient_password: VARCHAR(100)

### Table: admin
- id: INT, Primary Key, Auto Increment
- admin_email: VARCHAR(100)
- admin_password: VARCHAR(100)

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
