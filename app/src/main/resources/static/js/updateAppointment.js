// updateAppointment.js
import { updateAppointment } from "../js/services/appointmentRecordService.js";
import { getDoctors } from "../js/services/doctorServices.js";
document.addEventListener("DOMContentLoaded", initializePage);

async function initializePage() {
  const token = localStorage.getItem("token"); // Assuming token is stored in localStorage
  // Get appointmentId and patientId from the URL query parameters
  const urlParams = new URLSearchParams(window.location.search);
  const appointmentId = urlParams.get("appointmentId");
  const patientId = urlParams.get("patientId");
  const doctorId = urlParams.get("doctorId");
  const patientName = urlParams.get("patientName");
  const doctorName = urlParams.get("doctorName");
  const appointmentDate = urlParams.get("appointmentDate");
  const appointmentTime = urlParams.get("appointmentTime");

  console.log(doctorId)
  if (!token || !patientId) {
    alert("Missing session data, redirecting to appointments page.");
    window.location.href = "/pages/patientAppointments.html";
    return;
  }

  // get doctor to display only the available time of doctor
  getDoctors()
    .then(doctors => {
      // Find the doctor by the ID from the URL
      const doctor = doctors.find(d => d.id == doctorId);
      if (!doctor) {
        alert("Doctor not found.");
        return;
      }

      // Fill the form with the appointment data passed in the URL
      document.getElementById("patientName").value = patientName || "You";
      document.getElementById("doctorName").value = doctorName;
      document.getElementById("appointmentDate").value = appointmentDate;

      const timeSelect = document.getElementById("appointmentTime");
      (doctor.availableTimes || []).forEach(time => {
        const option = document.createElement("option");
        option.value = time;
        option.textContent = time;
        timeSelect.appendChild(option);
      });

      // The URL's appointmentTime comes from the backend's LocalTime
      // (Jackson serialises it as "HH:mm:ss", e.g. "09:00:00"), but each
      // <option> value is a range string from doctor.availableTimes
      // (e.g. "09:00-10:00"). A direct equality assignment never matches,
      // so the browser silently ignores it and the select falls back to
      // its first option -- pre-selecting the WRONG slot with no error.
      // Match on the "HH:mm" prefix (LocalTime always serialises with a
      // zero-padded 2-digit hour, so a 5-char slice is safe) to find the
      // range that starts at the booked time. Do not "simplify" this back
      // to `timeSelect.value = appointmentTime;`.
      if (appointmentTime) {
        const prefix = appointmentTime.slice(0, 5); // "09:00:00" -> "09:00"
        const matchedSlot = (doctor.availableTimes || []).find(t => t.startsWith(prefix));
        timeSelect.value = matchedSlot || appointmentTime;
      }

      // Handle form submission for updating the appointment
      document.getElementById("updateAppointmentForm").addEventListener("submit", async (e) => {
        e.preventDefault(); // Prevent default form submission

        const date = document.getElementById("appointmentDate").value;
        const time = document.getElementById("appointmentTime").value;
        const startTime = time.split('-')[0];
        if (!date || !time) {
          alert("Please select both date and time.");
          return;
        }

        const updatedAppointment = {
          id: appointmentId,
          doctor: { id: doctor.id },
          patient: { id: patientId },
          appointmentTime: `${date}T${startTime}:00`,
          status: 0
        };

        const updateResponse = await updateAppointment(updatedAppointment, token);

        if (updateResponse.success) {
          alert("Appointment updated successfully!");
          window.location.href = "/pages/patientAppointments.html"; // Redirect back to the appointments page
        } else {
          alert("❌ Failed to update appointment: " + updateResponse.message);
        }
      });
    })
    .catch(error => {
      console.error("Error fetching doctors:", error);
      alert("❌ Failed to load doctor data.");
    });
}
