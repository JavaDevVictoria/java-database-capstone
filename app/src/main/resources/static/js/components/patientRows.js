// patientRows.js

// Escapes a value for safe interpolation into HTML text/attribute contexts.
// Display-only: never use this on values that feed navigation URLs.
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (ch) => {
    switch (ch) {
      case "&":
        return "&amp;";
      case "<":
        return "&lt;";
      case ">":
        return "&gt;";
      case '"':
        return "&quot;";
      case "'":
        return "&#39;";
      default:
        return ch;
    }
  });
}

export function createPatientRow(patient, appointmentId, doctorId) {
  const tr = document.createElement("tr");
  console.log("CreatePatientRow :: ", doctorId)
  const displayName = patient.patientName || "this patient";
  const safeId = escapeHtml(patient.patientId);
  const safeName = escapeHtml(patient.patientName);
  const safePhone = escapeHtml(patient.patientPhone);
  const safeEmail = escapeHtml(patient.patientEmail);
  const safeDisplayNameAttr = escapeHtml(displayName);
  tr.innerHTML = `
      <td><button type="button" class="patient-id-btn" data-id="${safeId}" aria-label="View record for ${safeDisplayNameAttr}">${safeId}</button></td>
      <td>${safeName}</td>
      <td>${safePhone}</td>
      <td>${safeEmail}</td>
      <td>
        <button type="button" class="prescription-btn" data-id="${safeId}" aria-label="Add prescription for ${safeDisplayNameAttr}">
          <img src="../assets/images/addPrescriptionIcon/addPrescription.png" alt="" />
        </button>
      </td>
    `;

  // Attach event listeners
  tr.querySelector(".patient-id-btn").addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${patient.patientId}&doctorId=${doctorId}`;
  });

  tr.querySelector(".prescription-btn").addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointmentId}&patientName=${patient.patientName}`;
  });

  return tr;
}
