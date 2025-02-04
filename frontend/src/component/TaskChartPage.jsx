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
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { createWebSocketClient } from "../util/apiClient";

/**
 * Simple weighting logic:
 * - tasks = 60% of the overall daily completion
 * - practice = 40%
 */
const TASKS_WEIGHT = 0.6;
const PRACTICE_WEIGHT = 0.4;

function TaskChartPage() {
    // ---------------------------
    // 1) Range selection
    // ---------------------------
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");

    // ---------------------------
    // 2) We store tasks & practice data separately
    // ---------------------------
    const [tasksDayMap, setTasksDayMap] = useState({});       // { date: Task[] }
    const [practiceRangeMap, setPracticeRangeMap] = useState({}); // { date: { percentage, ... } }

    // ---------------------------
    // 3) Final chart data to be displayed in Recharts
    // ---------------------------
    const [chartData, setChartData] = useState([]);

    // ---------------------------
    // Day-level details
    // ---------------------------
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedDayTasks, setSelectedDayTasks] = useState([]);
    const [practiceMetrics, setPracticeMetrics] = useState(null);

    // Separate loading states for day-level fetch
    const [isDayTasksLoading, setIsDayTasksLoading] = useState(false);
    const [isDayPracticeLoading, setIsDayPracticeLoading] = useState(false);

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
    // 5) Whenever startDate/endDate changes, fetch tasks & practice
    // ---------------------------
    useEffect(() => {
        if (!startDate || !endDate) return;
        fetchTasksRange();
        fetchPracticeRange();
    }, [startDate, endDate]);

    // ---------------------------
    // 6) useEffect that merges tasksDayMap + practiceRangeMap
    //    into chartData whenever either changes
    // ---------------------------
    useEffect(() => {
        const combined = transformDayMapToChartData(tasksDayMap, practiceRangeMap);
        setChartData(combined);
    }, [tasksDayMap, practiceRangeMap]);

    // ---------------------------
    // 7) Fetch tasks for the entire range
    // ---------------------------
    async function fetchTasksRange() {
        try {
            const dayMap = await apiGet(`/tasks/range?start=${startDate}&end=${endDate}`);
            // dayMap => { "2025-01-01": [Task1, Task2], ... }
            setTasksDayMap(dayMap);
        } catch (err) {
            console.error("Failed to fetch tasks range:", err);
            setTasksDayMap({});
        }
    }

    // ---------------------------
    // 8) Fetch practice for the entire range
    // ---------------------------
    async function fetchPracticeRange() {
        try {
            const data = await apiGet(`/keybr/range?start=${startDate}&end=${endDate}`);
            // data => { "2025-01-01": { percentage, totalMinutes, ... }, ... }
            setPracticeRangeMap(data);
        } catch (err) {
            console.error("Failed to fetch practice range:", err);
            setPracticeRangeMap({});
        }
    }

    // ---------------------------
    // 9) transformDayMapToChartData: merges tasks & practice
    // ---------------------------
    function transformDayMapToChartData(tasksMap, practiceMap) {
        // tasksMap: { date -> Task[] }
        // practiceMap: { date -> { percentage, ... } }

        const results = Object.entries(tasksMap).map(([date, tasks]) => {
            // tasks portion
            const completedCount = tasks.filter(t => t.completed).length;
            const total = tasks.length;
            const tasksPercent = total > 0 ? (completedCount / total) * 100 : 0;

            // practice portion
            let practicePercent = 0;
            if (practiceMap[date] && practiceMap[date].percentage != null) {
                practicePercent = practiceMap[date].percentage;
            }

            // Weighted overall
            const overall = (tasksPercent * TASKS_WEIGHT) + (practicePercent * PRACTICE_WEIGHT);

            return {
                date,
                completed: completedCount,
                notCompleted: total - completedCount,
                percent: Math.round(overall),
            };
        });

        // (Optional) If you want to handle "practice days with no tasks," you'd also map over
        // practiceMap to fill those in. Typically, you only chart days that had tasks though.

        results.sort((a, b) => (a.date > b.date ? 1 : -1));
        return results;
    }

    // ---------------------------
    // 10) Day-level click => fetch tasks detail + practice detail
    // ---------------------------
    const handleChartClick = async (data) => {
        if (data && data.activePayload && data.activePayload.length > 0) {
            const { date } = data.activePayload[0].payload;
            console.log("Clicked date:", date);

            setSelectedDate(date);
            setSelectedDayTasks([]);
            setPracticeMetrics(null);

            setIsDayTasksLoading(true);
            setIsDayPracticeLoading(true);

            // A) Fetch tasks detail
            try {
                const dayTasks = await apiGet(`/tasks/day/${date}`);
                // if dayTasks is an array -> setSelectedDayTasks(dayTasks)
                // if it's { tasks: [...] }, adapt accordingly
                setSelectedDayTasks(dayTasks);
            } catch (err) {
                console.error("Failed to fetch day tasks:", err);
            } finally {
                setIsDayTasksLoading(false);
            }

            // B) Fetch practice detail
            try {
                const practiceDay = await apiGet(`/keybr/day/${date}`);
                setPracticeMetrics(practiceDay);
            } catch (err) {
                console.error("Failed to fetch practice detail:", err);
            } finally {
                setIsDayPracticeLoading(false);
            }
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
        setIsDayTasksLoading(false);
        setIsDayPracticeLoading(false);
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
                </label>
                {" "}
                <label>
                    End Date:{" "}
                    <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                    />
                </label>
                {" "}
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

                        {/* Stacked bars for tasks */}
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

                        {/* Single line for the combined daily completion */}
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
                        <h3>Tasks for {selectedDate}</h3>

                        {isDayTasksLoading ? (
                            <p>Loading tasks...</p>
                        ) : (
                            <>
                                {Array.isArray(selectedDayTasks) && selectedDayTasks.length > 0 ? (
                                    <ul>
                                        {selectedDayTasks.map((t, i) => (
                                            <li key={i} style={{ marginBottom: "0.5rem" }}>
                                                {t.completed ? "[x]" : "[ ]"} {t.description}
                                            </li>
                                        ))}
                                    </ul>
                                ) : (
                                    <p>No tasks found.</p>
                                )}
                            </>
                        )}

                        {isDayPracticeLoading ? (
                            <p>Loading practice metrics...</p>
                        ) : practiceMetrics ? (
                            <div style={{ marginTop: "1rem" }}>
                                <h4>Practice Metrics</h4>
                                <p>
                                    Percentage: <strong>{practiceMetrics.percentage}%</strong><br />
                                    Total Minutes: <strong>{practiceMetrics.totalMinutes}</strong><br />
                                    Minutes Practiced: <strong>{practiceMetrics.minutesPracticed}</strong>
                                </p>
                            </div>
                        ) : (
                            <p>No practice metrics for this day.</p>
                        )}
                    </>
                ) : (
                    <p>Click on a bar or line data point to see that day’s tasks and practice metrics.</p>
                )}
            </div>

            {/* (D) Real-Time Sensor Data via WebSocket */}
            <div style={{ marginTop: "2rem", border: "1px solid #ccc", padding: "1rem" }}>
                <h3>Real-Time Sensor Data</h3>
                {sensorData.rawValue !== null ? (
                    <div>
                        <p>Raw Value: <strong>{sensorData.rawValue}</strong></p>
                        <p>Weight: <strong>{sensorData.weight} kg</strong></p>
                    </div>
                ) : (
                    <p>No sensor data received yet.</p>
                )}
            </div>
        </div>
    );
}

export default TaskChartPage;
