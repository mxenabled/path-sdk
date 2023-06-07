package com.mx.path.gateway.events;

import lombok.Builder;
import lombok.Getter;

import com.mx.common.accessors.Accessor;
import com.mx.path.gateway.Gateway;
import com.mx.path.model.context.RequestContext;
import com.mx.path.model.context.Session;

/**
 * Event - Fires before accessor method is invoked.
 */
@Builder
@Getter
public class BeforeAccessorEvent implements GatewayEvent, AccessorEvent {

  private final Accessor currentAccessor;
  private final Gateway gateway;
  private final RequestContext requestContext;
  private final Session session;

}