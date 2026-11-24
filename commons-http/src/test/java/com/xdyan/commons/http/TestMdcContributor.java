package com.xdyan.commons.http;

import com.xdyan.commons.http.filter.MdcContributor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Like an app would: the tenant of the header X-Tenant-Id. It also tries to replace "path", a field
 * of the library.
 */
@ApplicationScoped
public class TestMdcContributor implements MdcContributor {

  @Override
  public Map<String, String> fields(ContainerRequestContext request) {
    Map<String, String> fields = new HashMap<>();
    fields.put("tenant_id", request.getHeaderString("X-Tenant-Id"));
    if (request.getHeaderString("X-Replace-Path") != null) {
      fields.put("path", "replaced");
    }
    if (request.getHeaderString("X-Fail") != null) {
      throw new IllegalStateException("contributor failed");
    }
    return fields;
  }
}
