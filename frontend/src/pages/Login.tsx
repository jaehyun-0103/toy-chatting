import React, { useState } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Swal from "sweetalert2";

// 로그인
const Login = () => {
  const navigate = useNavigate();
  
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    try {
      const response = await axios.post("/api/login", { email, password });
      Swal.fire({
        icon: "success",
        title: "로그인 성공",
        text: response.data.message,
      });
      localStorage.setItem("token", response.data.token);
      localStorage.setItem("user_id", response.data.user_id);
      navigate("/chatList");
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "로그인 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleKeyPress = (event: any) => {
    if (event.key === "Enter") {
      handleLogin();
    }
  };

  return (
    <Container>
      <Title>로그인</Title>
      <Title>배포 자동화 테스트1234~</Title>
      <Input type="email" placeholder="이메일" value={email} onChange={(e) => setEmail(e.target.value)} />
      <Input
        type="password"
        placeholder="비밀번호"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        onKeyPress={handleKeyPress}
      />
      <Button onClick={handleLogin}>로그인</Button>
      <RegisterButton onClick={() => navigate("/register")}>회원가입</RegisterButton>
    </Container>
  );
};

export default Login;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f0f2f5;
`;

const Title = styled.h1`
  font-size: 2rem;
  color: #333;
  margin-bottom: 1rem;
`;

const Input = styled.input`
  width: 300px;
  padding: 0.8rem;
  margin: 0.5rem 0;
  font-size: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  outline: none;
  &:focus {
    border-color: #007bff;
  }
`;

const Button = styled.button`
  width: 300px;
  padding: 0.8rem;
  margin-top: 1rem;
  font-size: 1rem;
  color: #fff;
  background-color: #007bff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #0056b3;
  }
`;

const RegisterButton = styled.button`
  width: 300px;
  padding: 0.8rem;
  margin-top: 0.5rem;
  font-size: 1rem;
  color: #007bff;
  background-color: #fff;
  border: 1px solid #007bff;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #e6f7ff;
  }
`;
