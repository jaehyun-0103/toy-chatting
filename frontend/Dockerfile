# 기본 Node.js 이미지를 사용
FROM node:16-alpine AS build

# 작업 디렉토리 설정
WORKDIR /app

# package.json과 package-lock.json 복사
COPY package*.json ./

# 의존성 설치
RUN npm install

# 소스 코드 복사
COPY . .

# 개발 서버 실행
# CMD ["npm", "start"]

# React 애플리케이션 빌드
RUN NODE_OPTIONS="--max_old_space_size=8012" npm run build

# Nginx를 기반으로 설정
FROM nginx:alpine

# React 앱 빌드 결과물을 Nginx의 정적 파일 경로로 복사
COPY --from=build /app/build /usr/share/nginx/html

# Nginx 포트 열기
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]