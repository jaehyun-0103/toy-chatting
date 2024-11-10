package com.example.toychat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendResponseDTO {

    private String message;

    @JsonProperty("message_id")
    private Long messageId;
}
