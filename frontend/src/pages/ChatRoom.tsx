import React, { useState, useEffect } from "react";
import styled from "styled-components";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";

interface MessageType {
  message_id: number;
  username: string;
  user_id: number;
  content: string;
  updated_at: string;
}

interface MemberType {
  user_id: number;
  username: string;
  joined_at: string;
}

// 채팅방
const ChatRoom: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const [messages, setMessages] = useState<MessageType[]>([]);
  const [content, setContent] = useState<string>("");
  const [members, setMembers] = useState<MemberType[]>([]);
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [editingMessageId, setEditingMessageId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState<string>("");
  const navigate = useNavigate();

  const userId = parseInt(localStorage.getItem("user_id") || "0");
  const creatorId = parseInt(localStorage.getItem("creator_id") || "0");
  const isPrivate = localStorage.getItem("is_private") === "true";

  useEffect(() => {
    const token = localStorage.getItem("token");
    const fetchMessagesAndMembers = async () => {
      setLoading(true);
      try {
        const messagesResponse = await axios.get<MessageType[]>(`/api/messages/${roomId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        setMessages(messagesResponse.data);

        const membersResponse = await axios.get<MemberType[]>(`/api/chatrooms/${roomId}/members`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        setMembers(membersResponse.data);
      } catch (error: any) {
        alert("데이터 로딩 실패");
      } finally {
        setLoading(false);
      }
    };

    fetchMessagesAndMembers();
  }, [roomId]);

  const handleSendMessage = async () => {
    const token = localStorage.getItem("token");

    // 임시로 생성한 메시지 ID
    const newMessage = {
      message_id: Date.now(),
      username: "나",
      user_id: userId,
      content,
      updated_at: new Date().toISOString(),
    };

    // 1. 메시지를 보낸 직후 바로 로컬 상태에 추가 (UI에 메시지가 즉시 표시됨)
    setMessages((prevMessages) => [...prevMessages, newMessage]);

    try {
      // 2. 서버에 메시지 전송 요청
      const response = await axios.post<{ message: string; message_id: number }>(
        `/api/messages/${roomId}`,
        { content },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // 3. 서버에서 받은 message_id로 로컬 상태를 업데이트
      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.message_id === newMessage.message_id
            ? { ...msg, message_id: response.data.message_id } // 서버에서 받은 실제 message_id로 교체
            : msg
        )
      );

      alert(response.data.message);
      setContent("");
    } catch (error: any) {
      alert("메시지 전송 실패");
      setMessages((prevMessages) => prevMessages.filter((msg) => msg.message_id !== newMessage.message_id));
    }
  };

  const handleEditMessage = (messageId: number, currentContent: string) => {
    setEditingMessageId(messageId);
    setEditingContent(currentContent);
  };

  const handleUpdateMessage = async () => {
    const token = localStorage.getItem("token");
    if (editingMessageId !== null && editingContent !== "") {
      try {
        const response = await axios.put<{ message: string }>(
          `/api/messages/${roomId}/${editingMessageId}`,
          { content: editingContent },
          {
            headers: { Authorization: `Bearer ${token}` },
          }
        );
        alert(response.data.message);
        setMessages(
          messages.map((msg) =>
            msg.message_id === editingMessageId ? { ...msg, content: editingContent, updated_at: new Date().toISOString() } : msg
          )
        );
        setEditingMessageId(null);
        setEditingContent("");
      } catch (error: any) {
        alert("메시지 수정 실패");
      }
    }
  };

  const handleLeaveChatRoom = async () => {
    const token = localStorage.getItem("token");
    try {
      await axios.delete(`/api/chatrooms/${roomId}/delete`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      alert("채팅방을 떠났습니다.");
      navigate("/chatList");
    } catch (error: any) {
      alert("채팅방 나가기 실패");
    }
  };

  const handleGenerateInviteCode = async () => {
    const token = localStorage.getItem("token");
    try {
      const response = await axios.post<{ message: string; invite_code: string }>(
        `/api/invite/create`,
        { chatroom_id: roomId },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      alert(`초대코드: ${response.data.invite_code}`);
    } catch (error: any) {
      alert("초대코드 생성 실패");
    }
  };

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (isSidebarOpen && !target.closest("#sidebar")) {
        setIsSidebarOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [isSidebarOpen]);

  return (
    <Container>
      <Header>
        <Title>채팅방</Title>
        <Button onClick={() => setIsSidebarOpen(!isSidebarOpen)}>회원 목록 보기</Button>
        <Button onClick={handleLeaveChatRoom}>채팅방 나가기</Button>
      </Header>

      {isPrivate && creatorId === userId && <GenerateInviteButton onClick={handleGenerateInviteCode}>초대코드 발급</GenerateInviteButton>}

      <MessageContainer>
        {loading ? (
          <LoadingText>로딩 중...</LoadingText>
        ) : messages.length > 0 ? (
          messages.map((msg) => (
            <MessageWrapper key={msg.message_id} iscurrentuser={msg.user_id === userId ? "true" : undefined}>
              <Message iscurrentuser={msg.user_id === userId ? "true" : undefined}>
                <strong>{msg.username}:</strong> {msg.content}
                {msg.user_id === userId && <EditButton onClick={() => handleEditMessage(msg.message_id, msg.content)}>✏️</EditButton>}
              </Message>
            </MessageWrapper>
          ))
        ) : (
          <NoDataText>메시지가 없습니다.</NoDataText>
        )}
      </MessageContainer>

      <InputContainer>
        <Input
          type="text"
          placeholder={editingMessageId ? "메시지 수정..." : "메시지 입력..."}
          value={editingMessageId ? editingContent : content}
          onChange={(e) => {
            if (editingMessageId) {
              setEditingContent(e.target.value);
            } else {
              setContent(e.target.value);
            }
          }}
        />
        {editingMessageId ? (
          <SendButton onClick={handleUpdateMessage}>수정</SendButton>
        ) : (
          <SendButton onClick={handleSendMessage}>전송</SendButton>
        )}
      </InputContainer>

      {isSidebarOpen && (
        <Sidebar id="sidebar">
          <SidebarTitle>회원 목록</SidebarTitle>
          {loading ? (
            <LoadingText>로딩 중...</LoadingText>
          ) : members.length > 0 ? (
            members.map((member) => (
              <Member key={member.user_id}>
                {member.username} (joined: {new Date(member.joined_at).toLocaleDateString()})
              </Member>
            ))
          ) : (
            <NoDataText>회원이 없습니다.</NoDataText>
          )}
        </Sidebar>
      )}
    </Container>
  );
};

export default ChatRoom;

const EditButton = styled.button`
  margin-left: 10px;
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 1.2rem;
  &:hover {
    color: #0056b3;
  }
`;

const GenerateInviteButton = styled.button`
  margin: 1rem;
  padding: 0.5rem 1rem;
  background-color: #ffcc00;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  &:hover {
    background-color: #e6b800;
  }
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0f2f5;
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #007bff;
  color: #fff;
`;

const Title = styled.h1`
  font-size: 1.5rem;
  margin: 0;
`;

const Button = styled.button`
  padding: 0.5rem 1rem;
  color: #007bff;
  background-color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #e6f7ff;
  }
`;

const MessageContainer = styled.div`
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`;

const MessageWrapper = styled.div<{ iscurrentuser?: string }>`
  display: flex;
  justify-content: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "flex-end" : "flex-start")};
`;

const Message = styled.div<{ iscurrentuser?: string }>`
  padding: 0.5rem;
  background-color: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "#28a745" : "#e9ecef")};
  color: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "#fff" : "#000")};
  border-radius: 4px;
  max-width: 70%;
  word-wrap: break-word;
`;

const InputContainer = styled.div`
  display: flex;
  padding: 1rem;
  background-color: #f8f9fa;
  border-top: 1px solid #ddd;
`;

const Input = styled.input`
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-right: 0.5rem;
  font-size: 1rem;
  outline: none;
`;

const SendButton = styled.button`
  padding: 0.5rem 1rem;
  background-color: #28a745;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #218838;
  }
`;

const Sidebar = styled.div`
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 300px;
  background-color: #fff;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  overflow-y: auto;
`;

const SidebarTitle = styled.h2`
  margin-top: 0;
`;

const Member = styled.div`
  padding: 0.5rem;
  margin: 0.5rem 0;
  background-color: #f8f9fa;
  border-radius: 4px;
`;

const NoDataText = styled.div`
  text-align: center;
  font-size: 1.2rem;
  color: #888;
`;

const LoadingText = styled.div`
  text-align: center;
  font-size: 1.2rem;
  color: #007bff;
`;
