import http from 'k6/http';
import { check, sleep } from 'k6';

// 환경 설정
export let options = {
    stages: [
        { duration: '1m', target: 5 },
        { duration: '5m', target: 10 },
        { duration: '1m', target: 0 },
    ],
};

// 테스트 시작
export default function () {
    register();

    login();

    deleteUser();

    sleep(1); // 1초 대기
}

let username;
let password;
let email;
let authToken;

// 회원가입
function register() {
    username = `test-${Date.now()}-${Math.random().toString(36).substring(2)}@naver.com`;
    password = 'testpassword'
    email = `test-${Date.now()}-${Math.random().toString(36).substring(2)}@naver.com`

    let registerPayload = JSON.stringify({
        username: username,
        password: password,
        email: email,
    });

    let registerRes = http.post('http://spring:8080/api/register', registerPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(registerRes, {
        '회원가입 성공': (r) => r.status === 201,
    });
}

// 로그인
function login() {
    let loginPayload = JSON.stringify({
        email: email,
        password: password,
    });

    let loginRes = http.post('http://spring:8080/api/login', loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(loginRes, {
        '로그인 성공': (r) => r.status === 200,
    });

    let responseBody = JSON.parse(loginRes.body);
    authToken = responseBody.token;
}

// 회원탈퇴
function deleteUser() {
    let deleteRes = http.del('http://spring:8080/api/delete', null, {
        headers: { 'Authorization': `Bearer ${authToken}` },
    });

    check(deleteRes, {
        '사용자 탈퇴 성공': (r) => r.status === 200,
    });
}