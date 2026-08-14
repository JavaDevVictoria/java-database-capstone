// modals.js

// The element that had focus right before the modal opened. Restored on close.
let previouslyFocusedElement = null;

// Sibling elements (of #modal) that were made inert/hidden from assistive tech
// while the modal is open. Restored on close.
let inertSiblings = [];

function getModalEl() {
  return document.getElementById('modal');
}

function getModalBodyEl() {
  return document.getElementById('modal-body');
}

function isModalOpen() {
  const modal = getModalEl();
  return !!modal && modal.style.display === 'block';
}

function modalContains(el) {
  const modal = getModalEl();
  return !!modal && !!el && modal.contains(el);
}

// Returns the focusable descendants of a given container, in DOM order,
// skipping anything currently hidden (offsetParent is null while display:none
// or while an ancestor is hidden).
function getFocusableElements(container) {
  if (!container) return [];
  const selector = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])'
  ].join(',');
  return Array.from(container.querySelectorAll(selector)).filter(
    (el) => el.offsetParent !== null
  );
}

// Hides everything outside the modal from assistive tech and (as a fallback,
// alongside the focus trap below) from the keyboard while it is open.
//
// This walks the modal's own siblings via its parentElement rather than a
// hardcoded selector like ".container", because modals.js is shared across
// pages with different DOM structures (index.html nests #modal inside
// .wrapper next to the header/footer; adminDashboard.html and
// patientDashboard.html put #modal as a sibling of .container at the body
// level). Working relative to #modal's actual parent degrades safely on all
// of them and never touches the modal itself.
function setBackgroundInert(hidden) {
  const modal = getModalEl();
  if (!modal || !modal.parentElement) return;

  if (hidden) {
    inertSiblings = Array.from(modal.parentElement.children).filter(
      (el) => el !== modal
    );
    inertSiblings.forEach((el) => {
      el.setAttribute('aria-hidden', 'true');
      el.setAttribute('inert', '');
    });
  } else {
    inertSiblings.forEach((el) => {
      el.removeAttribute('aria-hidden');
      el.removeAttribute('inert');
    });
    inertSiblings = [];
  }
}

