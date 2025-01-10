package com.mx.testing.binding;

import lombok.Getter;

import com.mx.path.core.common.collection.ObjectMap;
import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.configuration.ConfigurationField;
import com.mx.path.gateway.accessor.AccessorResponse;
import com.mx.path.gateway.behavior.GatewayBehavior;
import com.mx.path.gateway.configuration.Configurable;
import com.mx.path.gateway.configuration.annotations.Connection;
import com.mx.path.gateway.context.GatewayRequestContext;

public class BehaviorWithConfigurationAndConnection extends GatewayBehavior implements Configurable {
  @Getter
  private final BehaviorConfiguration behaviorConfiguration;

  @Getter
  private final ConnectionWithBoundConfiguration connectionWithBoundConfiguration;

  @Getter
  boolean initialized = false;

  public static class BehaviorConfiguration {
    @Getter
    @ConfigurationField("active")
    private boolean active;

    @Getter
    @ConfigurationField("actionFilter")
    private String actionFilter;
  }

  public BehaviorWithConfigurationAndConnection(ObjectMap configurations, @Configuration BehaviorConfiguration behaviorConfiguration, @Connection("connection") ConnectionWithBoundConfiguration connection) {
    super(configurations);
    this.connectionWithBoundConfiguration = connection;
    this.behaviorConfiguration = behaviorConfiguration;
  }

  @Override
  protected <T> AccessorResponse<T> call(Class<T> resultType, GatewayRequestContext request, GatewayBehavior terminatingBehavior) {
    return null;
  }

  @Override
  public void initialize() {
    this.initialized = true;
  }
}
