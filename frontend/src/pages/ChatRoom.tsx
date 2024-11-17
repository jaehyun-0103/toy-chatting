import React, { useState, useEffect, useRef } from "react";
import styled from "styled-components";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { FaUsers, FaKey, FaEdit, FaCrown, FaAngleDoubleDown } from "react-icons/fa";
import { GiExitDoor } from "react-icons/gi";
import { BiArrowBack } from "react-icons/bi";
import Swal from "sweetalert2";
import { Stomp } from "@stomp/stompjs";

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
  const navigate = useNavigate();

  const { roomId } = useParams<{ roomId: string }>();
  const userId = parseInt(localStorage.getItem("user_id") || "0");
  const creatorId = parseInt(localStorage.getItem("creator_id") || "0");
  const isPrivate = localStorage.getItem("is_private") === "true";
  const title = localStorage.getItem("title");

  const [loading, setLoading] = useState<boolean>(false);
  const [connected, setConnected] = useState<boolean>(false);
  const [client, setClient] = useState<any>(null);

  const [messages, setMessages] = useState<MessageType[]>([]);
  const [content, setContent] = useState<string>("");
  const [members, setMembers] = useState<MemberType[]>([]);

  const [editingContent, setEditingContent] = useState<string>("");
  const [editingMessageId, setEditingMessageId] = useState<number | null>(null);
  const [visibleEditButtons, setVisibleEditButtons] = useState<{ [key: number]: boolean }>({});

  const [issidebaropen, setIsSidebarOpen] = useState<boolean>(false);

  const messageEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchMessagesAndMembers = async () => {
      const token = localStorage.getItem("token");

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
        Swal.fire({
          icon: "error",
          title: "채팅방 로딩 실패",
          text: error.response?.data?.message || error.message,
        });
      } finally {
        setLoading(false);
        scrollToBottom();
      }
    };

    fetchMessagesAndMembers();

    const stompClient = Stomp.client("ws://localhost:8080/ws");

    stompClient.debug = () => {};

    stompClient.connect(
      {},
      () => {
        setConnected(true);

        stompClient.subscribe(`/topic/public/${roomId}`, (message: any) => {
          const { username, content, message_id, user_id, updated_at } = JSON.parse(message.body);

          const newMessage: MessageType = {
            username: username,
            content: content,
            message_id: message_id,
            user_id: user_id,
            updated_at: updated_at,
          };

          setMessages((prev) => [...prev, newMessage]);
        });
      },
      (error: string) => {
        console.error("STOMP connection error:", error);
      }
    );

    setClient(stompClient);

    return () => {
      stompClient.disconnect(() => console.log("Disconnected"));
    };
  }, [roomId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSendMessage = async () => {
    const token = localStorage.getItem("token");

    const url = `/app/chat.sendMessage/${roomId}`;
    client.send(
      url,
      {
        Authorization: `Bearer ${token}`,
      },
      JSON.stringify({
        content: content,
      })
    );

    setContent("");
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
        Swal.fire({
          icon: "success",
          title: "수정 성공",
          text: response.data.message,
        });
        setMessages(
          messages.map((msg) =>
            msg.message_id === editingMessageId ? { ...msg, content: editingContent, updated_at: new Date().toISOString() } : msg
          )
        );
        setEditingMessageId(null);
        setEditingContent("");
      } catch (error: any) {
        Swal.fire({
          icon: "error",
          title: "메시지 수정 실패",
          text: error.response?.data?.message || error.message,
        });
      }
    }
  };

  const handleBackButtonClick = () => {
    navigate("/chatList");
  };

  const handleLeaveChatRoom = async () => {
    const token = localStorage.getItem("token");
    try {
      const response = await axios.delete(`/api/chatrooms/${roomId}/delete`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      Swal.fire({
        icon: "success",
        title: "채팅방 탈퇴 성공",
        text: response.data.message,
      });
      navigate("/chatList");
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "채팅방 탈퇴 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const scrollToBottom = () => {
    if (messageEndRef.current) {
      messageEndRef.current.scrollIntoView({ behavior: "auto" });
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
      Swal.fire({
        icon: "info",
        title: "초대코드 생성 성공",
        text: response.data.invite_code,
      });
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "초대코드 생성 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleCancelEdit = () => {
    setEditingMessageId(null);
    setEditingContent("");
  };

  const handleToggleSidebar = () => {
    setIsSidebarOpen((prevState) => !prevState);
  };

  const toggleEditButton = (messageId: number) => {
    setVisibleEditButtons((prev) => ({
      ...prev,
      [messageId]: !prev[messageId],
    }));
  };

  return (
    <Container issidebaropen={issidebaropen ? "true" : "false"}>
      <Header>
        <IconButtonWrapper>
          <IconButton1 onClick={handleBackButtonClick}>
            <BiArrowBack />
          </IconButton1>
          {isPrivate && creatorId === userId && (
            <IconButton1 onClick={handleGenerateInviteCode}>
              <FaKey style={{ color: "#FFCC00" }} />
            </IconButton1>
          )}
        </IconButtonWrapper>
        <TitleWrapper>
          <Title>{title}</Title>
        </TitleWrapper>
        <IconButtonWrapper>
          <IconButton1 onClick={handleToggleSidebar}>
            <FaUsers />
          </IconButton1>
          <IconButton1 onClick={handleLeaveChatRoom}>
            <GiExitDoor />
          </IconButton1>
        </IconButtonWrapper>
      </Header>

      <MessageContainer>
        {loading && connected ? (
          <LoadingText>로딩 중...</LoadingText>
        ) : messages.length > 0 ? (
          messages.map((msg) => (
            <React.Fragment key={msg.message_id}>
              {msg.user_id !== userId && <Username>{msg.username}</Username>}
              <MessageWrapper key={msg.message_id} iscurrentuser={msg.user_id === userId ? "true" : undefined}>
                {msg.user_id === userId && visibleEditButtons[msg.message_id] && (
                  <EditButton onClick={() => handleEditMessage(msg.message_id, msg.content)}>
                    <FaEdit />
                  </EditButton>
                )}
                <Message iscurrentuser={msg.user_id === userId ? "true" : undefined} onClick={() => toggleEditButton(msg.message_id)}>
                  {msg.content}
                  <br></br>
                  <br></br>
                  <MessageDate>{new Date(msg.updated_at).toLocaleTimeString()}</MessageDate>
                </Message>
              </MessageWrapper>
            </React.Fragment>
          ))
        ) : (
          <NoDataText>메시지가 없습니다.</NoDataText>
        )}
        <div ref={messageEndRef} />
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
          onKeyPress={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              if (editingMessageId) {
                handleUpdateMessage();
              } else {
                handleSendMessage();
              }
            }
          }}
        />
        {editingMessageId ? (
          <>
            <SendButton onClick={handleUpdateMessage}>수정</SendButton>
            <CancelButton onClick={handleCancelEdit}>취소</CancelButton>
          </>
        ) : (
          <SendButton onClick={handleSendMessage}>전송</SendButton>
        )}
        <IconButton2 onClick={scrollToBottom}>
          <FaAngleDoubleDown />
        </IconButton2>
      </InputContainer>

      {issidebaropen && (
        <Sidebar issidebaropen={issidebaropen ? "true" : "false"}>
          <SidebarTitle>회원 목록</SidebarTitle>
          {loading ? (
            <LoadingText>로딩 중...</LoadingText>
          ) : members.length > 0 ? (
            members.map((member, index) => (
              <Member key={member.user_id}>
                <MemberInfo>
                  <MemberName>
                    {index === 0 && <CrownIcon />}
                    {member.username}
                  </MemberName>
                  <MemberDate>가입일: {new Date(member.joined_at).toLocaleDateString()}</MemberDate>
                </MemberInfo>
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

const Container = styled.div<{ issidebaropen: string }>`
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0f2f5;
  transition: margin-right 0.3s ease-in-out;
  margin-right: ${({ issidebaropen }) => (issidebaropen === "true" ? "330px" : "0")};
`;

const Header = styled.header`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #007bff;
  color: #fff;
`;

const IconButtonWrapper = styled.div`
  display: flex;
  gap: 3px;
  align-items: center;
`;

const IconButton1 = styled.button`
  display: flex;
  align-items: center;
  padding: 0.5rem 0.5rem;
  color: #fff;
  background-color: #007bff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1.4rem;
  margin-left: 0.3rem;
  margin-right: 0.3rem;
  &:hover {
    background-color: #0056b3;
  }
`;

const TitleWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  flex-grow: 1;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
`;

const Title = styled.h1`
  font-size: 1.5rem;
  margin: 0;
`;

const MessageContainer = styled.div`
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`;

const LoadingText = styled.div`
  text-align: center;
  font-size: 1.2rem;
  color: #007bff;
`;

const Username = styled.span`
  font-size: 1rem;
  text-align: left;
  margin-left: 0.3rem;
  margin-bottom: -0.5rem;
`;

const MessageWrapper = styled.div<{ iscurrentuser?: string }>`
  display: flex;
  justify-content: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "flex-end" : "flex-start")};
`;

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

const Message = styled.div<{ iscurrentuser?: string }>`
  padding: 0.5rem;
  background-color: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "#4e9df2" : "#d5d8db")};
  color: ${({ iscurrentuser }) => (iscurrentuser === "true" ? "#f5f6fc" : "#000")};
  border-radius: 20px;
  max-width: 45%;
  word-wrap: break-word;
  text-align: left;
`;

const MessageDate = styled.span`
  font-size: 0.8rem;
  color: #343a40;
  margin-right: 10rem;
`;

const NoDataText = styled.div`
  text-align: center;
  font-size: 1.2rem;
  color: #888;
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
  background-color: #007bff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #0056b3;
  }
`;

const CancelButton = styled.button`
  padding: 0.5rem 1rem;
  background-color: #6c757d;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 0.5rem;
  &:hover {
    background-color: #5a6268;
  }
`;

const IconButton2 = styled.button`
  display: flex;
  align-items: center;
  padding: 0.5rem 0.5rem;
  color: #fff;
  background-color: #808080;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1.4rem;
  margin-left: 0.3rem;
  margin-right: 0.3rem;
  &:hover {
    background-color: #2d4157;
  }
`;

const Sidebar = styled.div<{ issidebaropen: string }>`
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 300px;
  background-color: #ffffff;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  overflow-y: auto;
  transition: transform 0.3s ease-in-out;
  transform: ${({ issidebaropen }) => (issidebaropen === "true" ? "translateX(0)" : "translateX(100%)")};
`;

const SidebarTitle = styled.h2`
  margin-top: 0;
  font-size: 1.5rem;
  color: #333;
  text-align: center;
  font-weight: 600;
`;

const Member = styled.div`
  display: flex;
  align-items: center;
  background-color: #f9f9f9;
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, background-color 0.3s ease;
  cursor: pointer;
  &:hover {
    background-color: #f1f1f1;
    transform: translateX(8px);
  }
`;

const MemberInfo = styled.div`
  flex: 1;
`;

const MemberName = styled.div`
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
`;

const MemberDate = styled.div`
  font-size: 0.9rem;
  color: #666;
`;

const CrownIcon = styled(FaCrown)`
  color: #ffd700;
  margin-right: 8px;
  font-size: 1.2rem;
`;
