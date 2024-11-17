import React, { useState } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Swal from "sweetalert2";

// 채팅방 생성
const CreateChatRoom = () => {
  const navigate = useNavigate();
  
  const [title, setTitle] = useState("");
  const [maxMembers, setMaxMembers] = useState(10);
  const [isPrivate, setIsPrivate] = useState(false);

  const handleCreateChatRoom = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await axios.post(
        "/api/chatrooms",
        { title, max_members: maxMembers, is_private: isPrivate },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      Swal.fire({
        icon: "success",
        title: "생성 성공",
        text: response.data.message,
      });
      navigate("/chatList");
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "생성 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  return (
    <Container>
      <h2>채팅방 생성</h2>
      <Input type="text" placeholder="제목" value={title} onChange={(e) => setTitle(e.target.value)} />
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
      <ButtonContainer>
        <Button onClick={handleCreateChatRoom}>생성</Button>
        <BackButton onClick={() => navigate("/chatList")}>취소</BackButton>
      </ButtonContainer>
    </Container>
  );
};

export default CreateChatRoom;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  height: 100vh;
  background-color: #f8f9fa;
  padding-top: 20px;
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
  justify-content: flex-start;
  width: 300px;
  margin: 0.5rem 0;
`;

const Checkbox = styled.input`
  margin-right: 0.5rem;
  transform: scale(1.2);
  vertical-align: middle;
`;

const ButtonContainer = styled.div`
  display: flex;
  justify-content: space-between;
  width: 300px;
  margin-top: 1rem;
`;

const Button = styled.button`
  width: 45%;
  padding: 0.8rem;
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

const BackButton = styled.button`
  width: 45%;
  padding: 0.8rem;
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
