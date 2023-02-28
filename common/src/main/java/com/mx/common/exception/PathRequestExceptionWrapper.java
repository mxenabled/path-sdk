package com.mx.common.exception;

/**
 * A PathRequestException wrapper.
 *
 * <p>This exception type would indicate that something unexpected happened and the cause should be inspected, since
 * this exception would give much contextual information. Commonly used in behaviors and outside the gateway
 * to standardize exception handling.
 *
 * <p>Example:
 *
 * <p><pre>{@code
 *   try {
 *     try {
 *       setupWebRequest(); // this could throw any kind of error
 *       gateway.status();
 *     } catch (PathRequestException e) {
 *       throw e;
 *     } catch (Exception e) {
 *       // Wrap the exception and set status and user message.
 *       throw new PathRequestExceptionWrapper(e.getMessage(), e)
 *         .withStatus(PathResponseStatus.INTERNAL_ERROR)
 *         .withUserMessage("An unexpected gateway error occurred");
 *     }
 *   } catch (PathRequestException exception) {
 *     // This exception handler will catch all request exceptions and unexpected
 *     System.out.println("An error occurred with status: " + exception.getStatus());
 *     System.out.println(exception.getUserMessage());
 *   }
 * }</pre>
 */
public class PathRequestExceptionWrapper extends PathRequestException {
  public PathRequestExceptionWrapper() {
    super();
  }

  public PathRequestExceptionWrapper(Throwable cause) {
    super(cause);
  }

  public PathRequestExceptionWrapper(String message, Throwable cause) {
    super(message, cause);
  }
}
