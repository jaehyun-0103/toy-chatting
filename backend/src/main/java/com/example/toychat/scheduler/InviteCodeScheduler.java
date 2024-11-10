package com.example.toychat.scheduler;

import com.example.toychat.entity.InviteCode;

import com.example.toychat.repository.InviteCodeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InviteCodeScheduler {

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    // 1분마다 초대 코드 만료 시 삭제
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredInviteCodes() {
        LocalDateTime now = LocalDateTime.now();

        List<InviteCode> expiredInviteCodes = inviteCodeRepository.findByExpirationDate(now);

        if (!expiredInviteCodes.isEmpty()) {
            // 배치 삭제 호출
            inviteCodeRepository.deleteExpiredInviteCodes(now);
            System.out.println("만료된 초대코드가 삭제되었습니다.");
        }
    }
}

