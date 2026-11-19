package com.example.toychat.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class InviteJoinRateLimiter {

    private static final int MAX_ATTEMPTS = 10; // 윈도우 내 최대 허용 시도 횟수
    private static final int MAX_ENTRIES = 10_000; // 메모리에 저장할 최대 사용자 엔트리 수
    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1); // 제한 시간 윈도우 (1분)

    private final Map<Long, AttemptWindow> windows = new HashMap<>(); // 사용자 ID별 시도 횟수 및 윈도우 정보 저장소

    /**
     * 초대 코드 참여 요청의 속도 제한 여부를 확인합니다.
     * 요청을 허용하면 0, 제한하면 재시도까지 남은 초를 반환합니다.
     * 허용된 요청은 성공·실패와 관계없이 횟수에 포함됩니다.
     *
     * @param userId 요청한 사용자 ID
     * @return 허용된 경우 0, 제한된 경우 재시도까지 남은 대기 시간(초)
     */
    public synchronized long retryAfterIfLimited(Long userId) {
        long now = System.nanoTime(); // 현재 나노초 시각 측정
        AttemptWindow window = windows.get(userId); // 사용자의 기존 시도 윈도우 조회

        // 기존 윈도우가 유효 시간 내에 있는 경우
        if (window != null && now - window.startedAt < WINDOW_NANOS) {
            if (window.attempts >= MAX_ATTEMPTS) {
                long remaining = WINDOW_NANOS - (now - window.startedAt); // 남은 제한 시간 계산
                return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(remaining + TimeUnit.SECONDS.toNanos(1) - 1)); // 올림 처리하여 남은 초 반환
            }

            window.attempts++; // 허용 횟수 내인 경우 시도 횟수 증가
            return 0;
        }

        // 만료된 기존 윈도우 제거
        windows.remove(userId);

        // 저장소 최대 엔트리 수 초과 시 만료된 항목 정리
        if (windows.size() >= MAX_ENTRIES) {
            removeExpired(now);

            if (windows.size() >= MAX_ENTRIES) {
                return 1; // 정리 후에도 가득 찬 경우 일시 제한
            }
        }

        windows.put(userId, new AttemptWindow(now)); // 새로운 시도 윈도우 등록
        return 0;
    }

    /**
     * 주기적으로 만료된 시도 윈도우 항목을 정리합니다.
     */
    @Scheduled(fixedDelay = 60_000)
    public synchronized void removeExpiredWindows() {
        removeExpired(System.nanoTime()); // 현재 시각 기준으로 만료된 항목 제거
    }

    /**
     * 기준 시각을 초과해 만료된 윈도우 엔트리를 맵에서 제거합니다.
     *
     * @param now 기준 나노초 시각
     */
    private void removeExpired(long now) {
        windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_NANOS); // 윈도우 시간이 경과한 항목 삭제
    }

    /**
     * 사용자별 요청 시작 시각과 누적 시도 횟수를 보관하는 내부 클래스입니다.
     */
    private static final class AttemptWindow {

        private final long startedAt; // 윈도우 시작 시각 (나노초)
        private int attempts = 1; // 누적 시도 횟수

        /**
         * AttemptWindow 생성자입니다.
         *
         * @param startedAt 윈도우 시작 시각 (나노초)
         */
        private AttemptWindow(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}