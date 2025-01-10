package com.mx.path.gateway.configuration

import com.mx.path.core.common.collection.ObjectMap
import com.mx.path.core.common.messaging.MessageBroker
import com.mx.path.gateway.behavior.GatewayBehavior
import com.mx.path.gateway.service.GatewayService
import com.mx.testing.MessageBrokerImpl
import com.mx.testing.binding.BehaviorWithConfiguration
import com.mx.testing.binding.BehaviorWithConfigurationAndConnection
import com.mx.testing.binding.BindedConfigGatewayService

import spock.lang.Specification

class GatewayObjectConfiguratorTest extends Specification {
  GatewayObjectConfigurator subject
  ConfigurationState state

  def setup() {
    state = ConfigurationState.current
    state.pushLevel("client1")
    subject = new GatewayObjectConfigurator(state)
  }

  def cleanup() {
    ConfigurationState.resetCurrent()
  }

  def "builds and binds a GatewayService"() {
    given:
    def node = new ObjectMap().tap {
      put("class", BindedConfigGatewayService.getCanonicalName())
      put("configurations", new ObjectMap().tap { put("string", "value") })
    }

    when:
    def result = subject.buildFromNode(node, "client1", GatewayService)

    then:
    result instanceof BindedConfigGatewayService
    verifyAll((BindedConfigGatewayService) result) {
      config.string == "value"
      config.clientId == "client1"
      initialize
    }
  }

  def "builds and binds a GatewayBehavior"() {
    given:
    def node = new ObjectMap().tap {
      put("class", BehaviorWithConfiguration.getCanonicalName())
      put("configurations", new ObjectMap().tap {
        put("active", true)
        put("actionFilter", "put")
      })
    }

    when:
    def result = subject.buildFromNode(node, "client1", GatewayBehavior)

    then:
    result instanceof BehaviorWithConfiguration
    verifyAll((BehaviorWithConfiguration) result) {
      behaviorConfiguration.active
      initialized
    }
  }

  def "builds and binds a GatewayBehavior with Connection"() {
    given:
    def node = new ObjectMap().tap {
      put("class", BehaviorWithConfigurationAndConnection.getCanonicalName())
      put("configurations", new ObjectMap().tap {
        put("active", true)
        put("actionFilter", "put")
      })
      createMap("connections").tap {
        createMap("connection").tap {
          createMap("configurations").tap {
            put("clientId", "clientId")
          }
          put("baseUrl", "url")
          put("certificateAlias", "alias")
          put("keystorePath", "path")
          put("keystorePassword", "password")
        }
      }
    }

    when:
    def result = subject.buildFromNode(node, "client1", GatewayBehavior)

    then:
    result instanceof BehaviorWithConfigurationAndConnection
    verifyAll((BehaviorWithConfigurationAndConnection) result) {
      connectionWithBoundConfiguration
      behaviorConfiguration.active
      initialized
      verifyAll (connectionWithBoundConfiguration) {
        baseUrl == "url"
        certificateAlias == "alias"
        keystorePath == "path"
        keystorePassword.toString() == "password"
      }
    }
  }

  def "builds and binds a Facility"() {
    given:
    def node = new ObjectMap().tap {
      put("class", MessageBrokerImpl.getCanonicalName())
      put("configurations", new ObjectMap().tap { put("key1", "value1") })
    }

    when:
    def result = subject.buildFromNode(node, "client1", MessageBroker)

    then:
    result instanceof MessageBrokerImpl
    verifyAll((MessageBrokerImpl) result) {
      configurations.testField == "value1"
      configurations.clientId == "client1"
      initialized
    }
  }
}
