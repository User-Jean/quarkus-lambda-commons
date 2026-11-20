package com.xdyan.commons.xray;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class DefaultErrorClassifier implements ErrorClassifier {

  @Override
  public boolean isClientError(Throwable error) {
    return error instanceof ClientError;
  }
}
