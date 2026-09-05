package com.example.urlshortener.web;

import com.example.urlshortener.service.AliasAlreadyExistsException;
import com.example.urlshortener.service.CodeGenerationException;
import com.example.urlshortener.service.InvalidAliasException;
import com.example.urlshortener.service.InvalidUrlException;
import com.example.urlshortener.service.ShortUrlNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates service/validation exceptions into RFC 7807 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onBeanValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(InvalidUrlException.class)
    ProblemDetail onInvalidUrl(InvalidUrlException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    ProblemDetail onNotFound(ShortUrlNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidAliasException.class)
    ProblemDetail onInvalidAlias(InvalidAliasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    ProblemDetail onAliasTaken(AliasAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CodeGenerationException.class)
    ProblemDetail onCodeGenerationExhausted(CodeGenerationException ex) {
        log.error("Short-code generation exhausted", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a short code, please retry");
    }
}
