// ---------------------------------------------------------------------------------------------------------------------
//   GENERATED FILE - ** Do not edit **
//   Wrapped Class: com.mx.accessors.BaseAccessor
// ---------------------------------------------------------------------------------------------------------------------
package com.mx.testing.accessors.proxy;

import lombok.Getter;

import com.mx.accessors.AccessorConfiguration;
import com.mx.accessors.RootAccessor;
import com.mx.path.gateway.configuration.AccessorConstructionContext;
import com.mx.path.gateway.configuration.AccessorProxy;
import com.mx.testing.accessors.AccountBaseAccessor;
import com.mx.testing.accessors.BaseAccessor;

/**
 * Base class for wrapping com.mx.accessors.BaseAccessor.
 * <p>
 * Used to provide scoped construction strategies.
 */
@RootAccessor
public abstract class BaseAccessorProxy extends BaseAccessor implements AccessorProxy {
  @Getter
  private final AccessorConstructionContext<? extends BaseAccessor> accessorConstructionContext;

  public BaseAccessorProxy(AccessorConfiguration configuration,
      Class<? extends BaseAccessor> accessorClass) {
    super(configuration);
    this.accessorConstructionContext = new AccessorConstructionContext<BaseAccessor>(accessorClass, configuration);
  }

  @Override
  public Class<? extends BaseAccessor> getAccessorClass() {
    return accessorConstructionContext.getAccessorClass();
  }

  /**
   * @return accounts accessor
   */
  @Override
  public AccountBaseAccessor accounts() {
    if (getAccounts() != null) {
      return getAccounts();
    }
    return build().accounts();
  }

  /**
   * Create an instance of Accessor type klass
   *
   * <p>Override to change how the accessor is constructed.
   * @return Accessor
   */
  protected BaseAccessor buildAccessor() {
    return accessorConstructionContext.build();
  }

  public abstract BaseAccessor build();
}