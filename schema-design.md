## MySQL Database Design

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id)
- patient_id: INT, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)

### Table: doctors
- id: INT, Primary Key, Auto Increment
- doctor_first_name: VARCHAR
- doctor_surname: VARCHAR
- doctor_email: VARCHAR
- specialism: VARCHAR
- 
## MongoDB Collection Design
