package com.example.knowledgecollector.web.api;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return failure(HttpStatus.NOT_FOUND, "RES-404", exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleException exception,
            HttpServletRequest request
    ) {
        return failure(HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(),
                exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleBinding(
            Exception exception,
            HttpServletRequest request
    ) {
        var bindingResult = exception instanceof MethodArgumentNotValidException methodArgument
                ? methodArgument.getBindingResult()
                : ((BindException) exception).getBindingResult();
        List<FieldErrorDetail> errors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "VAL-001", "请求参数不符合要求", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorDetail> errors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "VAL-001", "请求参数不符合要求", errors, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String correlationId = correlationId(request);
        log.error("Unhandled request failure, correlationId={}", correlationId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(new ApiError("SYS-001", "系统内部错误，请根据关联编号查看日志"),
                        correlationId));
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            HttpStatus status,
            String code,
            String message,
            List<FieldErrorDetail> fieldErrors,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(new ApiError(code, message, fieldErrors), correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        return value == null ? "unknown" : value.toString();
    }
}
