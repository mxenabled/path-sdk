package com.mx.path.api.connect.http

import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy

import com.mx.path.connect.http.HttpClientFilter
import com.mx.path.connect.http.HttpRequest
import com.mx.path.connect.http.HttpResponse
import com.mx.path.core.common.collection.MultiValueMap
import com.mx.path.core.common.connect.ConnectException
import com.mx.path.core.common.connect.Request
import com.mx.path.core.common.connect.RequestFilter

import org.apache.http.HttpEntity

import spock.lang.Specification
import spock.lang.Unroll
import testing.connect.RequestFilterStub

class HttpClientFilterTest extends Specification {
  RequestFilter filterChain

  static MultiValueMap<String, String> makeContentTypeHeaders(String... contentTypes) {
    MultiValueMap<String, String> headers = new MultiValueMap<>()
    headers.addAll("Content-Type", Arrays.asList(contentTypes))
    return headers
  }

  def setup() {
    filterChain = new RequestFilterStub()
  }

  def "sets the Response body based on the Request's preferences"() {
    given:
    def subject = spy(new HttpClientFilter())
    def request = new HttpRequest(filterChain)
    def response = new HttpResponse()
    MultiValueMap<String, String> responseHeaders = new MultiValueMap<>()
    def httpEntity = mock(HttpEntity)
    def stringBody = "This is the response"
    def rawBody = stringBody.bytes

    doReturn(stringBody).when(subject).getHttpEntityBodyAsString(httpEntity)
    doReturn(rawBody).when(subject).getHttpEntityBodyAsBytes(httpEntity)

    when: "No preference is specified"
    subject.setResponseBody(request, response, responseHeaders, httpEntity)

    then:
    response.body == stringBody
    response.rawBody.length == 0

    when: "Raw body is preferred"
    request = new HttpRequest(filterChain).tap {
      withPreferredResponseBodyType(Request.PreferredResponseBodyType.RAW)
    }
    response = new HttpResponse()
    subject.setResponseBody(request, response, responseHeaders, httpEntity)

    then:
    response.body == ""
    response.rawBody == rawBody


    when: "String body is preferred"
    request = new HttpRequest(filterChain).tap {
      withPreferredResponseBodyType(Request.PreferredResponseBodyType.STRING)
    }
    response = new HttpResponse()
    subject.setResponseBody(request, response, responseHeaders, httpEntity)

    then:
    response.body == stringBody
    response.rawBody.length == 0


    when: "Both String and raw body are preferred"
    request = new HttpRequest(filterChain).tap {
      withPreferredResponseBodyType(Request.PreferredResponseBodyType.STRING_AND_RAW)
    }
    response = new HttpResponse()
    subject.setResponseBody(request, response, responseHeaders, httpEntity)

    then:
    response.body == stringBody
    response.rawBody == rawBody
  }

  @Unroll
  def "infers whether the #rawBodyShouldBeReturned based on the #contentTypeHeaders"() {
    given:
    def subject = new HttpClientFilter()

    when:
    def result = subject.shouldReturnRawBody(contentTypeHeaders)

    then:
    result == rawBodyShouldBeReturned

    where:
    rawBodyShouldBeReturned || contentTypeHeaders
    true                    || makeContentTypeHeaders("application/pdf")
    true                    || makeContentTypeHeaders("image/png")
    true                    || makeContentTypeHeaders("IMAGE/JPEG")
    true                    || makeContentTypeHeaders("Image/gif")
    true                    || makeContentTypeHeaders("image/svg+xml")
    true                    || makeContentTypeHeaders("image/tiff")
    true                    || makeContentTypeHeaders("application/msword")
    false                   || makeContentTypeHeaders("application/json")
    false                   || makeContentTypeHeaders("text/html")
    false                   || makeContentTypeHeaders("application/xml")
    false                   || makeContentTypeHeaders()
  }

  def "test connect exception handling in execute method"() {
    given: "A mock HttpRequest and Response"
    def mockRequest = Mock(HttpRequest)
    def mockResponse = Mock(HttpResponse)

    // Create the real executor object (HttpClientFilter)
    HttpClientFilter executor = Mock(HttpClientFilter)

    executor.execute(mockRequest, mockResponse) >> { throw new ConnectException("Connection Exception") }

    when: "Execute method is called"
    executor.execute(mockRequest, mockResponse)

    then: "ConnectException is thrown"
    thrown(ConnectException)
  }

}
