import React, { useState, useEffect } from "react";
import {
    BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, ComposedChart
} from "recharts";
import { apiGet } from "../util/apiClient";
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { createWebSocketClient } from "../util/apiClient";

function TaskChartPage() {
    // Existing state for task chart
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [chartData, setChartData] = useState([]);
    const [selectedDayTasks, setSelectedDayTasks] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);
    const [practiceMetrics, setPracticeMetrics] = useState(null);
    const [isDayDataLoading, setIsDayDataLoading] = useState(false);

    // New state for WebSocket data
    const [sensorData, setSensorData] = useState({ rawValue: null, weight: null });

    // WebSocket connection
    useEffect(() => {
        // Create WebSocket client
        const client = createWebSocketClient(
          (client) => {
            // On connect, subscribe to the topic
            client.subscribe('/topic/sensor-data', (message) => {
              const data = JSON.parse(message.body);
              setSensorData(data);
              console.log('Received sensor data:', data);
            });
          },
          (error) => {
            console.error('WebSocket connection error:', error);
          }
        );

        client.activate();

        return () => {
            client.deactivate();
        };
    }, []);

    // Existing useEffect for initial load
    useEffect(() => {
        const today = new Date();
        const todayStr = today.toISOString().split("T")[0];

        const minus7 = new Date();
        minus7.setDate(minus7.getDate() - 7);
        const minus7Str = minus7.toISOString().split("T")[0];

        setEndDate(todayStr);
        setStartDate(minus7Str);
    }, []);

    // Existing useEffect for date range change
    useEffect(() => {
        if (!startDate || !endDate) return;
        fetchRange();
    }, [startDate, endDate]);

    // Existing fetchRange function
    const fetchRange = async () => {
        console.log("Fetching range data...");
        const dayMap = await apiGet(`/tasks/range?start=${startDate}&end=${endDate}`);
        const transformed = transformDayMapToChartData(dayMap);
        setChartData(transformed);

        // Reset selected day info
        setSelectedDate(null);
        setSelectedDayTasks([]);
        setPracticeMetrics(null);
        setIsDayDataLoading(false);

        console.log(dayMap);
    };

    // Existing handleChartClick function
    const handleChartClick = async (data) => {
        if (data && data.activePayload && data.activePayload.length > 0) {
            const { date } = data.activePayload[0].payload;
            console.log("Clicked date:", date);

            setSelectedDate(date);
            setSelectedDayTasks([]);
            setPracticeMetrics(null);
            setIsDayDataLoading(true);

            try {
                const dayData = await apiGet(`/tasks/day/${date}/v2`);
                console.log("Day data:", dayData);

                setSelectedDayTasks(dayData.tasks || []);
                setPracticeMetrics(dayData.keyBrPracticeResult || null);
            } catch (err) {
                console.error("Failed to fetch day data:", err);
            } finally {
                setIsDayDataLoading(false);
            }
        }
    };

    // Existing handleReset function
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

            {/* New Section: WebSocket Data */}
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

// Existing transformDayMapToChartData function
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

    results.sort((a, b) => (a.date > b.date ? 1 : -1));
    return results;
}

export default TaskChartPage;