// Single keydown listener attached once at module load. It no-ops whenever
// the modal isn't open, so openModal()/closeModal() never add or remove this
// listener themselves - it can't be duplicated by repeated opens and there is
// nothing to leak or forget to clean up.
document.addEventListener('keydown', (e) => {
  if (!isModalOpen()) return;

  if (e.key === 'Escape') {
    e.preventDefault();
    closeModal();
    return;
  }

  if (e.key === 'Tab') {
    const modal = getModalEl();
    const focusable = getFocusableElements(modal);

    if (focusable.length === 0) {
      // Nothing to tab to inside the modal - keep focus from escaping it.
      e.preventDefault();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = document.activeElement;

    if (e.shiftKey) {
      if (active === first || !modalContains(active)) {
        e.preventDefault();
        last.focus();
      }
    } else if (active === last || !modalContains(active)) {
      e.preventDefault();
      first.focus();
    }
  }
});

export function openModal(type) {
  let modalContent = '';
  if (type === 'addDoctor') {
    modalContent = `
         <h2 id="modalTitle">Add Doctor</h2>
         <input type="text" id="doctorName" placeholder="Doctor Name" class="input-field">
         <select id="specialization" class="input-field select-dropdown">
             <option value="">Specialization</option>
                        <option value="cardiologist">Cardiologist</option>
                        <option value="dermatologist">Dermatologist</option>
                        <option value="neurologist">Neurologist</option>
                        <option value="pediatrician">Pediatrician</option>
                        <option value="orthopedic">Orthopedic</option>
                        <option value="gynecologist">Gynecologist</option>
                        <option value="psychiatrist">Psychiatrist</option>
                        <option value="dentist">Dentist</option>
                        <option value="ophthalmologist">Ophthalmologist</option>
                        <option value="ent">ENT Specialist</option>
                        <option value="urologist">Urologist</option>
                        <option value="oncologist">Oncologist</option>
                        <option value="gastroenterologist">Gastroenterologist</option>
                        <option value="general">General Physician</option>

        </select>
        <input type="email" id="doctorEmail" placeholder="Email" class="input-field">
        <input type="password" id="doctorPassword" placeholder="Password" class="input-field">
        <input type="text" id="doctorPhone" placeholder="Mobile No." class="input-field">
        <div class="availability-container">
        <label class="availabilityLabel">Select Availability:</label>
          <div class="checkbox-group">
              <label><input type="checkbox" name="availability" value="09:00-10:00"> 9:00 AM - 10:00 AM</label>
              <label><input type="checkbox" name="availability" value="10:00-11:00"> 10:00 AM - 11:00 AM</label>
              <label><input type="checkbox" name="availability" value="11:00-12:00"> 11:00 AM - 12:00 PM</label>
              <label><input type="checkbox" name="availability" value="12:00-13:00"> 12:00 PM - 1:00 PM</label>
          </div>
        </div>
        <button class="dashboard-btn" id="saveDoctorBtn">Save</button>
      `;
  } else if (type === 'patientLogin') {
    modalContent = `
        <h2 id="modalTitle">Patient Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="loginBtn">Login</button>
      `;
  }
  else if (type === "patientSignup") {
    modalContent = `
      <h2 id="modalTitle">Patient Signup</h2>
      <input type="text" id="name" placeholder="Name" class="input-field">
      <input type="email" id="email" placeholder="Email" class="input-field">
      <input type="password" id="password" placeholder="Password" class="input-field">
      <input type="text" id="phone" placeholder="Phone" class="input-field">
      <input type="text" id="address" placeholder="Address" class="input-field">
      <button class="dashboard-btn" id="signupBtn">Signup</button>
    `;

  } else if (type === 'adminLogin') {
    modalContent = `
        <h2 id="modalTitle">Admin Login</h2>
        <input type="text" id="username" name="username" placeholder="Username" class="input-field">
        <input type="password" id="password" name="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="adminLoginBtn" >Login</button>
      `;
  } else if (type === 'doctorLogin') {
    modalContent = `
        <h2 id="modalTitle">Doctor Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="doctorLoginBtn" >Login</button>
      `;
  }

  const modal = getModalEl();
  const modalBody = getModalBodyEl();
  if (modalBody) modalBody.innerHTML = modalContent;

  // Remember what had focus so it can be restored when the modal closes.
  // Only capture this if a modal isn't already open - otherwise a second
  // openModal() call (before the first was closed) would overwrite it with
  // an element inside the modal that's about to be replaced, losing the
  // original trigger and breaking focus restoration on close.
  if (!isModalOpen()) {
    previouslyFocusedElement = document.activeElement;
  }

  if (modal) modal.style.display = 'block';

  // Hide the rest of the page from assistive tech / keyboard while the modal is open.
  setBackgroundInert(true);

  const closeBtn = document.getElementById('closeModal');
  if (closeBtn) {
    closeBtn.onclick = closeModal;
  }

  if (type === "patientSignup") {
    const signupBtn = document.getElementById("signupBtn");
    if (signupBtn) signupBtn.addEventListener("click", signupPatient);
  }

  if (type === "patientLogin") {
    const loginBtn = document.getElementById("loginBtn");
    if (loginBtn) loginBtn.addEventListener("click", loginPatient);
  }

  if (type === 'addDoctor') {
    const saveDoctorBtn = document.getElementById('saveDoctorBtn');
    if (saveDoctorBtn) saveDoctorBtn.addEventListener('click', adminAddDoctor);
  }

  if (type === 'adminLogin') {
    const adminLoginBtn = document.getElementById('adminLoginBtn');
    if (adminLoginBtn) adminLoginBtn.addEventListener('click', adminLoginHandler);
  }

  if (type === 'doctorLogin') {
    const doctorLoginBtn = document.getElementById('doctorLoginBtn');
    if (doctorLoginBtn) doctorLoginBtn.addEventListener('click', doctorLoginHandler);
  }

  // Move focus into the dialog: first focusable field inside #modal-body,
  // falling back to the close button if the body has nothing focusable.
  const bodyFocusable = getFocusableElements(modalBody);
  if (bodyFocusable.length > 0) {
    bodyFocusable[0].focus();
  } else if (closeBtn) {
    closeBtn.focus();
  }
}

export function closeModal() {
  const modal = getModalEl();
  const modalBody = getModalBodyEl();

  if (modal) modal.style.display = 'none';
  if (modalBody) modalBody.innerHTML = ''; // Clears the inputs

  // Reveal the rest of the page to assistive tech / keyboard again.
  setBackgroundInert(false);

  // Restore focus to whatever triggered the modal, if it still exists in the DOM.
  if (previouslyFocusedElement && document.body.contains(previouslyFocusedElement)) {
    previouslyFocusedElement.focus();
  }
  previouslyFocusedElement = null;
}

// header.js is loaded as a classic (non-module) script on every page and
// calls openModal(...)/closeModal() as bare globals - both via inline
// onclick="openModal(...)" attributes and from addEventListener callbacks.
// ES module exports are not placed on `window`, so those calls would
// otherwise throw ReferenceError. Bridge them onto window in addition to
// the existing exports, matching the convention already used elsewhere in
// this codebase (window.adminLoginHandler, window.doctorLoginHandler,
// window.signupPatient, window.loginPatient) for the same reason.
window.openModal = openModal;
window.closeModal = closeModal;
