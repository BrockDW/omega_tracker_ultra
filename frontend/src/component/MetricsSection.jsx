import React, { useState } from "react";

const MetricsSection = ({
  practiceMetrics,
  weightMetrics,
  leetcodeMetrics,
  isDayPracticeLoading,
  isDayWeightLoading,
  isDayLeetCodeLoading,
  handleForcePractice,
}) => {
  const [showPractice, setShowPractice] = useState(true);
  const [showWeight, setShowWeight] = useState(true);
  const [showLeetCode, setShowLeetCode] = useState(true);

  return (
    <div style={{ marginTop: "1rem" }}>
      {/* Practice Metrics */}
      <div style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            marginBottom: "0.5rem",
          }}
          onClick={() => setShowPractice(!showPractice)}
        >
          <h4 style={{ margin: 0 }}>Practice Metrics</h4>
          <span style={{ marginLeft: "0.5rem" }}>{showPractice ? "▲" : "▼"}</span>
        </div>
        {showPractice && (
          <div
            style={{
              padding: "1rem",
              border: "1px solid #8884d8",
              borderRadius: "4px",
              backgroundColor: "#f0f0ff",
            }}
          >
            {isDayPracticeLoading ? (
              <p>Loading practice metrics...</p>
            ) : practiceMetrics ? (
              <>
                <div style={{ marginBottom: "0.5rem" }}>
                  <strong>Progress: </strong>
                  <progress value={practiceMetrics.percentage} max={100} />{" "}
                  {practiceMetrics.percentage}%
                </div>
                <p>
                  Total Minutes: <strong>{practiceMetrics.totalMinutes}</strong>
                  <br />
                  Minutes Practiced:{" "}
                  <strong>{practiceMetrics.minutesPracticed}</strong>
                </p>
                <button
                  onClick={handleForcePractice}
                  style={{
                    marginTop: "0.5rem",
                    padding: "0.5rem 1rem",
                    backgroundColor: "#8884d8",
                    color: "white",
                    border: "none",
                    borderRadius: "4px",
                    cursor: "pointer",
                  }}
                >
                  Force Keybr (isForced=true)
                </button>
              </>
            ) : (
              <p>No practice metrics for this day.</p>
            )}
          </div>
        )}
      </div>

      {/* Weight Metrics */}
      <div style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            marginBottom: "0.5rem",
          }}
          onClick={() => setShowWeight(!showWeight)}
        >
          <h4 style={{ margin: 0 }}>Weight Metrics</h4>
          <span style={{ marginLeft: "0.5rem" }}>{showWeight ? "▲" : "▼"}</span>
        </div>
        {showWeight && (
          <div
            style={{
              padding: "1rem",
              border: "1px solid #82ca9d",
              borderRadius: "4px",
              backgroundColor: "#f0f9f0",
            }}
          >
            {isDayWeightLoading ? (
              <p>Loading weight metrics...</p>
            ) : weightMetrics ? (
              <>
                <div style={{ marginBottom: "0.5rem" }}>
                  <strong>Progress: </strong>
                  <progress value={weightMetrics.percentage} max={100} />{" "}
                  {weightMetrics.percentage.toFixed(2)}%
                </div>
                <p>
                  Goal Seconds: <strong>{weightMetrics.goalSeconds}</strong>
                  <br />
                  Seconds Practiced:{" "}
                  <strong>{weightMetrics.secondsPracticed}</strong>
                </p>
              </>
            ) : (
              <p>No weight metrics for this day.</p>
            )}
          </div>
        )}
      </div>

      {/* LeetCode Metrics */}
      <div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            marginBottom: "0.5rem",
          }}
          onClick={() => setShowLeetCode(!showLeetCode)}
        >
          <h4 style={{ margin: 0 }}>LeetCode Metrics</h4>
          <span style={{ marginLeft: "0.5rem" }}>{showLeetCode ? "▲" : "▼"}</span>
        </div>
        {showLeetCode && (
          <div
            style={{
              padding: "1rem",
              border: "1px solid #ff8b8b",
              borderRadius: "4px",
              backgroundColor: "#fff0f0",
            }}
          >
            {isDayLeetCodeLoading ? (
              <p>Loading LeetCode metrics...</p>
            ) : leetcodeMetrics ? (
              <>
                <div style={{ marginBottom: "0.5rem" }}>
                  <strong>Progress: </strong>
                  <progress value={leetcodeMetrics.percentage} max={100} />{" "}
                  {leetcodeMetrics.percentage}%
                </div>
                <p>
                  Goal Questions:{" "}
                  <strong>{leetcodeMetrics.goalQuestion}</strong>
                  <br />
                  Questions Finished:{" "}
                  <strong>{leetcodeMetrics.questionFinished}</strong>
                </p>
              </>
            ) : (
              <p>No LeetCode metrics for this day.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default MetricsSection;