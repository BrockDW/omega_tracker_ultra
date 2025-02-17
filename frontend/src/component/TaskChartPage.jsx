import React, { useState, useEffect } from "react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ComposedChart
} from "recharts";
import { apiGet } from "../util/apiClient";
import { createWebSocketClient } from "../util/apiClient";
import TaskDisplaySection from "./TaskDisplaySection";
import MetricsSection from "./MetricsSection"; // Import the new component

const TASKS_WEIGHT = 0.4;
const PRACTICE_WEIGHT = 0.2;
const WEIGHT_WEIGHT = 0.1;
const LEETCODE_WEIGHT = 0.3;

function TaskChartPage() {
  // ---------------------------
  // 1) Range selection
  // ---------------------------
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // ---------------------------
  // 2) Data maps
  // ---------------------------
  const [tasksDayMap, setTasksDayMap] = useState({});
  const [practiceRangeMap, setPracticeRangeMap] = useState({});
  const [weightRangeMap, setWeightRangeMap] = useState({});
  const [leetcodeRangeMap, setLeetCodeRangeMap] = useState({});

  // ---------------------------
  // 3) Final chart data
  // ---------------------------
  const [chartData, setChartData] = useState([]);

  // ---------------------------
  // Day-level details
  // ---------------------------
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedDayTasks, setSelectedDayTasks] = useState([]);
  const [practiceMetrics, setPracticeMetrics] = useState(null);
  const [weightMetrics, setWeightMetrics] = useState(null);
  const [leetcodeMetrics, setLeetCodeMetrics] = useState(null);

  const [isDayTasksLoading, setIsDayTasksLoading] = useState(false);
  const [isDayPracticeLoading, setIsDayPracticeLoading] = useState(false);
  const [isDayWeightLoading, setIsDayWeightLoading] = useState(false);
  const [isDayLeetCodeLoading, setIsDayLeetCodeLoading] = useState(false);

  // WebSocket sensor data
  const [sensorData, setSensorData] = useState({ rawValue: null, weight: null });

  // ---------------------------
  // 4) On mount, set [startDate, endDate] to [today-7, today]
  // ---------------------------
  useEffect(() => {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];

    const minus7 = new Date();
    minus7.setDate(minus7.getDate() - 7);
    const minus7Str = minus7.toISOString().split("T")[0];

    setEndDate(todayStr);
    setStartDate(minus7Str);
  }, []);

  // ---------------------------
  // 5) Fetch tasks/practice/weight whenever date changes
  // ---------------------------
  useEffect(() => {
    if (!startDate || !endDate) return;
    fetchTasksRange();
    fetchPracticeRange();
    fetchWeightRange();
    fetchLeetCodeRange();
  }, [startDate, endDate]);

  // ---------------------------
  // 6) Merge data into chartData
  // ---------------------------
  useEffect(() => {
    const combined = transformDayMapToChartData(tasksDayMap, practiceRangeMap, weightRangeMap, leetcodeRangeMap);
    setChartData(combined);
  }, [tasksDayMap, practiceRangeMap, weightRangeMap, leetcodeRangeMap]);

  // ---------------------------
  // 7) Fetch tasks range
  // ---------------------------
  async function fetchTasksRange() {
    try {
      const dayMap = await apiGet(`/tasks/range?start=${startDate}&end=${endDate}`);
      setTasksDayMap(dayMap);
    } catch (err) {
      console.error("Failed to fetch tasks range:", err);
      setTasksDayMap({});
    }
  }

  // ---------------------------
  // 8) Fetch practice range
  // ---------------------------
  async function fetchPracticeRange() {
    try {
      const data = await apiGet(`/keybr/range?start=${startDate}&end=${endDate}`);
      setPracticeRangeMap(data);
    } catch (err) {
      console.error("Failed to fetch practice range:", err);
      setPracticeRangeMap({});
    }
  }

  // ---------------------------
  // Fetch weight range
  // ---------------------------
  async function fetchWeightRange() {
    try {
      const data = await apiGet(`/weight/range?start=${startDate}&end=${endDate}`);
      setWeightRangeMap(data);
    } catch (err) {
      console.error("Failed to fetch weight range:", err);
      setWeightRangeMap({});
    }
  }

  // ---------------------------
  // Fetch LeetCode range
  // ---------------------------
  async function fetchLeetCodeRange() {
    try {
      const data = await apiGet(`/leetcode/range?start=${startDate}&end=${endDate}`);
      setLeetCodeRangeMap(data);
    } catch (err) {
      console.error("Failed to fetch LeetCode range:", err);
      setLeetCodeRangeMap({});
    }
  }

  // ---------------------------
  // 9) Merge day-level data
  // ---------------------------
  function transformDayMapToChartData(tasksMap, practiceMap, weightMap, leetcodeMap) {
    // gather all dates from each map
    const allDates = new Set([
      ...Object.keys(tasksMap),
      ...Object.keys(practiceMap),
      ...Object.keys(weightMap),
      ...Object.keys(leetcodeMap),
    ]);

    // define a helper to clamp percentages
    const clampTo100 = (value) => Math.min(value, 100);

    const results = [...allDates].map((date) => {
      // 1) tasks
      const tasks = tasksMap[date] || [];
      const completedCount = tasks.filter((t) => t.completed).length;
      const totalTasks = tasks.length;
      const tasksPercent = totalTasks > 0 ? (completedCount / totalTasks) * 100 : 0;

      // 2) practice
      const practice = practiceMap[date] || {};
      const practicePercent = practice.percentage || 0;

      // 3) weight
      const weight = weightMap[date] || {};
      const weightPercent = weight.percentage || 0;

      // 4) leetcode
      const leetcode = leetcodeMap[date] || {};
      const leetcodePercent = leetcode.percentage || 0;

      console.log("leetcode percentage is here: ", leetcode)

      // clamp each
      const tasksClamped = clampTo100(tasksPercent);
      const practiceClamped = clampTo100(practicePercent);
      const weightClamped = clampTo100(weightPercent);
      const leetcodeClamped = clampTo100(leetcodePercent);

      // Weighted overall
      const overall =
        tasksClamped * TASKS_WEIGHT +
        practiceClamped * PRACTICE_WEIGHT +
        weightClamped * WEIGHT_WEIGHT +
        leetcodeClamped * LEETCODE_WEIGHT;

      // also clamp overall if you want to ensure it never exceeds 100
      const overallClamped = clampTo100(overall);

      return {
        date,
        completed: completedCount,
        notCompleted: totalTasks - completedCount,
        // for the line chart:
        percent: Math.round(overallClamped),
        leetcodePercent: Math.round(leetcodeClamped),
      };
    });

    // sort chronologically
    results.sort((a, b) => (a.date > b.date ? 1 : -1));
    return results;
  }

  // ---------------------------
  // 10) Day-level click
  // ---------------------------
  const handleChartClick = async (data) => {
    if (data && data.activePayload && data.activePayload.length > 0) {
      const { date } = data.activePayload[0].payload;
      console.log("Clicked date:", date);

      setSelectedDate(date);
      setSelectedDayTasks([]);
      setPracticeMetrics(null);
      setWeightMetrics(null);
      setLeetCodeMetrics(null);

      setIsDayTasksLoading(true);
      setIsDayPracticeLoading(true);
      setIsDayWeightLoading(true);
      setIsDayLeetCodeLoading(true);

      // 10A) tasks detail
      try {
        const dayTasks = await apiGet(`/tasks/day/${date}`);
        setSelectedDayTasks(dayTasks);
      } catch (err) {
        console.error("Failed to fetch day tasks:", err);
      } finally {
        setIsDayTasksLoading(false);
      }

      // 10B) weight detail
      try {
        const weightDay = await apiGet(`/weight/day/${date}`);
        setWeightMetrics(weightDay);
      } catch (err) {
        console.error("Failed to fetch weight detail:", err);
        setWeightMetrics(null);
      } finally {
        setIsDayWeightLoading(false);
      }

      // 10C) practice detail (default isForced=false on backend)
      try {
        const practiceDay = await apiGet(`/keybr/day/${date}`);
        setPracticeMetrics(practiceDay);
      } catch (err) {
        console.error("Failed to fetch practice detail:", err);
      } finally {
        setIsDayPracticeLoading(false);
      }

      // 10D) leetcode detail
      try {
        const leetcodeDay = await apiGet(`/leetcode/day/${date}`);
        setLeetCodeMetrics(leetcodeDay);
      } catch (err) {
        console.error("Failed to fetch LeetCode detail:", err);
      } finally {
        setIsDayLeetCodeLoading(false);
      }
    }
  };

  // ---------------------------
  // 3) (NEW) Force practice detail
  // ---------------------------
  const handleForcePractice = async () => {
    if (!selectedDate) return;
    try {
      setIsDayPracticeLoading(true);
      // Call the same endpoint, but add isForced=true
      const practiceDay = await apiGet(`/keybr/day/${selectedDate}?isForced=true`);
      setPracticeMetrics(practiceDay);
    } catch (err) {
      console.error("Failed to fetch practice detail with force:", err);
    } finally {
      setIsDayPracticeLoading(false);
    }
  };

  // ---------------------------
  // 11) Reset the date range
  // ---------------------------
  const handleReset = () => {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];

    const minus7 = new Date();
    minus7.setDate(minus7.getDate() - 7);
    const minus7Str = minus7.toISOString().split("T")[0];

    setEndDate(todayStr);
    setStartDate(minus7Str);

    setSelectedDate(null);
    setSelectedDayTasks([]);
    setPracticeMetrics(null);
    setWeightMetrics(null);
    setLeetCodeMetrics(null);
    setIsDayTasksLoading(false);
    setIsDayPracticeLoading(false);
    setIsDayWeightLoading(false);
    setIsDayLeetCodeLoading(false);
  };

  // ---------------------------
  // 12) WebSocket for sensor data
  // ---------------------------
  useEffect(() => {
    const client = createWebSocketClient(
      (client) => {
        client.subscribe("/topic/sensor-data", (message) => {
          const data = JSON.parse(message.body);
          setSensorData(data);
          console.log("Received sensor data:", data);
        });
      },
      (error) => {
        console.error("WebSocket connection error:", error);
      }
    );

    client.activate();
    return () => {
      client.deactivate();
    };
  }, []);

  // ---------------------------
  // Render
  // ---------------------------
  return (
    <div style={{ padding: "1rem" }}>
      <h2>Tasks Over Date Range</h2>

      {/* (A) Date Range Selectors */}
      <div style={{ marginBottom: "1rem" }}>
        <label>
          Start Date:{" "}
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </label>{" "}
        <label>
          End Date:{" "}
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </label>{" "}
        <button onClick={handleReset}>Reset Range</button>
      </div>

      {/* (B) Chart */}
      <div style={{ width: "100%", height: 400 }}>
        <ResponsiveContainer>
          <ComposedChart
            data={chartData}
            margin={{ top: 20, right: 20, bottom: 20, left: 20 }}
            onClick={handleChartClick}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis yAxisId="left" />
            <YAxis yAxisId="right" orientation="right" domain={[0, 100]} />
            <Tooltip />
            <Legend />

            {/* Stacked bars */}
            <Bar
              yAxisId="left"
              dataKey="completed"
              stackId="tasks"
              fill="#82ca9d"
              name="Completed"
            />
            <Bar
              yAxisId="left"
              dataKey="notCompleted"
              stackId="tasks"
              fill="#ff8b8b"
              name="Not Completed"
            />

            {/* Line for total completion */}
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="percent"
              stroke="#8884d8"
              name="Total Completion (%)"
              activeDot={{ r: 8 }}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {/* (C) Day-Level Detail */}
      <div style={{ marginTop: "1rem", border: "1px solid #ccc", padding: "1rem" }}>
        {selectedDate ? (
          <>
            {/* Use the TaskDisplaySection component */}
            <TaskDisplaySection
              selectedDayTasks={selectedDayTasks}
              isDayTasksLoading={isDayTasksLoading}
              selectedDate={selectedDate}
            />

            {/* Metrics Section */}
            <MetricsSection
              practiceMetrics={practiceMetrics}
              weightMetrics={weightMetrics}
              leetcodeMetrics={leetcodeMetrics}
              isDayPracticeLoading={isDayPracticeLoading}
              isDayWeightLoading={isDayWeightLoading}
              isDayLeetCodeLoading={isDayLeetCodeLoading}
              handleForcePractice={handleForcePractice}
            />
          </>
        ) : (
          <p>Click on a bar or line data point to see that day’s tasks and metrics.</p>
        )}
      </div>

      {/* (D) Real-Time Sensor Data via WebSocket */}
      <div style={{ marginTop: "2rem", border: "1px solid #ccc", padding: "1rem" }}>
        <h3>Real-Time Sensor Data</h3>
        {sensorData.rawValue !== null ? (
          <div>
            <p>
              Raw Value: <strong>{sensorData.rawValue}</strong>
            </p>
            <p>
              Weight: <strong>{sensorData.weight} kg</strong>
            </p>
          </div>
        ) : (
          <p>No sensor data received yet.</p>
        )}
      </div>
    </div>
  );
}

export default TaskChartPage;
