import React, { useState, useEffect } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Swal from "sweetalert2";
import { BiListUl, BiCommentAdd, BiLogOut } from "react-icons/bi";
import { MdOutlineChatBubbleOutline, MdExitToApp } from "react-icons/md";
import { FcInvite } from "react-icons/fc";
import { FaLock } from "react-icons/fa";

// 채팅방 목록
const ChatList = () => {
  const navigate = useNavigate();

  const [chatRooms, setChatRooms] = useState<any[]>([]);
  const [myChatRooms, setMyChatRooms] = useState<any[]>([]);
  const [selectedTab, setSelectedTab] = useState<string>("all");
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [inviteCode, setInviteCode] = useState<string>("");

  useEffect(() => {
    fetchChatRooms();
  }, []);

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
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "목록 불러오기 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleJoinChatRoom = async (chatroom_id: number) => {
    const token = localStorage.getItem("token");

    try {
      const response = await axios.post("/api/chatrooms/join", { chatroom_id }, { headers: { Authorization: `Bearer ${token}` } });
      Swal.fire({
        icon: "success",
        title: "참여 성공",
        text: response.data.message,
      });
      fetchChatRooms();
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "참여 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleJoinWithInviteCode = async () => {
    const token = localStorage.getItem("token");

    try {
      const response = await axios.post("/api/invite/join", { invite_code: inviteCode }, { headers: { Authorization: `Bearer ${token}` } });
      Swal.fire({
        icon: "success",
        title: "초대코드 입력 성공",
        text: response.data.message,
      });
      setIsModalOpen(false);
      fetchChatRooms();
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "초대코드 입력 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleChatRoomClick = (chatroom_id: number) => {
    const selectedChatRoom = myChatRooms.find((room) => room.chatroom_id === chatroom_id);

    if (selectedChatRoom) {
      localStorage.setItem("creator_id", selectedChatRoom.creator_id.toString());
      localStorage.setItem("is_private", selectedChatRoom.is_private.toString());
      localStorage.setItem("title", selectedChatRoom.title.toString());

      navigate(`/chatroom/${chatroom_id}`);
    } else {
      const selectedAllChatRoom = chatRooms.find((room) => room.chatroom_id === chatroom_id);

      if (selectedAllChatRoom) {
        localStorage.setItem("creator_id", selectedAllChatRoom.creator_id.toString());
        localStorage.setItem("is_private", selectedAllChatRoom.is_private.toString());
        localStorage.setItem("title", selectedChatRoom.title.toString());

        navigate(`/chatroom/${chatroom_id}`);
      } else {
        Swal.fire({
          icon: "error",
          title: "채팅방 로딩 실패",
          text: "Chatting room not found",
        });
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
      Swal.fire({
        icon: "success",
        title: "탈퇴 성공",
        text: response.data.message,
      });
      localStorage.removeItem("token");
      navigate("/");
    } catch (error: any) {
      Swal.fire({
        icon: "error",
        title: "탈퇴 실패",
        text: error.response?.data?.message || error.message,
      });
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <Container>
      <Sidebar>
        <SidebarItem onClick={() => setSelectedTab("all")}>
          <BiListUl size={24} />
          <span>전체 채팅방</span>
        </SidebarItem>
        <SidebarItem onClick={() => setSelectedTab("my")}>
          <MdOutlineChatBubbleOutline size={24} />
          <span>내 채팅방</span>
        </SidebarItem>
        <SidebarItem onClick={() => setIsModalOpen(true)}>
          <FcInvite size={24} />
          <span>초대 코드</span>
        </SidebarItem>
        <SidebarItem onClick={handleCreateChatRoom}>
          <BiCommentAdd size={24} />
          <span>채팅방 생성</span>
        </SidebarItem>
        <SidebarItem onClick={handleLogout}>
          <BiLogOut size={24} />
          <span>로그아웃</span>
        </SidebarItem>
        <SidebarItem onClick={handleDeleteUser}>
          <MdExitToApp size={24} />
          <span>사용자 탈퇴</span>
        </SidebarItem>
      </Sidebar>

      <MainContent>
        {selectedTab === "all" ? (
          <>
            <h2>전체 채팅방</h2>
            <ChatRoomList>
              {chatRooms
                .slice()
                .reverse()
                .map((chatRoom) => {
                  const isMember = myChatRooms.some((room) => room.chatroom_id === chatRoom.chatroom_id);

                  return (
                    <ChatRoomItem
                      key={chatRoom.chatroom_id}
                      onClick={() => isMember && handleChatRoomClick(chatRoom.chatroom_id)}
                      disabled={!isMember}
                    >
                      <ChatRoomInfo>
                        <span>{chatRoom.title}</span>
                        <span>
                          인원수 : {chatRoom.current_members} / {chatRoom.max_members}
                        </span>
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
            <h2>내 채팅방</h2>
            <ChatRoomList>
              {myChatRooms
                .slice()
                .reverse()
                .map((chatRoom) => (
                  <ChatRoomItem key={chatRoom.chatroom_id} onClick={() => handleChatRoomClick(chatRoom.chatroom_id)} disabled={false}>
                    <ChatRoomInfo>
                      <span>
                        {chatRoom.is_private && <FaLock style={{ marginRight: "5px" }} />}
                        {chatRoom.title}
                      </span>
                      <span>
                        인원수 : {chatRoom.current_members}/{chatRoom.max_members}
                      </span>
                    </ChatRoomInfo>
                    <JoinButton disabled>참여됨</JoinButton>
                  </ChatRoomItem>
                ))}
            </ChatRoomList>
          </>
        )}
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
  width: 80px;
  background-color: #007bff;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: fixed;
  height: 100vh;
  padding: 20px 0;
`;

const SidebarItem = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  color: white;
  cursor: pointer;
  font-size: 12px;
  text-align: center;
  &:hover {
    color: #0056b3;
  }
`;

const MainContent = styled.div`
  flex: 1;
  margin-left: 80px;
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
  text-align: left;
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
