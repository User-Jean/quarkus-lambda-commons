package com.xdyan.commons.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.Locale;

/**
 * Names each subsegment "<layer> <Class>.<method>", with the layer taken from the package, so the
 * calls API → Service → MySQL show nested in the trace. Runs before @Transactional (PLATFORM_BEFORE
 * + 200) so the subsegment also covers the commit, where Hibernate flushes pending INSERT/UPDATE
 * statements.
 */
@XRayTraced
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class XRayTracedInterceptor {

  @Inject ErrorClassifier errorClassifier;

  // CloseResource: the subsegment is ended by AWSXRay.endSubsegment() in the finally.
  // AvoidCatchingGenericException: any exception of the method is recorded and thrown again as is
  @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidCatchingGenericException"})
  @AroundInvoke
  Object trace(InvocationContext ctx) throws Exception {
    // No trace context outside Lambda (tests, local run). Checked per call, not in a static field,
    // because the native build initializes classes at build time.
    // getCurrentSegmentOptional() can't be used: in Lambda the parent segment is created lazily by
    // beginSubsegment from the trace header, so it is always empty before the first subsegment.
    if (System.getenv("LAMBDA_TASK_ROOT") == null) {
      return ctx.proceed();
    }

    var method = ctx.getMethod();
    var type = method.getDeclaringClass();
    var layer = layerOf(type.getPackageName());
    var operation = type.getSimpleName() + "." + method.getName();
    Subsegment subsegment = AWSXRay.beginSubsegment(layer + " " + operation);
    try {
      // No namespace/SQL data on purpose: with them X-Ray adds an inferred database segment, which
      // Grafana shows as a separate group outside the function instead of in its timeline.
      subsegment.putAnnotation("layer", layer);
      subsegment.putAnnotation("operation", operation);
      return ctx.proceed();
    } catch (Exception e) {
      record(subsegment, e);
      throw e;
    } finally {
      AWSXRay.endSubsegment();
    }
  }

  private void record(Subsegment subsegment, Exception e) {
    if (errorClassifier.isClientError(e)) {
      subsegment.setError(true);
      subsegment.putAnnotation("error_message", String.valueOf(e.getMessage()));
    } else {
      subsegment.addException(e);
    }
  }

  /**
   * api → API, business.service → Service, infrastructure.mysql → MySQL,
   * infrastructure.&lt;name&gt; → &lt;Name&gt; (e.g. infrastructure.client → Client), anything else
   * → App.
   */
  static String layerOf(String packageName) {
    if (packageName.endsWith(".api")) {
      return "API";
    }
    if (packageName.endsWith(".business.service")) {
      return "Service";
    }
    if (packageName.endsWith(".infrastructure.mysql")) {
      return "MySQL";
    }
    int infrastructure = packageName.lastIndexOf(".infrastructure.");
    if (infrastructure >= 0) {
      var name = packageName.substring(infrastructure + ".infrastructure.".length());
      if (!name.isEmpty() && !name.contains(".")) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
      }
    }
    return "App";
  }
}
