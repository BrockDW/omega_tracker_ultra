import React, { useState, useEffect } from "react";
import {
    BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, ComposedChart
} from "recharts";
import { apiGet } from "../util/apiClient";

// This page shows a combined bar + line chart using Recharts' "ComposedChart"

function TaskChartPage() {
    // 1) State for date range
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");

    // 2) State for chart data
    const [chartData, setChartData] = useState([]);

    // 3) State for selected day’s tasks (when clicking bar/line)
    const [selectedDayTasks, setSelectedDayTasks] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);

    // 4) State for practice metrics from /day/{date}/v2
    const [practiceMetrics, setPracticeMetrics] = useState(null);

    // 5) NEW: loading indicator for day details
    const [isDayDataLoading, setIsDayDataLoading] = useState(false);

    // On initial load, compute "today" and "today minus 7" and set them as the date range
    useEffect(() => {
        const today = new Date();
        const todayStr = today.toISOString().split("T")[0];

        const minus7 = new Date();
        minus7.setDate(minus7.getDate() - 7);
        const minus7Str = minus7.toISOString().split("T")[0];

        setEndDate(todayStr);
        setStartDate(minus7Str);
    }, []);

    // Whenever startDate or endDate changes, fetch the chart data
    useEffect(() => {
        if (!startDate || !endDate) return;
        fetchRange();
    }, [startDate, endDate]);

    // Fetch the aggregated data for the date range to fill the chart
    const fetchRange = async () => {
        console.log("Fetching range data...");
        const dayMap = await apiGet(`/tasks/range?start=${startDate}&end=${endDate}`);
        const transformed = transformDayMapToChartData(dayMap);
        setChartData(transformed);

        // Reset selected day info
        setSelectedDate(null);
        setSelectedDayTasks([]);
        setPracticeMetrics(null);
        setIsDayDataLoading(false); // just in case

        console.log(dayMap);
    };

    // Handler for bar/line click
    // We'll now call the new endpoint: GET /day/{date}/v2
    const handleChartClick = async (data) => {
        if (data && data.activePayload && data.activePayload.length > 0) {
            const { date } = data.activePayload[0].payload;
            console.log("Clicked date:", date);

            // Immediately set selectedDate so the UI can display "Tasks for {date}"
            setSelectedDate(date);
            setSelectedDayTasks([]);
            setPracticeMetrics(null);
            setIsDayDataLoading(true);

            try {
                // Fetch from your new endpoint
                const dayData = await apiGet(`/tasks/day/${date}/v2`);
                console.log("Day data:", dayData);

                // dayData = {
                //   date: "2025-01-24",
                //   tasks: [...],
                //   keyBrPracticeResult: {
                //     percentage, totalMinutes, minutesPracticed
                //   }
                // }

                setSelectedDayTasks(dayData.tasks || []);
                setPracticeMetrics(dayData.keyBrPracticeResult || null);
            } catch (err) {
                console.error("Failed to fetch day data:", err);
                // Optionally set some error state if needed
            } finally {
                // Hide loading spinner
                setIsDayDataLoading(false);
            }
        }
    };

    // Reset button logic: set startDate and endDate again to "today" and "today minus 7"
    const handleReset = () => {
        const today = new Date();
        const todayStr = today.toISOString().split("T")[0];

        const minus7 = new Date();
        minus7.setDate(minus7.getDate() - 7);
        const minus7Str = minus7.toISOString().split("T")[0];

        setEndDate(todayStr);
        setStartDate(minus7Str);

        // clear state
        setSelectedDate(null);
        setSelectedDayTasks([]);
        setPracticeMetrics(null);
        setIsDayDataLoading(false);
    };

    return (
        <div style={{ padding: "1rem" }}>
            <h2>Tasks Over Date Range</h2>

            {/* Date Range Selectors */}
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
                {" "}
                <button onClick={fetchRange}>Refresh</button>
            </div>

            {/* Combined Bar + Line Chart */}
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

                        {/* One stacked bar with "completed" at the bottom, "notCompleted" on top */}
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

                        {/* The line for percent completed */}
                        <Line
                            yAxisId="right"
                            type="monotone"
                            dataKey="percent"
                            stroke="#8884d8"
                            name="Percent Completed"
                            activeDot={{ r: 8 }}
                        />
                    </ComposedChart>
                </ResponsiveContainer>
            </div>

            {/* Panel for the day’s tasks + practice metrics */}
            <div style={{ marginTop: "1rem", border: "1px solid #ccc", padding: "1rem" }}>
                {selectedDate ? (
                    <>
                        <h3>Tasks for {selectedDate}</h3>

                        {/* If data is still loading, show a loading indicator */}
                        {isDayDataLoading ? (
                            <p>Loading...</p>
                        ) : (
                            <>
                                {selectedDayTasks.length > 0 ? (
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

                                {/* Practice metrics */}
                                {practiceMetrics ? (
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
                        )}
                    </>
                ) : (
                    <p>Click on a bar or line data point to see that day’s tasks and practice metrics.</p>
                )}
            </div>
        </div>
    );
}

/**
 * Helper function: transformDayMapToChartData
 *
 * dayMap example:
 * {
 *   "2024-01-01": [ {description, completed}, {description, completed}, ... ],
 *   "2024-01-02": [],
 *   ...
 * }
 *
 * We produce an array of objects:
 * [
 *   { date, completed, notCompleted, percent },
 *   ...
 * ]
 */
function transformDayMapToChartData(dayMap) {
    const results = Object.entries(dayMap).map(([date, tasks]) => {
        const completedCount = tasks.filter(t => t.completed).length;
        const notCompletedCount = tasks.length - completedCount;
        const total = tasks.length;
        const percent = total > 0 ? Math.round((completedCount / total) * 100) : 0;
        return {
            date,
            completed: completedCount,
            notCompleted: notCompletedCount,
            percent
        };
    });

    // optionally sort results by date
    results.sort((a, b) => (a.date > b.date ? 1 : -1));
    return results;
}

export default TaskChartPage;
