// apiClient.js

const BASE_URL = process.env.REACT_APP_API_URL; 
// e.g. "http://localhost:8080" or "http://your-server.com"

export async function apiGet(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
      // add other headers or auth tokens if needed
    }
  });
  if (!response.ok) {
    throw new Error(`GET ${path} failed: ${response.status}`);
  }
  return response.json();
}

export async function apiPost(path, bodyData) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
      // add auth if needed
    },
    body: JSON.stringify(bodyData)
  });
  if (!response.ok) {
    throw new Error(`POST ${path} failed: ${response.status}`);
  }
  return response.json();
}

// ...and similarly for PUT, DELETE, etc.
