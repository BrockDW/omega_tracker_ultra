import React, { useState, useEffect } from "react";
import {
    BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, ComposedChart
} from "recharts";
import { apiGet } from "../util/apiClient";

// This page shows a combined bar + line chart using Recharts' "ComposedChart"

function TaskChartPage() {
    // 1) State for date range
    const [startDate, setStartDate] = useState("2024-01-01");
    const [endDate, setEndDate] = useState("2024-01-07");

    // 2) State for chart data
    const [chartData, setChartData] = useState([]);

    // 3) State for selected day’s tasks (when clicking bar/line)
    const [selectedDayTasks, setSelectedDayTasks] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);

    // 2) On initial load, compute "today" and "today minus 7" 
    //    and set them as the date range
    useEffect(() => {
        const today = new Date();
        const todayStr = today.toISOString().split("T")[0];

        const minus7 = new Date();
        minus7.setDate(minus7.getDate() - 7);
        const minus7Str = minus7.toISOString().split("T")[0];

        // set them once on mount
        setEndDate(todayStr);
        setStartDate(minus7Str);
    }, []);

    // Fake data generator (in a real app, you'd fetch from your backend)
    // We'll return an array of objects, each with:
    //   date, completed, notCompleted, percentCompleted
    // for example: { date: '2024-01-01', completed: 5, notCompleted: 2, percent: 71.4 }
    useEffect(() => {
        async function fetchRange() {
            const dayMap = await apiGet(`/api/tasks/range?start=${startDate}&end=${endDate}`);
            const transformed = transformDayMapToChartData(dayMap);
            setChartData(transformed);

            setSelectedDate(null);
            setSelectedDayTasks([]);

            console.log(dayMap);
        }
        // In real usage, you'd fetch from your server using startDate/endDate
        fetchRange();
        // const dummyData = generateDummyData(startDate, endDate);
        // setChartData(dummyData);
    }, [startDate, endDate]);

    // Handler for bar/line click
    // The Recharts "payload" typically includes the data object
    const handleChartClick = async (data, index) => {
        if (data && data.activePayload && data.activePayload.length > 0) {
            const { date } = data.activePayload[0].payload;
            console.log(date);
            // In real usage, you'd fetch tasks from the server for that date
            // For now, we’ll just generate some fake tasks
            const tasks = await apiGet(`/api/tasks/day/${date}`);
            console.log(tasks);
            setSelectedDate(date);
            setSelectedDayTasks(tasks);
        }
    };


    // 5) "Reset" button logic: set startDate and endDate 
    //    again to "today" and "today minus 7"
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

            {/* Panel for the day’s tasks */}
            <div style={{ marginTop: "1rem", border: "1px solid #ccc", padding: "1rem" }}>
                {selectedDate ? (
                    <>
                        <h3>Tasks for {selectedDate}</h3>
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
                    </>
                ) : (
                    <p>Click on a bar or line data point to see that day’s tasks.</p>
                )}
            </div>
        </div>
    );
}

/** 
 * -- Helper Functions --
 * 
 * 1) generateDummyData: produces an array of objects: 
 *    [ { date, completed, notCompleted, percent }, ... ]
 * 
 * 2) generateFakeTasksForDate: returns an array of tasks 
 *    [ { description, completed }, ... ]
 */

// For example, let's create a date range from startDate to endDate 
// and randomize completed / notCompleted.
function generateDummyData(startDateStr, endDateStr) {
    const results = [];

    let current = new Date(startDateStr);
    const end = new Date(endDateStr);

    while (current <= end) {
        // random numbers
        const completed = Math.floor(Math.random() * 5) + 1;
        const notCompleted = Math.floor(Math.random() * 5);
        const total = completed + notCompleted;
        const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

        // format date as YYYY-MM-DD
        const dateStr = current.toISOString().split("T")[0];

        results.push({
            date: dateStr,
            completed,
            notCompleted,
            percent,
        });

        // increment 1 day
        current.setDate(current.getDate() + 1);
    }

    return results;
}

function transformDayMapToChartData(dayMap) {
    // dayMap example:
    // {
    //   "2024-01-01": [ {description, completed}, {description, completed}, ... ],
    //   "2024-01-02": [],
    //   ...
    // }

    // We'll do Object.entries to get the [date, tasks] pairs
    // Then for each date, count how many completed vs not
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
