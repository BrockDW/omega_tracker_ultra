// TaskDisplaySection.js
import React, { useState } from "react";

const TaskDisplaySection = ({ selectedDayTasks, isDayTasksLoading, selectedDate }) => {
  const [showCompleted, setShowCompleted] = useState(true);
  const [showIncomplete, setShowIncomplete] = useState(true);

  if (isDayTasksLoading) {
    return <p>Loading tasks...</p>;
  }

  if (!selectedDayTasks || selectedDayTasks.length === 0) {
    return <p>No tasks found for this day.</p>;
  }

  const completedTasks = selectedDayTasks.filter((task) => task.completed);
  const incompleteTasks = selectedDayTasks.filter((task) => !task.completed);

  return (
    <div style={{ marginTop: "1rem" }}>
      <h3>Tasks for {selectedDate}</h3>

      {/* Progress Bar */}
      <div style={{ marginBottom: "1rem" }}>
        <strong>Progress: </strong>
        <progress
          value={completedTasks.length}
          max={selectedDayTasks.length}
        />{" "}
        {((completedTasks.length / selectedDayTasks.length) * 100).toFixed(1)}%
      </div>

      {/* Collapsible Completed Tasks Section */}
      <div style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            marginBottom: "0.5rem",
          }}
          onClick={() => setShowCompleted(!showCompleted)}
        >
          <h4 style={{ margin: 0 }}>
            Completed Tasks ({completedTasks.length})
          </h4>
          <span style={{ marginLeft: "0.5rem" }}>
            {showCompleted ? "▲" : "▼"}
          </span>
        </div>
        {showCompleted && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "1rem" }}>
            {completedTasks.map((task, i) => (
              <div
                key={i}
                style={{
                  padding: "0.5rem",
                  border: "1px solid #82ca9d",
                  borderRadius: "4px",
                  backgroundColor: "#f0f9f0",
                }}
              >
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={{ marginRight: "0.5rem", color: "#82ca9d" }}>✔️</span>
                  <span>{task.description}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Collapsible Incomplete Tasks Section */}
      <div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            marginBottom: "0.5rem",
          }}
          onClick={() => setShowIncomplete(!showIncomplete)}
        >
          <h4 style={{ margin: 0 }}>
            Incomplete Tasks ({incompleteTasks.length})
          </h4>
          <span style={{ marginLeft: "0.5rem" }}>
            {showIncomplete ? "▲" : "▼"}
          </span>
        </div>
        {showIncomplete && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "1rem" }}>
            {incompleteTasks.map((task, i) => (
              <div
                key={i}
                style={{
                  padding: "0.5rem",
                  border: "1px solid #ff8b8b",
                  borderRadius: "4px",
                  backgroundColor: "#fff0f0",
                }}
              >
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={{ marginRight: "0.5rem", color: "#ff8b8b" }}>❌</span>
                  <span>{task.description}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TaskDisplaySection;