import React, { useEffect, useState } from 'react';
import { apiGet, apiPost } from "../util/apiClient"; // or wherever you keep them
import "./../css/TaskColumns.css"

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
  const [tasksMap, setTasksMap] = useState({
    dailyTasks: [],
    weeklyTasks: [],
    monthlyTasks: [],
  });
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

      // Sort tasks alphabetically by description
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

  /**
   * Toggle a task's excluded status.
   * 1) Locally flip its 'excluded' boolean so UI updates immediately
   * 2) POST to /tasks/exclusion so the server updates task_exclusion_list.md
   */
  const handleToggleExclusion = async (taskGroup, clickedTask) => {
    // 1) Make a clone of the entire tasksMap
    const newTasksMap = structuredClone(tasksMap);

    // 2) Find the matching task in newTasksMap[taskGroup]
    //    e.g. by matching the description
    const tasksArray = newTasksMap[taskGroup];
    const foundIndex = tasksArray.findIndex(
      (t) => t.description === clickedTask.description
    );
    if (foundIndex === -1) {
      console.warn("Could not find matching task:", clickedTask.description);
      return;
    }

    // Flip the excluded value
    tasksArray[foundIndex].excluded = !tasksArray[foundIndex].excluded;

    // 3) Update state so we immediately see the columns change
    setTasksMap(newTasksMap);

    try {
      // 4) POST to the server to update the local exclusion file
      await apiPost("/tasks/exclusion", {
        description: clickedTask.description,
        excluded: tasksArray[foundIndex].excluded,
      });
    } catch (error) {
      console.error("Error updating exclusion:", error);

      // Optional: revert the change if the request failed
      tasksArray[foundIndex].excluded = !tasksArray[foundIndex].excluded;
      setTasksMap(structuredClone(newTasksMap));
    }
  };


  // Helper to display tasks in two columns: Not Excluded / Excluded
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
              {notExcluded.map((task, idx) => (
                <li key={`${groupKey}-notExcluded-${idx}`}>
                  <label>
                    <input
                      type="checkbox"
                      checked={task.excluded}
                      onChange={() => handleToggleExclusion(groupKey, task)}
                    />
                    {task.description}
                  </label>
                </li>
              ))}
            </ul>
          </div>

          {/* Column 2: Excluded */}
          <div className="task-column">
            <h5>Excluded</h5>
            <ul>
              {excluded.map((task, idx) => (
                <li key={`${groupKey}-excluded-${idx}`}>
                  <label>
                    <input
                      type="checkbox"
                      checked={task.excluded}
                      onChange={() => handleToggleExclusion(groupKey, task)}
                    />
                    {task.description}
                  </label>
                </li>
              ))}
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
