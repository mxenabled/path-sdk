package com.mx.common.configuration;

import com.mx.common.exception.PathSystemException;

/**
 * Thrown on gateway configuration error
 *
 * <p>See {@link PathSystemException} for usage details
 */
public class ConfigurationException extends PathSystemException {
  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
