package com.xdyan.commons.http.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.xdyan.commons.http.HttpConfig;
import jakarta.enterprise.inject.Any;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Puts the request id, the X-Ray trace id, the method, the path and the client (X-Client-Name,
 * X-Client-Version) in the MDC of every log of the request, plus the fields of the app ({@link
 * MdcContributor}), and logs the request body and the status and body of the response (cut at
 * xdyan.commons.http.log.max-body-length). The exception of an error response is logged by its
 * provider (ErrorLog).
 *
 * <ul>
 *   <li>request_id: the id of the request in API Gateway, which it returns to the client in the
 *       header apigw-requestid: from an error seen in the client to its logs
 *   <li>trace_id: the X-Ray trace of the invocation, with the subsegments of @XRayTraced. Empty
 *       outside Lambda
 * </ul>
 */
@Slf4j
@Provider
public class CorrelationFilter
    implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {

  private static final String AGW_REQUEST_ID_KEY = "x-agw-request-id";
  private static final String XRAY_TRACE_HEADER_PROPERTY = "com.amazonaws.xray.traceHeader";
  private static final String CLIENT_NAME_KEY = "X-Client-Name";
  private static final String CLIENT_VERSION_KEY = "X-Client-Version";
  private static final String REQUEST_ID = "request_id";
  private static final String TRACE_ID = "trace_id";
  private static final String METHOD = "method";
  private static final String PATH = "path";
  private static final String STATUS = "status";
  private static final String CLIENT_NAME = "client_name";
  private static final String CLIENT_VERSION = "client_version";
  private static final String EMPTY = "";

  /** The MDC fields of the library: an {@link MdcContributor} can't replace them */
  public static final Set<String> FIELDS =
      Set.of(REQUEST_ID, TRACE_ID, METHOD, PATH, STATUS, CLIENT_NAME, CLIENT_VERSION);

  @Inject Instance<APIGatewayV2HTTPEvent> httpEventInstance;

  // Read when used: RESTEasy creates the providers before the @ConfigMapping are registered
  @Inject Instance<HttpConfig> config;

  @Inject @Any Instance<MdcContributor> contributors;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    MDC.put(REQUEST_ID, resolveRequestId(requestContext));
    MDC.put(TRACE_ID, resolveTraceRoot());
    MDC.put(METHOD, ofNullable(requestContext.getMethod()).orElse(EMPTY));
    MDC.put(PATH, ofNullable(requestContext.getUriInfo().getRequestUri().toString()).orElse(EMPTY));
    MDC.put(CLIENT_NAME, ofNullable(requestContext.getHeaderString(CLIENT_NAME_KEY)).orElse(EMPTY));
    MDC.put(
        CLIENT_VERSION,
        ofNullable(requestContext.getHeaderString(CLIENT_VERSION_KEY)).orElse(EMPTY));
    putFieldsOfTheApp(requestContext);
    // In Lambda a request without body (e.g. a GET) still has an entity, with no bytes
    if (!requestContext.hasEntity()) {
      log.info("Request Body is empty");
    } else {
      try {
        String requestBody = new String(requestContext.getEntityStream().readAllBytes(), UTF_8);
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody.getBytes(UTF_8)));

        if (requestBody.isEmpty()) {
          log.info("Request Body is empty");
        } else if (requestBody.length() > maxBodyLength()) {
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
   * Runs before the body is written (aroundWriteTo). A response without body (e.g. 204) never
   * reaches aroundWriteTo, so it is logged and the MDC cleared here, or the next request would
   * start with the MDC of this one.
   */
  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.put(STATUS, String.valueOf(responseContext.getStatus()));
    if (!responseContext.hasEntity()) {
      log.info("Response {}: Body is empty", responseContext.getStatus());
      MDC.clear();
    }
  }

  // CloseResource: the stream of the response belongs to RESTEasy, which closes it
  @SuppressWarnings("PMD.CloseResource")
  @Override
  public void aroundWriteTo(WriterInterceptorContext context)
      throws IOException, WebApplicationException {
    OutputStream originalStream = context.getOutputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    context.setOutputStream(baos);

    try {
      context.proceed();
      byte[] responseBytes = baos.toByteArray();

      var status = ofNullable(MDC.get(STATUS)).orElse(EMPTY);
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
      // The whole body is already here: with its length it is not sent chunked. Above 8 KB a
      // chunked response reaches the client of the tests (mock event server of
      // quarkus-amazon-lambda-http) without an end, and the request hangs until its timeout
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

  private void putFieldsOfTheApp(ContainerRequestContext requestContext) {
    for (MdcContributor contributor : contributors) {
      try {
        Map<String, String> fields = contributor.fields(requestContext);
        if (fields == null) {
          continue;
        }
        fields.forEach((key, value) -> {
          if (FIELDS.contains(key)) {
            log.warn(
                "MDC field '{}' of {} ignored: it is a field of the library",
                key,
                contributor.getClass().getName());
          } else if (key != null && value != null) {
            MDC.put(key, value);
          }
        });
        // A bug in a contributor of the app must not break the request: it only loses its fields
      } catch (
          @SuppressWarnings("PMD.AvoidCatchingGenericException")
          RuntimeException e) {
        log.error("MDC fields of {} not added", contributor.getClass().getName(), e);
      }
    }
  }

  /**
   * The Lambda runtime sets the property on every invocation ("Root=1-...;Parent=...;Sampled=1").
   * Outside Lambda there is none, or the mock event server of the tests sets a UUID: parsing it
   * would log "Malformed TraceHeader String input" as ERROR on every request.
   */
  private String resolveTraceRoot() {
    String header = System.getProperty(XRAY_TRACE_HEADER_PROPERTY);
    if (header == null || !header.contains("Root=")) {
      return EMPTY;
    }
    return ofNullable(TraceHeader.fromString(header).getRootTraceId())
        .map(TraceID::toString)
        .orElse(EMPTY);
  }

  private String resolveRequestId(ContainerRequestContext requestContext) {
    return ofNullable(requestContext.getHeaderString(AGW_REQUEST_ID_KEY))
        .or(() ->
            ofNullable(httpEventInstance.get())
                .map(APIGatewayV2HTTPEvent::getRequestContext)
                .map(APIGatewayV2HTTPEvent.RequestContext::getRequestId))
        .orElse(EMPTY);
  }
}
