import React, { useEffect, useState } from 'react';
import { apiGet, apiPost } from "../util/apiClient";
import "./../css/TaskColumns.css";

const getDateRange = (option) => {
  const today = new Date();
  let startDate;

  switch (option) {
    case "this_week": {
      const day = today.getDay();
      startDate = new Date(today);
      startDate.setDate(today.getDate() - day);
      break;
    }
    case "this_month": {
      startDate = new Date(today.getFullYear(), today.getMonth(), 1);
      break;
    }
    case "this_year": {
      startDate = new Date(today.getFullYear(), 0, 1);
      break;
    }
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
  const [tasksMap, setTasksMap] = useState({
    dailyTasks: [],
    weeklyTasks: [],
    monthlyTasks: [],
  });
  const [rangeOption, setRangeOption] = useState("this_week");
  const [loading, setLoading] = useState(false);

  // NEW: track highlight states
  // key: task description; value: "red" or "green"
  const [highlightMap, setHighlightMap] = useState({});

  useEffect(() => {
    fetchIncompleteTasks(rangeOption);
  }, [rangeOption]);

  // Optionally, you could remove highlightMap whenever you re-fetch new tasks:
  // useEffect(() => { setHighlightMap({}); }, [tasksMap]);

  const fetchIncompleteTasks = async (option) => {
    const { start, end } = getDateRange(option);
    setLoading(true);

    try {
      const data = await apiGet(`/tasks/incomplete-aggregated?start=${start}&end=${end}`);

      // We assume the server already sorts by frequency if needed
      // Otherwise, do any custom sorting here

      setTasksMap({
        dailyTasks: data.dailyTasks,
        weeklyTasks: data.weeklyTasks,
        monthlyTasks: data.monthlyTasks,
      });

      // Optionally reset highlightMap to empty so old highlights disappear on new fetch
      setHighlightMap({});
    } catch (error) {
      console.error("Error fetching tasks:", error);
      setTasksMap({ dailyTasks: [], weeklyTasks: [], monthlyTasks: [] });
    } finally {
      setLoading(false);
    }
  };

  /**
   * Toggle a task's excluded status.
   * 1) Flip 'excluded' in local state
   * 2) POST to /tasks/exclusion
   * 3) If successful, highlight that row in red or green
   * 4) If fail, revert local state & revert highlight
   */
  const handleToggleExclusion = async (taskGroup, clickedTask) => {
    const newTasksMap = structuredClone(tasksMap);

    const tasksArray = newTasksMap[taskGroup];
    const foundIndex = tasksArray.findIndex((t) => t.description === clickedTask.description);
    if (foundIndex === -1) {
      console.warn("Could not find matching task:", clickedTask.description);
      return;
    }

    // Old state
    const wasExcluded = tasksArray[foundIndex].excluded;
    // Flip
    tasksArray[foundIndex].excluded = !wasExcluded;

    setTasksMap(newTasksMap);

    try {
      await apiPost("/tasks/exclusion", {
        description: clickedTask.description,
        excluded: !wasExcluded,
      });

      // If the server call succeeded, set highlight
      setHighlightMap((prev) => ({
        ...prev,
        [clickedTask.description]: wasExcluded ? "green" : "red"
        // If it WAS excluded, that means we are removing from excluded => highlight green
        // If it was NOT excluded, we are adding to excluded => highlight red
      }));
    } catch (error) {
      console.error("Error updating exclusion:", error);

      // Revert the local exclude state
      tasksArray[foundIndex].excluded = wasExcluded;
      setTasksMap(structuredClone(newTasksMap));

      // Revert highlight if we had set it
      setHighlightMap((prev) => {
        const copy = { ...prev };
        delete copy[clickedTask.description];
        return copy;
      });
    }
  };

  // Render each category in two columns
  const renderTwoColumnTasks = (title, tasks, groupKey) => {
    const notExcluded = tasks.filter((t) => !t.excluded);
    const excluded = tasks.filter((t) => t.excluded);

    if (tasks.length === 0) return null;

    return (
      <div style={{ marginBottom: "1rem" }}>
        <h4>{title}</h4>

        <div className="task-columns-container">
          {/* Column 1: Not Excluded */}
          <div className="task-column">
            <h5>Not Excluded</h5>
            <ul>
              {notExcluded.map((task, idx) => {
                // Determine if we apply a highlight class
                const highlightClass = highlightMap[task.description] === "green"
                  ? "highlight-green"
                  : highlightMap[task.description] === "red"
                  ? "highlight-red"
                  : "";

                return (
                  <li key={`${groupKey}-notExcluded-${idx}`} className={highlightClass}>
                    <label>
                      <input
                        type="checkbox"
                        checked={task.excluded}
                        onChange={() => handleToggleExclusion(groupKey, task)}
                      />
                      {task.description}
                      {task.frequency && <span> (freq: {task.frequency})</span>}
                    </label>
                  </li>
                );
              })}
            </ul>
          </div>

          {/* Column 2: Excluded */}
          <div className="task-column">
            <h5>Excluded</h5>
            <ul>
              {excluded.map((task, idx) => {
                // Determine if we apply a highlight class
                const highlightClass = highlightMap[task.description] === "red"
                  ? "highlight-red"
                  : highlightMap[task.description] === "green"
                  ? "highlight-green"
                  : "";

                return (
                  <li key={`${groupKey}-excluded-${idx}`} className={highlightClass}>
                    <label>
                      <input
                        type="checkbox"
                        checked={task.excluded}
                        onChange={() => handleToggleExclusion(groupKey, task)}
                      />
                      {task.description}
                      {task.frequency && <span> (freq: {task.frequency})</span>}
                    </label>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div style={{ padding: "1rem" }}>
      <h2>Incomplete Tasks</h2>

      <div style={{ marginBottom: "1rem" }}>
        <label>Select Date Range: </label>
        <select value={rangeOption} onChange={(e) => setRangeOption(e.target.value)}>
          <option value="this_week">This Week</option>
          <option value="this_month">This Month</option>
          <option value="this_year">This Year</option>
        </select>
      </div>

      {loading ? (
        <p>Loading incomplete tasks...</p>
      ) : (
        <>
          {renderTwoColumnTasks("Daily Tasks", tasksMap.dailyTasks, "dailyTasks")}
          {renderTwoColumnTasks("Weekly Tasks", tasksMap.weeklyTasks, "weeklyTasks")}
          {renderTwoColumnTasks("Monthly Tasks", tasksMap.monthlyTasks, "monthlyTasks")}

          {tasksMap.dailyTasks.length === 0 &&
           tasksMap.weeklyTasks.length === 0 &&
           tasksMap.monthlyTasks.length === 0 && (
             <p>No incomplete tasks for the selected period.</p>
          )}
        </>
      )}
    </div>
  );
}

export default IncompleteTasksPage;
