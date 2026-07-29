import { API_BASE_URL } from '../config/config.js';

const DOCTOR_API = API_BASE_URL + '/doctor'

async function getDoctors() {
    try {
        const response = await fetch(DOCTOR_API, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });

        // 1. Always check if the server returned a success status (200-299)
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        // 2. Parse the body as JSON data
        const doctors = await response.json();
        
        // 3. Return the data so you can use it outside this function
        return doctors;
    }
    catch (error) {
        // 4. Handle errors (network failures, 404s, 500s, etc.)
        console.error("Failed to fetch doctors:", error);
    }
}

async function deleteDoctor(id, token) {
    try {
        // 1. Append the ID directly to the API endpoint URL
        const response = await fetch(`${DOCTOR_API}/${id}`, {
            method: 'DELETE',
            headers: { 
                'Content-Type': 'application/json',
                // 2. Pass the bearer token for authentication
                'Authorization': `Bearer ${token}` 
            },
        });

        // 3. Check for HTTP errors (e.g., 401 Unauthorized or 404 Not Found)
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        // 4. Handle the response (DELETE often returns a 204 No Content, check if body exists)
        const isJson = response.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await response.json() : { success: true };
        
        return data;
    }
    catch (error) {
        console.error(`Failed to delete doctor with ID ${id}:`, error);
    }
}

async function saveDoctor(doctor, token) {
    try {
        const response = await fetch(DOCTOR_API, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            // 1. Convert your JavaScript object into a JSON string
            body: JSON.stringify(doctor)
        });

        // 2. Parse the backend response (usually contains the new item or a message)
        const responseData = await response.json();

        // 3. Check if the server rejected the creation (e.g., 400 Bad Request, 409 Conflict)
        if (!response.ok) {
            return {
                success: false,
                message: responseData.message || `Server error: ${response.status}`
            };
        }

        // 4. Return your standard success structure
        return {
            success: true,
            message: 'Doctor saved successfully!',
            data: responseData
        };

    } catch (error) {
        // 5. Return the same structure for network/runtime errors
        return {
            success: false,
            message: error.message || 'A network error occurred. Please try again.'
        };
    }
}

async function filterDoctors(name, time, specialty) {
    try {
        // If a variable is null/undefined, it falls back to an empty string ''
        const safeName = name ?? '';
        const safeTime = time ?? '';
        const safeSpecialty = specialty ?? '';

        // Note: encodeURIComponent safely handles spaces and symbols
        const url = `${DOCTOR_API}?name=${encodeURIComponent(safeName)}&time=${encodeURIComponent(safeTime)}&specialty=${encodeURIComponent(safeSpecialty)}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        return await response.json();
    }
    catch (error) {
        console.error("Failed to fetch doctors:", error);
    }
}



/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/
