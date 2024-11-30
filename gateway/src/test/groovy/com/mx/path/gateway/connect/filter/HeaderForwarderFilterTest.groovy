package com.mx.path.gateway.connect.filter

import static org.mockito.Mockito.mock

import com.mx.path.core.common.connect.Request
import com.mx.path.core.common.connect.RequestFilter
import com.mx.path.core.common.connect.Response
import com.mx.path.core.context.RequestContext
import com.mx.path.core.context.UpstreamRequestConfiguration

import spock.lang.Specification

class HeaderForwarderFilterTest extends Specification {
  class TestRequest extends Request<TestRequest, TestResponse> {
    TestRequest(RequestFilter filterChain) {
      super(filterChain)
    }

    @Override
    TestResponse execute() {
      return null
    }
  }

  class TestResponse extends Response<TestRequest, TestResponse> {
  }

  def "forwards all headers in current RequestContext forwardedHeader"() {
    given:
    def requestFilter = mock(RequestFilter)
    def subject = new HeaderForwarderFilter()
    def request = new TestRequest(requestFilter)
    def response = new TestResponse()
    RequestContext.builder()
        .upstreamRequestConfiguration(UpstreamRequestConfiguration.builder()
        .forwardedHeader("mx_forwarded_important_header", "12345")
        .forwardedHeader("mx-forwarded-important-header", "12345")
        .build())
        .build()
        .register()

    when:
    subject.execute(request, response)

    then:
    request.headers.containsKey("mx_forwarded_important_header")
    request.headers.containsKey("mx-forwarded-important-header")
  }
}
