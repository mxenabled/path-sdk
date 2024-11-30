package com.mx.path.gateway.connect.filter;

import com.mx.path.core.common.connect.ConnectException;
import com.mx.path.core.common.connect.Request;
import com.mx.path.core.common.connect.RequestFilterBase;
import com.mx.path.core.common.connect.Response;
import com.mx.path.core.common.http.HttpStatus;
import com.mx.path.core.common.process.FaultTolerantExecutor;
import com.mx.path.core.context.RequestContext;
import com.mx.path.core.context.ResponseContext;
import com.mx.path.core.context.Session;
import com.mx.path.core.context.facility.Facilities;
import com.mx.path.gateway.context.GatewayRequestContext;

/**
 * Wraps the upstream call in a FaultTolerantExecutor if one is configured. Otherwise, uses the configured hystrix wrapper
 * (if enabled) and calls next.
 */
public class FaultTolerantRequestFilter extends RequestFilterBase {

  /**
   * Execute this filter.
   *
   * @param request  Request
   * @param response Response
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  @Override
  public final void execute(Request request, Response response) {
    FaultTolerantExecutor faultTolerantExecutor = Facilities.getFaultTolerantExecutor(RequestContext.current().getClientId());
    if (faultTolerantExecutor != null) {
      try {
        RequestContext currentRequestContext = RequestContext.current();
        ResponseContext currentResponseContext = ResponseContext.current();
        Session currentSession = Session.current();

        String scope = request.getFaultTolerantScope() != null ? request.getFaultTolerantScope() : buildScope();
        faultTolerantExecutor.submit(scope, scopeConfigurations -> {

          // Forward context objects to current thread.
          if (currentRequestContext != null) {
            currentRequestContext.register();
          }

          if (currentResponseContext != null) {
            currentResponseContext.register();
          }

          if (currentSession != null) {
            Session.setCurrent(currentSession);
          }

          // If a request timeout was explicitly provided (should be rare) we respect it. Otherwise, we use the timeout
          // that the FaultTolerantExecutor is using for this request.
          if (request.getRequestTimeOut() == null) {
            request.withTimeOut(scopeConfigurations.getTimeout());
          }
          next(request, response);
          return null;
        });
      } catch (ConnectException e) {
        response.withBody("** MX INTERNAL FAULT TOLERANCE ERROR: " + e.getStatus().name() + " **");
        response.withStatus(e.getStatus().toHttpStatus());
        throw e;
      } catch (Exception e) {
        response.withBody("** MX INTERNAL FAULT TOLERANCE ERROR **");
        response.withStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        throw new ConnectException("Upstream API request failure", e);
      }
    } else {
      next(request, response);
    }
  }

  /**
   * Build scope.
   *
   * @return scope
   */
  final String buildScope() {
    StringBuilder sb = new StringBuilder();
    sb.append("http");

    if (RequestContext.current().getFeature() != null) {
      sb.append(".");
      sb.append(RequestContext.current().getFeature());
    }

    if (GatewayRequestContext.current() != null && GatewayRequestContext.current().getOp() != null) {
      sb.append(".");
      sb.append(GatewayRequestContext.current().getOp());
    }

    return sb.toString();
  }
}
