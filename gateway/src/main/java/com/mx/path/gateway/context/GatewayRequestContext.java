package com.mx.path.gateway.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import com.mx.path.core.common.model.ModelBase;
import com.mx.path.core.context.RequestContext;
import com.mx.path.gateway.Gateway;
import com.mx.path.gateway.accessor.Accessor;

/**
 * Decorates RequestContext with Gateway-specific fields.
 *
 * Provides the same API as RequestContext, but will need to be explicitly cast to GatewayRequestContext if accessed
 * via GatewayRequestContext.current() or RequestContext.current()
 */
@SuppressWarnings("RedundantModifier")
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public final class GatewayRequestContext extends RequestContext {
  private Gateway<?> gateway;
  private Accessor currentAccessor;
  private Gateway<?> currentGateway;
  private boolean listOp;
  private Class<? extends ModelBase<?>> model;
  private String op;

  /**
   * Coerces the current RequestContext into a GatewayRequestContext and returns it.
   *
   * @return GatewayRequestContext
   */
  @SuppressWarnings("PMD.EmptyCatchBlock")
  public static GatewayRequestContext current() {
    RequestContext requestContext = RequestContext.current();
    if (requestContext == null) {
      return null;
    }
    if (requestContext.getOriginatingIP() == null) {
      try {
        URL url = new URL("https://api.ipify.org"); // Public IP service URL
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
        requestContext.setOriginatingIP(in.readLine());
        in.close();
      } catch (IOException e) {
        //Do nothing as we don't want to obstruct the flow
      }
    }
    return fromRequestContext(requestContext);
  }

  /**
   * Build gateway context from request context.
   *
   * @param requestContext context
   * @return gateway context
   */
  public static GatewayRequestContext fromRequestContext(RequestContext requestContext) {
    if (requestContext instanceof GatewayRequestContext) {
      return (GatewayRequestContext) requestContext;
    } else if (requestContext == null) {
      return GatewayRequestContext.builder().build();
    }
    return new GatewayRequestContext(requestContext);
  }

  private GatewayRequestContext(RequestContext requestContext) {
    super(requestContext.toBuilder());
  }

  /**
   * @return the root gateway
   * @param <T> type
   */
  @SuppressWarnings("unchecked")
  public <T extends Gateway<?>> T getGateway() {
    return (T) gateway;
  }

  /**
   * @return the current gateway
   * @param <T> type
   */
  @SuppressWarnings("unchecked")
  public <T extends Gateway<?>> T getCurrentGateway() {
    return (T) currentGateway;
  }
}
