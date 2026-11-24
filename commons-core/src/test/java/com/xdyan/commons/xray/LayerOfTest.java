package com.xdyan.commons.xray;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LayerOfTest {

  @ParameterizedTest(name = "{0} → {1}")
  @CsvSource({
    "com.jean.orders.app.api, API",
    "com.jean.orders.app.business.service, Service",
    "com.jean.orders.app.infrastructure.mysql, MySQL",
    "com.jean.orders.app.infrastructure.client, Client",
    "com.jean.orders.app.infrastructure.sqs, Sqs",
    "com.jean.orders.app.infrastructure.client.payments, App",
    "com.jean.orders.app.business.utils, App"
  })
  public void nameTheLayerFromThePackage(String packageName, String layer) {
    assertEquals(layer, XRayTracedInterceptor.layerOf(packageName));
  }
}
