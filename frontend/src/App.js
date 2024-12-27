import React, { useEffect, useState } from 'react';

function App() {
  const [message, setMessage] = useState('');

  useEffect(() => {
    const baseUrl = process.env.REACT_APP_API_URL;
    fetch(`${baseUrl}/api/hello`)
      .then((res) => res.text())
      .then((text) => setMessage(text))
      .catch((err) => console.error(err));

    fetch(`${baseUrl}/api/tasks/today`)
      .then(response => response.json())
      .then(data => {
        console.log('Today’s tasks:', data);
      })
      .catch(error => {
        console.error('Error fetching tasks:', error);
      });
  }, []);

  return (
    <div style={{ padding: '2rem', fontFamily: 'Arial, sans-serif' }}>
      <h1>Hello from React!</h1>
      <p>Backend says: {message}</p>
    </div>
  );
}

export default App;
