package com.adobe.printservice.web;

import com.adobe.printservice.exception.UnknownTemplateException;
import com.adobe.printservice.exception.JobResultUnavailableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownTemplateException.class)
    ProblemDetail handleUnknownTemplate(UnknownTemplateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid status. Expected one of: QUEUED, PROCESSING, DONE, FAILED"
        );
    }

    @ExceptionHandler(JobResultUnavailableException.class)
    ProblemDetail handleResultUnavailable(JobResultUnavailableException exception) {
        HttpStatus status = exception.getJobStatus() == com.adobe.printservice.model.JobStatus.FAILED
                ? HttpStatus.UNPROCESSABLE_CONTENT
                : HttpStatus.CONFLICT;
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }
}
