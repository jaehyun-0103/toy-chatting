import React, { useState, useEffect, useCallback, useRef } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Swal from "sweetalert2";
import { BiListUl, BiCommentAdd, BiLogOut } from "react-icons/bi";
import { MdOutlineChatBubbleOutline, MdExitToApp } from "react-icons/md";
import { FcInvite } from "react-icons/fc";
import { FaLock } from "react-icons/fa";

interface RoomPageResponse {
  items: any[];
  hasNext: boolean;
  joinedRoomIds: number[];
}

// 채팅방 목록
const ChatList = () => {
  const navigate = useNavigate();

  const [chatRooms, setChatRooms] = useState<any[]>([]);
  const [myChatRooms, setMyChatRooms] = useState<any[]>([]);
  const [selectedTab, setSelectedTab] = useState<string>("all");
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [inviteCode, setInviteCode] = useState<string>("");
  const [allPage, setAllPage] = useState(0);
  const [myPage, setMyPage] = useState(0);
  const [allHasNext, setAllHasNext] = useState(false);
  const [myHasNext, setMyHasNext] = useState(false);
  const [joinedRoomIds, setJoinedRoomIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const requestVersionRef = useRef(0);

  const fetchChatRooms = useCallback(async () => {
    const token = localStorage.getItem("token");
    const version = ++requestVersionRef.current;
    setLoading(true);

    try {
      const [allResponse, myResponse] = await Promise.all([
        axios.get<RoomPageResponse>("/api/chatrooms", {
          params: { page: allPage, size: 20 },
          headers: { Authorization: `Bearer ${token}` },
        }),
        axios.get<RoomPageResponse>("/api/chatrooms/lists", {
          params: { page: myPage, size: 20 },
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      if (version !== requestVersionRef.current) {
        return;
      }

      setChatRooms(allResponse.data.items);
      setMyChatRooms(myResponse.data.items);
      setAllHasNext(allResponse.data.hasNext);
      setMyHasNext(myResponse.data.hasNext);
      setJoinedRoomIds(allResponse.data.joinedRoomIds);
    } catch (error: any) {
      if (version === requestVersionRef.current) {
        Swal.fire({
          icon: "error",
          title: "목록 불러오기 실패",
          text: error.response?.data?.message || error.message,
        });
      }
    } finally {
      if (version === requestVersionRef.current) {
        setLoading(false);
      }
    }
  }, [allPage, myPage]);

  useEffect(() => {
    void fetchChatRooms();

    return () => {
      requestVersionRef.current += 1;
    };
  }, [fetchChatRooms]);

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
      const retryAfter = Number(error.response?.headers?.["retry-after"]);

      const message =
        error.response?.status === 429
          ? Number.isFinite(retryAfter) && retryAfter > 0
            ? `초대코드 참여 요청이 너무 많습니다. 약 ${retryAfter}초 후 다시 시도해주세요.`
            : "초대코드 참여 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
          : error.response?.data?.message || error.message;

      Swal.fire({
        icon: "error",
        title: "초대코드 입력 실패",
        text: message,
      });
    }
  };

  const handleChatRoomClick = (room: any) => {
    localStorage.setItem("creator_id", room.creator_id.toString());
    localStorage.setItem("is_private", room.is_private.toString());
    localStorage.setItem("title", room.title.toString());

    navigate(`/chatroom/${room.chatroom_id}`);
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
        {loading ? (
          <p>목록을 불러오는 중...</p>
        ) : selectedTab === "all" ? (
          <>
            <h2>전체 채팅방</h2>
            <ChatRoomList>
              {chatRooms.map((chatRoom) => {
                const isMember = joinedRoomIds.includes(chatRoom.chatroom_id);

                return (
                  <ChatRoomItem key={chatRoom.chatroom_id} onClick={() => isMember && handleChatRoomClick(chatRoom)} disabled={!isMember}>
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
              {myChatRooms.map((chatRoom) => (
                <ChatRoomItem key={chatRoom.chatroom_id} onClick={() => handleChatRoomClick(chatRoom)} disabled={false}>
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
        {!loading && (selectedTab === "all" ? chatRooms : myChatRooms).length === 0 && <p>표시할 채팅방이 없습니다.</p>}

        <div style={{ display: "flex", gap: 12, alignItems: "center", marginTop: 20 }}>
          <button
            type="button"
            disabled={loading || (selectedTab === "all" ? allPage : myPage) === 0}
            onClick={() => {
              setLoading(true);

              if (selectedTab === "all") {
                setAllPage((page) => page - 1);
              } else {
                setMyPage((page) => page - 1);
              }
            }}
          >
            이전
          </button>

          <span>{(selectedTab === "all" ? allPage : myPage) + 1} 페이지</span>

          <button
            type="button"
            disabled={loading || !(selectedTab === "all" ? allHasNext : myHasNext)}
            onClick={() => {
              setLoading(true);

              if (selectedTab === "all") {
                setAllPage((page) => page + 1);
              } else {
                setMyPage((page) => page + 1);
              }
            }}
          >
            다음
          </button>
        </div>
      </MainContent>

      {isModalOpen && (
        <ModalOverlay>
          <ModalContent>
            <h3>초대 코드 입력</h3>
            <ModalInput
              type="text"
              placeholder="초대 코드 입력 (신규 8자리)"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
            />
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
