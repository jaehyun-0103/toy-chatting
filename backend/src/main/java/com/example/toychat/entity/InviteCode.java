package com.example.toychat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "InviteCode")
@Getter
@Setter
@NoArgsConstructor
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "chatroom_id", unique = true)
    private ChatRoom chatRoom;

    @Column(name = "invite_code", nullable = false, unique = true, length = 50)
    private String inviteCode;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.expirationDate = this.createdAt.plusHours(1); // createdAt + 1h
    }
}
