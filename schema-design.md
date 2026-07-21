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
- doctor_telno: INT
- specialism: VARCHAR

### Table: patients
- id: INT, Primary Key, Auto Increment
- patient_first_name: VARCHAR
- patient_surname: VARCHAR
- patient_email: VARCHAR
- patient_telno: INT

### Table: admin
- 
## MongoDB Collection Design
