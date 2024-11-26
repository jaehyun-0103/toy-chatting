# toy chatting

프로젝트 기간 : 2024.11.1. ~ 2024.11.25.

## 📢 Instruction

소규모 그룹 간의 원활한 소통과 협업을 지원하는 실시간 채팅 웹사이트입니다. 사용자들은 **공개 채팅방**과 **초대 코드**를 통해 참여할 수 있는 **비공개 채팅방** 중에서 선택할 수 있습니다. 누구나 쉽게 사용할 수 있도록 설계되었으며, 안전한 개인적 소통 공간도 제공합니다.

<hr>

## 📖 DEMO

### 공개 채팅방 생성

<img width="1200" src=".github\assets\gif\chatList_public.gif">

### 비공개 채팅방 생성

<img width="1200" src=".github\assets\gif\chatList_private.gif">

### 초대코드 발급

<img width="1200" src=".github\assets\gif\inviteCode.gif">

### 초대코드 입력

<img width="1200" src=".github\assets\gif\inviteCode_input.gif">

### 메시지 전송

<img width="1200" src=".github\assets\gif\chatting_send.gif">

### 메시지 수정

<img width="1200" src=".github\assets\gif\chatting_edit.gif">

<hr>

## 💻 System Architechture

<img width="800" src=".github\assets\architecture.png">

<hr>

## 🛠 Tech stack

분야| 사용 기술|
:--------:|:------------------------------:|
**Backend** | <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=Spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20WebSocket-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <br> <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
**Fronted** | <img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=TypeScript&logoColor=white"/> <img src="https://img.shields.io/badge/WebSocket%20API-FF6600?style=for-the-badge">
**DevOps** | <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/githubactions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"/> <img src="https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=nginx&logoColor=black"> <br> <img src="https://img.shields.io/badge/Amazon_EC2-FF9900?style=for-the-badge&logo=Amazon-EC2&logoColor=black"> <img src="https://img.shields.io/badge/Amazon RDS-527FFF?style=for-the-badge&logo=Amazon RDS&logoColor=white">
**Testing** | <img src="https://img.shields.io/badge/-JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"/> <img src="https://img.shields.io/badge/Mockito-FF9900?style=for-the-badge"> <img src="https://img.shields.io/badge/-k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white"/> <img src="https://img.shields.io/badge/-ZAP-00549E?style=for-the-badge&logo=zap&logoColor=white"/>
**etc** | <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=Notion&logoColor=white"/> <img src="https://img.shields.io/badge/postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white"/>

<hr>

## 💾 ERD

<img width="1200" src=".github\assets\erd.PNG">

<hr>

## 📚 API

<img width="600" src=".github\assets\api.PNG">

<hr>

## Testing

### K6

<img width="600" src=".github\assets\k6.PNG">

### Owasp Zap

<img width="1200" src=".github\assets\owasp zap.PNG">

<hr>

## 🧐 How to start

### Clone Repository

```
git clone https://github.com/jaehyun-0103/toy-chatting
```

### Set up .env in toy-chatting folder

```
# .env
MYSQL_URL=
MYSQL_USERNAME=
MYSQL_PASSWORD=
```

### Run Docker

- Local Development
  ```
  docker-compose build
  docker-compose up
  ```

- Production
  ```
  docker-compose -f docker-compose.prod.yml build
  docker-compose -f docker-compose.prod.yml up
  ```

- Testing
  ```
  docker-compose -f docker-compose.test.yml build
  docker-compose -f docker-compose.test.yml up
  ```
