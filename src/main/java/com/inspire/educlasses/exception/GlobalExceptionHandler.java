package com.inspire.educlasses.exception;


import com.inspire.educlasses.dto.responseDto.ExceptionResponse;
import com.inspire.educlasses.dto.responseDto.ValidateException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* Handles Resource Not Found Exception */
    @ExceptionHandler({ResourceNotFoundException.class,
    UsernameNotFoundException.class,
    NoResourceFoundException.class})
    public ResponseEntity<ExceptionResponse> handleResourceNotFoundException (Exception ex,
                                                                                 HttpServletRequest request){

        String errorMessage = ex.getMessage();

        if (ex instanceof NoResourceFoundException) {
            errorMessage = "The requested URL path does not exist on this server.";
        }

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.NOT_FOUND,
                errorMessage,
                request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exceptionResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionResponse> handleMethodNotAllowed (HttpRequestMethodNotSupportedException ex,
                                                                            HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(exceptionResponse);
    }

    /* Handles BadCredentials Exception */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsException (RuntimeException ex,
                                                                              HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exceptionResponse);
    }

    /*Handle connection failure Exception */
    @ExceptionHandler({ConnectException.class,
            DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class})
    public ResponseEntity<ExceptionResponse> handleConnectionFailures (Exception ex,
                                                                              HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The system is temporarily unable to reach required backend services",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(exceptionResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionResponse> handleHttpMediaTypeNotSupportedException (Exception ex,
                                                                       HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Please send data in JSON format (application/json)",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(exceptionResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ExceptionResponse response = getExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body. Please check the enum values and JSON format.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        // Extract exactly which parameter failed and what type it should have been
        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type";
        String providedValue = ex.getValue() != null ? ex.getValue().toString() : "empty";

        // Format a clean, user-friendly message (e.g., "Invalid value 'abc' for parameter 'pageSize'. Expected an Integer.")
        String errorMessage = String.format("Invalid value '%s' for parameter '%s'. Expected a %s.",
                providedValue, paramName, expectedType);

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exceptionResponse);
    }

    /* Handles Method Input validation*/

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidateException> handleMethodArgumentNotValidException (MethodArgumentNotValidException ex,
                                                                                       HttpServletRequest request){

        Map<String,String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(),error.getDefaultMessage()));

        ValidateException validationException = new ValidateException(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors

        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(validationException);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        // Default message for generic integrity violations (like foreign key failures)
        String message = "The provided data conflicts with existing records.";

        // Dig into the root cause to see if it's specifically your UNIQUE constraint
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause.getMessage() != null && rootCause.getMessage().contains("Duplicate entry")) {
            // You can customize this based on the unique constraint that failed
            message = "A resource with this title already exists for this user. Please choose a different title.";
        }

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.CONFLICT,
                message,
                request.getRequestURI()
        );

        // 409 Conflict is the standard HTTP status for duplicate data violations
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exceptionResponse);
    }

    /* Handles duplicate Resource Exception */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ExceptionResponse> handleDuplicateResourceException (RuntimeException ex,
                                                                                  HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exceptionResponse);
    }

    /*  Handle Runtime exceptions */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResponse> handleRuntimeException (RuntimeException ex,
                                                                        HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        log.error(" ERROR RUNTIME  : {}",ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exceptionResponse);
    }

    /*  Handle Resource Access Denied exceptions */
    @ExceptionHandler(ResourceAccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleResourceAccessDeniedException (RuntimeException ex,
                                                                     HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exceptionResponse);
    }

    /*  Handle Generic exceptions */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException (Exception ex,
                                                                        HttpServletRequest request){

        ExceptionResponse exceptionResponse = getExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                request.getRequestURI()
        );

        log.error(" ERROR GENERIC : {}",ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exceptionResponse);
    }

    private ExceptionResponse getExceptionResponse(HttpStatus status,
                                                      String message,
                                                      String requestUri){
        return new ExceptionResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                requestUri
        );
    }

}
