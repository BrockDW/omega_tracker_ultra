import React, { useEffect, useState } from 'react';
import TaskChartPage from './component/TaskChartPage';

function App() {
  const [message, setMessage] = useState('');

  useEffect(() => {
    const baseUrl = process.env.REACT_APP_API_URL;
    fetch(`${baseUrl}/hello`)
      .then((res) => res.text())
      .then((text) => setMessage(text))
      .catch((err) => console.error(err));

    fetch(`${baseUrl}/tasks/today`)
      .then(response => response.json())
      .then(data => {
        console.log("triggered");
        console.log('Today’s tasks:', data);
      })
      .catch(error => {
        console.error('Error fetching tasks:', error);
      });
  }, []);

  return (
    <div style={{ padding: '2rem', fontFamily: 'Arial, sans-serif' }}>
      <TaskChartPage />
    </div>
  );
}

export default App;
