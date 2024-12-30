import React, { useState } from "react";
import LoginPage from "./LoginPage";
import VisualizationPage from "./VisualizationPage";

const App = () => {
  const [token, setToken] = useState(localStorage.getItem("authToken") || "");

  return (
    <div>
      {!token ? (
        <LoginPage onLogin={setToken} />
      ) : (
        <VisualizationPage token={token} />
      )}
    </div>
  );
};

export default App;
