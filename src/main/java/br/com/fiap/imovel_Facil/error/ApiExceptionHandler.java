package br.com.fiap.imovel_Facil.error;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PropertyNotFoundException.class)
    public ResponseEntity<ApiError> notFound(PropertyNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(InvalidRequestException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation() {
        return ResponseEntity.badRequest().body(new ApiError("Invalid request"));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("Conflicting operation"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + (error.isBindingFailure() ? "invalid value" : error.getDefaultMessage()))
                .distinct().sorted().collect(Collectors.joining("; "));
        return new ResponseEntity<>(new ApiError(message.isBlank() ? "Invalid request" : message), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "Invalid request";
            case 404 -> "Resource not found";
            case 405 -> "Method not allowed";
            case 406 -> "Not acceptable";
            case 415 -> "Unsupported media type";
            default -> status.is5xxServerError() ? "Internal server error" : "Request rejected";
        };
        return new ResponseEntity<>(new ApiError(message), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        LOGGER.error("Falha inesperada ao processar requisição", exception);
        return ResponseEntity.internalServerError().body(new ApiError("Internal server error"));
    }
}
