package com.mx.path.gateway.configuration

import com.mx.path.core.common.accessor.PathResponseStatus
import com.mx.path.core.common.collection.ObjectMap
import com.mx.path.core.common.gateway.GatewayException
import com.mx.testing.binding.ConnectionWithBoundConfiguration

import spock.lang.Specification

class ConnectionBinderTest extends Specification {

  ObjectMap configuration
  ConnectionBinder subject

  def setup() {
    subject = new ConnectionBinder()
    configuration = new ObjectMap()
  }

  def "build connection"() {
    given:
    def configuration = new ObjectMap().tap {
      createMap("TestConnection").tap {
        createMap("configurations").tap {
          put("clientId", "clientId")
        }
        put("baseUrl", "url")
        put("certificateAlias", "alias")
        put("keystorePath", "path")
        put("keystorePassword", "password")
      }
    }

    when:
    def connection = (ConnectionWithBoundConfiguration) subject.build(ConnectionWithBoundConfiguration.class, configuration, "TestConnection")

    then:
    verifyAll (connection) {
      baseUrl == "url"
      certificateAlias == "alias"
      keystorePath == "path"
      keystorePassword.toString() == "password"
    }
  }

  def "build connection throw on missing client id"() {
    given:
    def configuration = new ObjectMap().tap {
      createMap("TestConnection").tap {
        createMap("configurations").tap {
          put("clientId", null)
        }
        put("baseUrl", "url")
        put("certificateAlias", "alias")
        put("keystorePath", "path")
        put("keystorePassword", "password")
      }
    }

    when:
    subject.build(ConnectionWithBoundConfiguration.class, configuration, "TestConnection")

    then:
    def e = thrown(ConfigurationError)
    e.message == "Client ID not provided for connection TestConnection at connections.TestConnection"
  }

  def "build connection settings"() {
    given:
    def configuration = new ObjectMap().tap {
      createMap("TestConnection").tap {
        put("baseUrl", "url")
        put("certificateAlias", "alias")
        put("keystorePath", "path")
        put("keystorePassword", "password")
      }
    }

    when:
    def connection = subject.buildConnection(configuration, "TestConnection")

    then:
    verifyAll (connection) {
      baseUrl == "url"
      certificateAlias == "alias"
      keystorePath == "path"
      keystorePassword.toString() == "password"
    }
  }

  def "build connection and fail validation"() {
    given:
    def configuration = new ObjectMap().tap {
      createMap("TestConnection").tap {
        put("baseUrl", "url")
        put("certificateAlias", "")
        put("keystorePath", "path")
        put("keystorePassword", "password")
      }
    }

    when:
    subject.buildConnection(configuration, "TestConnection")

    then:
    def e = thrown(GatewayException)
    e.status == PathResponseStatus.INTERNAL_ERROR
    e.message == "Invalid connection details. Missing certificateAlias"
  }
}
