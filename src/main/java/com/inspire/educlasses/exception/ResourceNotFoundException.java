package com.inspire.educlasses.exception;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(
            String message
    ){

        super(message);
    }
}
