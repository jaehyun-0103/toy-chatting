import React, { useState, useEffect } from "react";
import styled from "styled-components";
import axios from "axios";
import { useNavigate } from "react-router-dom";

// 채팅방 목록
const ChatList = () => {
  const [chatRooms, setChatRooms] = useState<any[]>([]);
  const [myChatRooms, setMyChatRooms] = useState<any[]>([]);
  const [selectedTab, setSelectedTab] = useState<string>("all");
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [inviteCode, setInviteCode] = useState<string>("");
  const navigate = useNavigate();

  const fetchChatRooms = async () => {
    const token = localStorage.getItem("token");

    try {
      const allChatRoomsResponse = await axios.get("/api/chatrooms", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setChatRooms(allChatRoomsResponse.data);

      const myChatRoomsResponse = await axios.get("/api/chatrooms/lists", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setMyChatRooms(myChatRoomsResponse.data);
    } catch (error) {
      alert("채팅방 목록을 불러오는 데 실패했습니다.");
    }
  };

  const handleJoinChatRoom = async (chatroom_id: number) => {
    const token = localStorage.getItem("token");

    try {
      const response = await axios.post("/api/chatrooms/join", { chatroom_id }, { headers: { Authorization: `Bearer ${token}` } });
      alert(response.data.message);
      fetchChatRooms();
    } catch (error) {
      alert("채팅방 참여에 실패했습니다.");
    }
  };

  const handleJoinWithInviteCode = async () => {
    const token = localStorage.getItem("token");

    try {
      const response = await axios.post("/api/invite/join", { invite_code: inviteCode }, { headers: { Authorization: `Bearer ${token}` } });
      alert(response.data.message);
      setIsModalOpen(false);
      fetchChatRooms();
    } catch (error) {
      alert("초대 코드로 참여에 실패했습니다.");
    }
  };

  const handleChatRoomClick = (chatroom_id: number) => {
    const selectedChatRoom = myChatRooms.find((room) => room.chatroom_id === chatroom_id);

    if (selectedChatRoom) {
      localStorage.setItem("creator_id", selectedChatRoom.creator_id.toString());
      localStorage.setItem("is_private", selectedChatRoom.is_private.toString());

      navigate(`/chatroom/${chatroom_id}`);
    } else {
      const selectedAllChatRoom = chatRooms.find((room) => room.chatroom_id === chatroom_id);

      if (selectedAllChatRoom) {
        localStorage.setItem("creator_id", selectedAllChatRoom.creator_id.toString());
        localStorage.setItem("is_private", selectedAllChatRoom.is_private.toString());

        navigate(`/chatroom/${chatroom_id}`);
      } else {
        alert("해당 채팅방을 찾을 수 없습니다.");
      }
    }
  };

  const handleCreateChatRoom = () => {
    navigate("/createChatRoom");
  };

  const handleDeleteUser = async () => {
    const token = localStorage.getItem("token");

    try {
      const response = await axios.delete("/api/delete", {
        headers: { Authorization: `Bearer ${token}` },
      });
      alert(response.data.message);
      localStorage.removeItem("token");
      navigate("/");
    } catch (error) {
      alert("사용자 탈퇴에 실패했습니다.");
    }
  };

  useEffect(() => {
    fetchChatRooms();
  }, []);

  return (
    <Container>
      <Sidebar>
        <SidebarButton onClick={() => setSelectedTab("all")}>전체 채팅방</SidebarButton>
        <SidebarButton onClick={() => setSelectedTab("my")}>내 채팅방</SidebarButton>
        <CreateChatRoomButton onClick={handleCreateChatRoom}>채팅방 생성</CreateChatRoomButton>
        <DeleteUserButton onClick={handleDeleteUser}>사용자 탈퇴</DeleteUserButton>
      </Sidebar>

      <MainContent>
        {selectedTab === "all" ? (
          <>
            <h2>전체 채팅방 목록</h2>
            <ChatRoomList>
              {chatRooms.map((chatRoom) => {
                const isMember = myChatRooms.some((room) => room.chatroom_id === chatRoom.chatroom_id);

                return (
                  <ChatRoomItem
                    key={chatRoom.chatroom_id}
                    onClick={() => isMember && handleChatRoomClick(chatRoom.chatroom_id)}
                    disabled={!isMember}
                  >
                    <ChatRoomInfo>
                      <span>{chatRoom.title}</span>
                      <span>최대 인원: {chatRoom.max_members}</span>
                      <span>현재 인원: {chatRoom.current_members}</span>
                    </ChatRoomInfo>
                    {isMember ? (
                      <JoinButton disabled>참여됨</JoinButton>
                    ) : (
                      <JoinButton
                        onClick={(e) => {
                          e.stopPropagation();
                          handleJoinChatRoom(chatRoom.chatroom_id);
                        }}
                      >
                        참여
                      </JoinButton>
                    )}
                  </ChatRoomItem>
                );
              })}
            </ChatRoomList>
          </>
        ) : (
          <>
            <h2>내 채팅방 목록</h2>
            <ChatRoomList>
              {myChatRooms.map((chatRoom) => (
                <ChatRoomItem key={chatRoom.chatroom_id} onClick={() => handleChatRoomClick(chatRoom.chatroom_id)} disabled={false}>
                  <ChatRoomInfo>
                    <span>{chatRoom.title}</span>
                    <span>최대 인원: {chatRoom.max_members}</span>
                    <span>현재 인원: {chatRoom.current_members}</span>
                  </ChatRoomInfo>
                  <JoinButton disabled>참여됨</JoinButton>
                </ChatRoomItem>
              ))}
            </ChatRoomList>
          </>
        )}

        <InviteCodeButton onClick={() => setIsModalOpen(true)}>초대 코드로 참여</InviteCodeButton>
      </MainContent>

      {isModalOpen && (
        <ModalOverlay>
          <ModalContent>
            <h3>초대 코드 입력</h3>
            <ModalInput type="text" placeholder="초대 코드 입력" value={inviteCode} onChange={(e) => setInviteCode(e.target.value)} />
            <ModalButtons>
              <ModalButton onClick={handleJoinWithInviteCode}>확인</ModalButton>
              <ModalButton onClick={() => setIsModalOpen(false)}>취소</ModalButton>
            </ModalButtons>
          </ModalContent>
        </ModalOverlay>
      )}
    </Container>
  );
};

export default ChatList;

const Container = styled.div`
  display: flex;
`;

const Sidebar = styled.div`
  width: 200px;
  padding: 20px;
  background-color: #f8f9fa;
`;

const SidebarButton = styled.button`
  width: 100%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  margin-bottom: 10px;
  cursor: pointer;
  &:hover {
    background-color: #0056b3;
  }
`;

const CreateChatRoomButton = styled.button`
  width: 100%;
  padding: 10px;
  background-color: #28a745;
  color: white;
  border: none;
  margin-top: 10px;
  cursor: pointer;
  &:hover {
    background-color: #218838;
  }
`;

const DeleteUserButton = styled.button`
  width: 100%;
  padding: 10px;
  background-color: #dc3545;
  color: white;
  border: none;
  margin-top: 10px;
  cursor: pointer;
  &:hover {
    background-color: #c82333;
  }
`;

const MainContent = styled.div`
  flex: 1;
  padding: 20px;
`;

const ChatRoomList = styled.div`
  margin-top: 20px;
`;

const ChatRoomItem = styled.div<{ disabled: boolean }>`
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background-color: #f1f1f1;
  margin-bottom: 10px;
  border-radius: 5px;
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "pointer")};
  opacity: ${({ disabled }) => (disabled ? 0.6 : 1)};
`;

const ChatRoomInfo = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`;

const JoinButton = styled.button`
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 5px;
  &:disabled {
    background-color: #ccc;
    cursor: not-allowed;
  }
`;

const InviteCodeButton = styled.button`
  padding: 10px;
  background-color: #28a745;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 5px;
  margin-top: 20px;
  &:hover {
    background-color: #218838;
  }
`;

const ModalOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
`;

const ModalContent = styled.div`
  background-color: white;
  padding: 20px;
  border-radius: 10px;
  width: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const ModalInput = styled.input`
  padding: 10px;
  width: 100%;
  margin-top: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
`;

const ModalButtons = styled.div`
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  width: 100%;
`;

const ModalButton = styled.button`
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 5px;
  &:hover {
    background-color: #0056b3;
  }
`;
