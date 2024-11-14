import React, { useState } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";

// 채팅방 생성
const CreateChatRoom = () => {
  const [title, setTitle] = useState("");
  const [maxMembers, setMaxMembers] = useState(10);
  const [isPrivate, setIsPrivate] = useState(false);
  const navigate = useNavigate();

  const handleCreateChatRoom = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await axios.post(
        "/api/chatrooms",
        { title, max_members: maxMembers, is_private: isPrivate },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      alert(response.data.message);
      navigate("/chatList");
    } catch (error: any) {
      alert("채팅방 생성 실패: " + (error.response?.data?.message || error.message));
    }
  };

  return (
    <Container>
      <Title>채팅방 생성</Title>
      <Input type="text" placeholder="채팅방 제목" value={title} onChange={(e) => setTitle(e.target.value)} />
      <Input
        type="number"
        placeholder="최대 인원 수"
        value={maxMembers}
        onChange={(e) => setMaxMembers(Number(e.target.value))}
        min={1}
        max={20}
      />
      <CheckboxContainer>
        <label>
          <Checkbox type="checkbox" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)} />
          비공개 방
        </label>
      </CheckboxContainer>
      <Button onClick={handleCreateChatRoom}>채팅방 생성</Button>
      <BackButton onClick={() => navigate("/chatList")}>채팅방 목록으로 돌아가기</BackButton>
    </Container>
  );
};

export default CreateChatRoom;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f8f9fa;
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

const CheckboxContainer = styled.div`
  display: flex;
  align-items: center;
  margin: 0.5rem 0;
`;

const Checkbox = styled.input`
  margin-right: 0.5rem;
  transform: scale(1.2);
`;

const Button = styled.button`
  width: 300px;
  padding: 0.8rem;
  margin-top: 1rem;
  font-size: 1rem;
  color: #fff;
  background-color: #28a745;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #218838;
  }
`;

const BackButton = styled.button`
  width: 300px;
  padding: 0.8rem;
  margin-top: 0.5rem;
  font-size: 1rem;
  color: #007bff;
  background-color: transparent;
  border: 1px solid #007bff;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #e6f7ff;
  }
`;
