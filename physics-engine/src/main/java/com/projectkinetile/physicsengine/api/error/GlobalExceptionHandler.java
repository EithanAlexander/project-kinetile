package com.projectkinetile.physicsengine.api.error;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps exceptions to sanitized {@link ApiErrorResponse} payloads for REST clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Client closed the connection (browser tab change, React Query abort, etc.) before the body
   * finished streaming. Not a server fault — avoid ERROR noise in logs.
   */
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleClientDisconnect(AsyncRequestNotUsableException ex) {
    log.debug("Client disconnected before response completed: {}", ex.getMessage());
  }

  /**
   * Handles explicit HTTP status exceptions from controllers.
   *
   * @param ex exception carrying status and reason
   * @return sanitized error body
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Required parameter '" + ex.getParameterName() + "' is missing"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Invalid value for parameter '" + ex.getName() + "'"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), status.name(), message));
  }

  /**
   * Handles unexpected failures without leaking internal details.
   *
   * @param ex root cause
   * @return generic 500 response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
    if (isClientDisconnect(ex)) {
      log.debug("Client disconnected before response completed: {}", ex.getMessage());
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    log.error("Unhandled API error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error has occurred"));
  }

  private static boolean isClientDisconnect(Throwable ex) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (current instanceof AsyncRequestNotUsableException) {
        return true;
      }
      if (current instanceof ClientAbortException) {
        return true;
      }
      if (current instanceof java.io.IOException ioEx
          && ioEx.getMessage() != null
          && ioEx.getMessage().contains("aborted")) {
        return true;
      }
    }
    return false;
  }
}
