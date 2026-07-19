package com.projectkinetile.physicsengine.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests for {@link GlobalExceptionHandler} sanitized responses.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Global exception handler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("does not treat client disconnect as a 500 error")
  void clientDisconnectIsNotInternalServerError() {
    AsyncRequestNotUsableException ex =
        new AsyncRequestNotUsableException("ServletOutputStream failed to write");

    ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  @DisplayName("does not expose internal message for generic exceptions")
  void genericExceptionDoesNotExposeInternalMessage() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleGeneric(new RuntimeException("SQL: select * from traffic_energy"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ApiErrorResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.message()).isEqualTo("An unexpected error has occurred");
    assertThat(body.message()).doesNotContain("SQL");
  }

  @Test
  @DisplayName("returns reason for response status exceptions")
  void responseStatusExceptionReturnsReason() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiErrorResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.message()).isEqualTo("city is required");
  }

  @Test
  @DisplayName("returns bad request when a required query parameter is missing")
  void missingParameterReturnsBadRequestWithParameterName() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleMissingParam(new MissingServletRequestParameterException("city", "String"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiErrorResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.error()).isEqualTo("BAD_REQUEST");
    assertThat(body.message()).isEqualTo("Required parameter 'city' is missing");
    assertThat(body.fieldErrors()).isEmpty();
  }

  @Test
  @DisplayName("returns bad request when a request parameter cannot be converted")
  void typeMismatchReturnsBadRequestWithParameterName() {
    MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
    when(ex.getName()).thenReturn("limit");

    ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiErrorResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.error()).isEqualTo("BAD_REQUEST");
    assertThat(body.message()).isEqualTo("Invalid value for parameter 'limit'");
  }

  @Test
  @DisplayName("uses HTTP reason phrase when response status exception has no reason")
  void responseStatusWithoutReasonUsesStatusPhrase() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ApiErrorResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.status()).isEqualTo(404);
    assertThat(body.error()).isEqualTo("NOT_FOUND");
    assertThat(body.message()).isEqualTo("Not Found");
  }
}
