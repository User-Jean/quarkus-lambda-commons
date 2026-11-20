package com.xdyan.commons.http.filter;

import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.xdyan.commons.http.HttpConfig;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

/**
 * Puts the request id, the trace id and the client (X-Client-Name, X-Client-Version) in the MDC of every log of the
 * request, and logs the request body and the status and body of the response (cut at
 * xdyan.commons.http.log.max-body-length). The exception of an error response is logged by its provider (ErrorLog).
 */
@Slf4j
@Provider
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {

  private static final String AGW_REQUEST_ID_KEY = "x-agw-request-id";
  private static final String AWS_TRACE_ID_KEY = "X-Amzn-Trace-Id";
  private static final String XRAY_TRACE_HEADER_PROPERTY = "com.amazonaws.xray.traceHeader";
  private static final String CLIENT_NAME_KEY = "X-Client-Name";
  private static final String CLIENT_VERSION_KEY = "X-Client-Version";
  private static final String STATUS_KEY = "status";
  private static final String EMPTY = "";

  @Inject
  Instance<APIGatewayV2HTTPEvent> httpEventInstance;

  // Read when used: RESTEasy creates the providers before the @ConfigMapping are registered
  @Inject
  Instance<HttpConfig> config;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    var apiGwRequestId = resolveRequestId(requestContext);
    var method = ofNullable(requestContext.getMethod()).orElse(EMPTY);
    var path = ofNullable(requestContext.getUriInfo().getRequestUri().toString()).orElse(EMPTY);
    var traceId = ofNullable(requestContext.getHeaderString(AWS_TRACE_ID_KEY)).orElse(EMPTY);

    MDC.put("requestId", apiGwRequestId);
    MDC.put("method", method);
    MDC.put("path", path);
    MDC.put("X-Amzn-Trace-Id", traceId);
    MDC.put("trace_id", resolveTraceRoot());
    MDC.put("client_name", ofNullable(requestContext.getHeaderString(CLIENT_NAME_KEY)).orElse(EMPTY));
    MDC.put("client_version", ofNullable(requestContext.getHeaderString(CLIENT_VERSION_KEY)).orElse(EMPTY));
    if (!requestContext.hasEntity()) {
      log.info("Request Body is empty");
    } else {
      try {
        String requestBody = new String(requestContext.getEntityStream().readAllBytes(), UTF_8);
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody.getBytes(UTF_8)));

        if  (requestBody.length() > maxBodyLength()) {
          log.info("Request Body (Truncated): {}", requestBody.substring(0, maxBodyLength()));
        } else {
          log.info("Request Body: {}", requestBody);
        }
      } catch (IOException e) {
        log.error("Error reading request body", e);
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Runs before the body is written (aroundWriteTo). A response without body (e.g. 204) never reaches aroundWriteTo,
   * so it is logged and the MDC cleared here, or the next request would start with the MDC of this one.
   */
  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.put(STATUS_KEY, String.valueOf(responseContext.getStatus()));
    if (!responseContext.hasEntity()) {
      log.info("Response {}: Body is empty", responseContext.getStatus());
      MDC.clear();
    }
  }

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
    OutputStream originalStream = context.getOutputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    context.setOutputStream(baos);

    try {
      context.proceed();
      byte[] responseBytes = baos.toByteArray();

      var status = ofNullable(MDC.get(STATUS_KEY)).orElse(EMPTY);
      if (responseBytes.length == 0) {
        log.info("Response {}: Body is empty", status);
      } else {
        String responseBody = new String(responseBytes, UTF_8);

        if (responseBody.length() > maxBodyLength()) {
          String truncatedBody = responseBody.substring(0, maxBodyLength()) + "...";
          log.info("Response {}: Body (Truncated): {}", status, truncatedBody);
        } else {
          log.info("Response {}: Body: {}", status, responseBody);
        }
      }
      // The whole body is already here: with its length it is not sent chunked. Above 8 KB a chunked response
      // reaches the client of the tests (mock event server of quarkus-amazon-lambda-http) without an end, and
      // the request hangs until its timeout
      context.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, responseBytes.length);
      originalStream.write(responseBytes);

    } finally {
      context.setOutputStream(originalStream);
      MDC.clear();
    }
  }

  private int maxBodyLength() {
    return config.get().log().maxBodyLength();
  }

  private String resolveTraceRoot() {
    return ofNullable(TraceHeader.fromString(System.getProperty(XRAY_TRACE_HEADER_PROPERTY)).getRootTraceId())
        .map(TraceID::toString)
        .orElse(EMPTY);
  }

  private String resolveRequestId(ContainerRequestContext requestContext) {
    return Optional.ofNullable(requestContext.getHeaderString(AGW_REQUEST_ID_KEY))
        .or(() -> Optional.ofNullable(httpEventInstance.get())
            .map(APIGatewayV2HTTPEvent::getRequestContext)
            .map(APIGatewayV2HTTPEvent.RequestContext::getRequestId))
        .orElse(EMPTY);
  }
}
