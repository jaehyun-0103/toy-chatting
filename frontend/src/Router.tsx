import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ChatList from "./pages/ChatList";
import CreateChatRoom from "./pages/CreateChatRoom";
import ChatRoom from "./pages/ChatRoom";

const AppRoutes: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/chatList" element={<ChatList />} />
        <Route path="/createChatRoom" element={<CreateChatRoom />} />
        <Route path="/chatRoom/:roomId" element={<ChatRoom />} />
      </Routes>
    </Router>
  );
};

export default AppRoutes;
