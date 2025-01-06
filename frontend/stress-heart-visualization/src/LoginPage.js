import React, { useState } from "react";
import axios from "axios";
import "./LoginPage.css"; // Link to external CSS for styling

const LoginPage = ({ onLogin }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async () => {
    try {
      const response = await axios.post("https://api.hcgateway.shuchir.dev/api/v2/login", {
        username,
        password,
      });
      const { token } = response.data;
      const {expiry} = response.data;
      const {refresh} = response.data;
      localStorage.setItem("authToken", token);
      localStorage.setItem("expiry", expiry);
      localStorage.setItem("refresh", refresh);
      onLogin(token, expiry, refresh);
    } catch (err) {
      setError("Login failed. Please check your credentials.");
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2 className="login-title">Sign In</h2>
        {error && <p className="login-error">{error}</p>}
        <div className="login-field">
          <input
            type="text"
            className="login-input"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>
        <div className="login-field">
          <input
            type="password"
            className="login-input"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button className="login-button" onClick={handleLogin}>
          Login
        </button>
      </div>
    </div>
  );
};

export default LoginPage;
