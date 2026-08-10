package com.microservices.userservice.web;

import com.microservices.userservice.messaging.UserEventPublisher;
import com.microservices.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserService.DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(UserService.DuplicateEmailException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Duplicate Email");
        return detail;
    }

    @ExceptionHandler(UserService.InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(UserService.InvalidCredentialsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        detail.setTitle("Authentication Failed");
        return detail;
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ProblemDetail handleNotFound(UserService.UserNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("User Not Found");
        return detail;
    }

    @ExceptionHandler(UserEventPublisher.EventPublishException.class)
    public ProblemDetail handleEventPublish(UserEventPublisher.EventPublishException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to publish event. Please retry."
        );
        detail.setTitle("Messaging Error");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Validation Failed");
        return detail;
    }
}
