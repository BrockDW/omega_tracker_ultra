import React, { useEffect, useState } from 'react';
import { apiGet } from "../util/apiClient";

const getDateRange = (option) => {
  const today = new Date();
  let startDate;

  switch (option) {
    case "this_week":
      const day = today.getDay();
      startDate = new Date(today);
      startDate.setDate(today.getDate() - day);
      break;
    case "this_month":
      startDate = new Date(today.getFullYear(), today.getMonth(), 1);
      break;
    case "this_year":
      startDate = new Date(today.getFullYear(), 0, 1);
      break;
    default:
      startDate = new Date(today);
      break;
  }

  return {
    start: startDate.toISOString().split("T")[0],
    end: today.toISOString().split("T")[0],
  };
};

function IncompleteTasksPage() {
  const [tasksMap, setTasksMap] = useState({ dailyTasks: [], weeklyTasks: [], monthlyTasks: [] });
  const [rangeOption, setRangeOption] = useState("this_week");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchIncompleteTasks(rangeOption);
  }, [rangeOption]);

  const fetchIncompleteTasks = async (option) => {
    const { start, end } = getDateRange(option);
    setLoading(true);

    try {
      const data = await apiGet(`/tasks/incomplete-aggregated?start=${start}&end=${end}`);

      // Sort tasks alphabetically
      const sortTasks = (tasks) => tasks.sort((a, b) => a.description.localeCompare(b.description));

      setTasksMap({
        dailyTasks: sortTasks(data.dailyTasks),
        weeklyTasks: sortTasks(data.weeklyTasks),
        monthlyTasks: sortTasks(data.monthlyTasks),
      });
    } catch (error) {
      console.error("Error fetching tasks:", error);
      setTasksMap({ dailyTasks: [], weeklyTasks: [], monthlyTasks: [] });
    } finally {
      setLoading(false);
    }
  };

  const renderTasks = (title, tasks) => (
    tasks.length > 0 && (
      <div style={{ marginBottom: "1rem" }}>
        <h4>{title}</h4>
        <ul>
          {tasks.map((task, index) => (
            <li key={index}>{task.description}</li>
          ))}
        </ul>
      </div>
    )
  );

  return (
    <div style={{ padding: "1rem" }}>
      <h2>Incomplete Tasks</h2>

      <div style={{ marginBottom: "1rem" }}>
        <label>Select Date Range: </label>
        <select
          value={rangeOption}
          onChange={(e) => setRangeOption(e.target.value)}
        >
          <option value="this_week">This Week</option>
          <option value="this_month">This Month</option>
          <option value="this_year">This Year</option>
        </select>
      </div>

      {loading ? (
        <p>Loading incomplete tasks...</p>
      ) : (
        <>
          {renderTasks("Daily Tasks", tasksMap.dailyTasks)}
          {renderTasks("Weekly Tasks", tasksMap.weeklyTasks)}
          {renderTasks("Monthly Tasks", tasksMap.monthlyTasks)}
          {(tasksMap.dailyTasks.length === 0 && tasksMap.weeklyTasks.length === 0 && tasksMap.monthlyTasks.length === 0) && (
            <p>No incomplete tasks for the selected period.</p>
          )}
        </>
      )}
    </div>
  );
}

export default IncompleteTasksPage;