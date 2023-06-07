package com.mx.path.gateway.events;

import com.mx.common.accessors.Accessor;

/**
 * Interface for all Accessor-emitted events
 */
public interface AccessorEvent {

  /**
   * @return The accessor emitting the event
   */
  Accessor getCurrentAccessor();

}