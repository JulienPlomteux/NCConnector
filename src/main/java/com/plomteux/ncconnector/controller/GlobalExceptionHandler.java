package com.plomteux.ncconnector.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        System.err.println(ex.getMessage());
        return new ResponseEntity<>("An internal server error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
