import React, { useState } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";

// 회원가입
const Register = () => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleRegister = async () => {
    try {
      const response = await axios.post("/api/register", { username, email, password });
      alert(response.data.message);
      navigate("/");
    } catch (error: any) {
      alert("회원가입 실패: " + (error.response?.data?.message || error.message));
    }
  };

  return (
    <Container>
      <Title>회원가입</Title>
      <Input type="text" placeholder="사용자 이름" value={username} onChange={(e) => setUsername(e.target.value)} />
      <Input type="email" placeholder="이메일" value={email} onChange={(e) => setEmail(e.target.value)} />
      <Input type="password" placeholder="비밀번호" value={password} onChange={(e) => setPassword(e.target.value)} />
      <LoginButton onClick={() => navigate("/")}>이미 계정이 있으신가요?</LoginButton>
      <Button onClick={handleRegister}>회원가입</Button>
    </Container>
  );
};

export default Register;

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

const LoginButton = styled.button`
  display: flex;
  justify-content: flex-end;
  width: 320px;
  padding: 0rem 0;
  margin-top: 0.5rem;
  font-size: 1rem;
  color: #007bff;
  background-color: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-decoration: underline;
  line-height: 1.5;
`;
