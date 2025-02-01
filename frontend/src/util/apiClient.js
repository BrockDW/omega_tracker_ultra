import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE_URL = process.env.REACT_APP_API_URL; 
// e.g. "http://localhost:8080" or "http://your-server.com"

// Generalized API functions
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

// Generalized WebSocket client
export function createWebSocketClient(onConnect, onError) {
  const sockJsUrl = `${BASE_URL}/ws`; // Use http/https URL for SockJS
  const client = new Client({
    webSocketFactory: () => new SockJS(sockJsUrl), // Use SockJS with http/https URL
    reconnectDelay: 5000, // Reconnect after 5 seconds if disconnected
    onConnect: () => {
      console.log('WebSocket connected');
      if (onConnect) onConnect(client);
    },
    onStompError: (frame) => {
      console.error('WebSocket error:', frame);
      if (onError) onError(frame);
    },
  });

  return client;
}