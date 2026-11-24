package com.xdyan.commons.http.contract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The module starts in its own tests because src/test/resources has an openapi.yaml, like an app.
 */
public class ContractFirstCheckTest {

  private static final ClassLoader WITHOUT_CONTRACT = new URLClassLoader(new URL[0], null);

  @Test
  @DisplayName("fail without openapi.yaml, saying how to fix it")
  public void failWithoutContract() {
    var error =
        assertThrows(
            IllegalStateException.class, () -> ContractFirstCheck.check(WITHOUT_CONTRACT, true));
    assertTrue(error.getMessage().contains("src/main/resources/openapi.yaml"), error.getMessage());
    assertTrue(
        error.getMessage().contains("xdyan.commons.http.openapi.required=false"),
        error.getMessage());
  }

  @Test
  @DisplayName("pass without openapi.yaml when it is not required")
  public void passWhenNotRequired() {
    assertDoesNotThrow(() -> ContractFirstCheck.check(WITHOUT_CONTRACT, false));
  }

  @Test
  @DisplayName("pass with openapi.yaml")
  public void passWithContract() {
    assertDoesNotThrow(() -> ContractFirstCheck.check(getClass().getClassLoader(), true));
  }
}
