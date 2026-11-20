package com.example.toychat.dto.response;

import lombok.Getter;

@Getter
public class MessageUpdateResponseDTO extends ResponseDTO {

    private final MessageResponseDTO data;

    public MessageUpdateResponseDTO(
            String message,
            MessageResponseDTO data
    ) {
        super(message);
        this.data = data;
    }
}