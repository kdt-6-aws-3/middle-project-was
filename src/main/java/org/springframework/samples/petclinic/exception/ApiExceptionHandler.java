package org.springframework.samples.petclinic.exception;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static final String VALIDATION_FAILED = "Validation failed";

	private final MessageSource messages;

	ApiExceptionHandler(MessageSource messages) {
		this.messages = messages;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ex.getBody();
		problem.setDetail(VALIDATION_FAILED);
		problem.setProperty("errors", toFieldErrors(ex.getBindingResult(), Map.of()));
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@ExceptionHandler(ApiValidationException.class)
	ProblemDetail handleApiValidation(ApiValidationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
		problem.setProperty("errors", toFieldErrors(ex.getErrors(), ex.getFieldAliases()));
		return problem;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflict(DataIntegrityViolationException ex) {
		logger.warn("Request rejected by a database constraint: {}", ex.getMostSpecificCause().getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		logger.error("Unexpected error while handling an API request", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	private List<ApiFieldError> toFieldErrors(Errors errors, Map<String, String> fieldAliases) {
		Locale locale = LocaleContextHolder.getLocale();
		return errors.getFieldErrors().stream().map(error -> toFieldError(error, fieldAliases, locale)).toList();
	}

	private ApiFieldError toFieldError(FieldError error, Map<String, String> fieldAliases, Locale locale) {
		String field = fieldAliases.getOrDefault(error.getField(), error.getField());
		return new ApiFieldError(field, error.getCode(), this.messages.getMessage(error, locale));
	}

	record ApiFieldError(String field, String code, String message) {
	}

}
