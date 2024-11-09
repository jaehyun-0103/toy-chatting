package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    @JsonProperty("message_id")
    private Long messageId;

    private String username;

    private String content;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

}
