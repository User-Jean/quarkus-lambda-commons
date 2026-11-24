package com.xdyan.commons.http.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Map;

/**
 * Adds fields of the app to the MDC of every request, next to the ones of {@link
 * CorrelationFilter}. Implement it as an {@code @ApplicationScoped} bean, nothing else is declared:
 * the filter calls every implementation before it logs the request body, so the fields are in every
 * log line of the request, and clears them with its own once the response is written.
 *
 * <pre>{@code
 * @ApplicationScoped
 * public class TenantMdc implements MdcContributor {
 *   public Map<String, String> fields(ContainerRequestContext request) {
 *     return Map.of("tenant_id", request.getHeaderString("X-Tenant-Id"));
 *   }
 * }
 * }</pre>
 *
 * Keys in snake_case, like the ones of the library. A key of the library ({@link
 * CorrelationFilter#FIELDS}) is never replaced, and a null value is skipped. An exception is logged
 * and the request goes on without these fields.
 */
public interface MdcContributor {

  Map<String, String> fields(ContainerRequestContext request);
}
