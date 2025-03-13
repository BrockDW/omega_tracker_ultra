import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import TaskChartPage from './component/TaskChartPage';
import IncompleteTasksPage from './component/IncompleteTasksPage';

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
    <Router>
      <div style={{ padding: '2rem', fontFamily: 'Arial, sans-serif' }}>
        <nav style={{ marginBottom: '1rem' }}>
          <Link to="/" style={{ marginRight: '1rem' }}>Home</Link>
          <Link to="/incomplete-tasks">Incomplete Tasks</Link>
        </nav>

        <Routes>
          <Route path="/" element={<TaskChartPage />} />
          <Route path="/incomplete-tasks" element={<IncompleteTasksPage />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;