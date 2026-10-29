package com.inspire.educlasses.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ExceptionResponse {

    private LocalDateTime time;
    private Integer statusCode;
    private String error;
    private String message;
    private String path;

}
