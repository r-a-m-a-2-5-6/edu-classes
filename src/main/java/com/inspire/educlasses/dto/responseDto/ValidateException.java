package com.inspire.educlasses.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValidateException {

    private LocalDateTime time;
    private Integer statusCode;
    private String error;
    private String message;
    private String path;
    private Map<String,String> fieldErrors;

}
