package com.example.toychat.scheduler;

import com.example.toychat.repository.InviteCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class InviteCodeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InviteCodeScheduler.class);

    private final InviteCodeRepository inviteCodeRepository;

    // 생성자 주입
    public InviteCodeScheduler(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    // 1분마다 초대 코드 만료 시 삭제
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredInviteCodes() {
        // 만료된 초대 코드 배치 삭제 호출
        int deletedCount = inviteCodeRepository.deleteExpiredInviteCodes(LocalDateTime.now());

        if (deletedCount > 0) {
            logger.info("Expired invite codes deleted: {}", deletedCount);
        }
    }
}