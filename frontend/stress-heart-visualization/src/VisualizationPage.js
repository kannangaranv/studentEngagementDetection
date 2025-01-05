import React, { useState, useEffect } from "react";
import axios from "axios";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const VisualizationPage = ({ token }) => {
  const [stressData, setStressData] = useState([]);
  const [stressLabels, setStressLabels] = useState([]); // Labels for stress scores
  const [heartRateData, setHeartRateData] = useState([]); // Heart rates as individual points
  const [heartRateLabels, setHeartRateLabels] = useState([]); // Corresponding timestamps for heart rates
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await axios.post(
          "http://localhost:8080/api/fetchHeartRateAndStressScore",
          {
            startTime: "2024-12-07T18:30:00Z",
            endTime: "2024-12-07T19:14:09.744Z",
            accessToken: token,
          },
          {
            headers: {
              "Content-Type": "application/json",
            },
          }
        );

        const data = response.data;

        // Process stress scores and labels
        const stress = data.map((entry) => entry.stressScore);
        const stressTimestamps = data.map((entry) =>
          new Date(entry.intervalStart).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
        );

        // Process heart rates and labels
        const heartRates = [];
        const heartRateTimestamps = [];
        data.forEach((entry) => {
          const intervalStart = new Date(entry.intervalStart).getTime();
          const intervalEnd = new Date(entry.intervalEnd).getTime();
          const intervalDuration = (intervalEnd - intervalStart) / entry.heartRates.length;

          entry.heartRates.forEach((heartRate, index) => {
            heartRates.push(heartRate);

            // Generate timestamp for each heart rate point
            const timestamp = new Date(intervalStart + index * intervalDuration);
            heartRateTimestamps.push(
              timestamp.toLocaleTimeString("en-US", {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              })
            );
          });
        });

        setStressData(stress);
        setStressLabels(stressTimestamps);
        setHeartRateData(heartRates);
        setHeartRateLabels(heartRateTimestamps);
      } catch (err) {
        console.error("Error fetching data:", err);
        setError("Failed to fetch data. Please check your server and network connection.");
      }
    };

    fetchData();
  }, [token]);

  return (
    <div style={{ padding: "20px", fontFamily: "Arial, sans-serif" }}>
      <h2 style={{ textAlign: "center", color: "#333" }}>Stress and Heart Rate Visualization</h2>
      {error && <p style={{ color: "red", textAlign: "center" }}>{error}</p>}

      <div style={{ margin: "20px 0" }}>
        <h3 style={{ textAlign: "center", color: "#555" }}>Stress Score</h3>
        <Line
          data={{
            labels: stressLabels,
            datasets: [
              {
                label: "Stress Score",
                data: stressData,
                borderColor: "rgba(255, 99, 132, 1)",
                borderWidth: 2,
                fill: false,
              },
            ],
          }}
          options={{
            responsive: true,
            plugins: {
              legend: {
                position: "top",
              },
              title: {
                display: true,
                text: "Stress Score Over Time",
              },
            },
            scales: {
              x: {
                title: { display: true, text: "Time" },
              },
              y: {
                title: { display: true, text: "Stress Score" },
              },
            },
          }}
        />
      </div>

      <div style={{ margin: "20px 0" }}>
        <h3 style={{ textAlign: "center", color: "#555" }}>Heart Rate</h3>
        <Line
          data={{
            labels: heartRateLabels,
            datasets: [
              {
                label: "Heart Rate",
                data: heartRateData,
                borderColor: "rgba(54, 162, 235, 1)",
                borderWidth: 2,
                fill: false,
              },
            ],
          }}
          options={{
            responsive: true,
            plugins: {
              legend: {
                position: "top",
              },
              title: {
                display: true,
                text: "Heart Rate Over Time",
              },
            },
            scales: {
              x: {
                title: { display: true, text: "Time" },
              },
              y: {
                title: { display: true, text: "Heart Rate (BPM)" },
              },
            },
          }}
        />
      </div>
    </div>
  );
};

export default VisualizationPage;
