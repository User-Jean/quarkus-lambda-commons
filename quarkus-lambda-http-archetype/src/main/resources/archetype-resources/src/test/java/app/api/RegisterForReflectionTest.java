package com.example.app.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The native image only sees the Jackson annotations of the classes registered for reflection: an
 * enum without it is written with name() instead of its @JsonValue (e.g. UP instead of up), and the
 * JVM tests don't notice. The pom adds @RegisterForReflection to every generated model and enum
 * (additionalModelTypeAnnotations and additionalEnumTypeAnnotations); this checks none is left out.
 */
public class RegisterForReflectionTest {

  private static final String MODEL_PACKAGE = "com.example.api.model";

  @Test
  @DisplayName("register every generated model and enum for reflection")
  public void registerEveryGeneratedModelAndEnum() throws Exception {
    List<String> missing = new ArrayList<>();
    List<Class<?>> models = generatedModels();
    assertFalse(models.isEmpty());
    for (Class<?> model : models) {
      if (!model.isAnnotationPresent(RegisterForReflection.class)) {
        missing.add(model.getSimpleName());
      }
    }
    assertEquals(List.of(), missing, "Generated models and enums without @RegisterForReflection");
  }

  private static List<Class<?>> generatedModels()
      throws URISyntaxException, ClassNotFoundException {
    URL url =
        Thread.currentThread().getContextClassLoader().getResource(MODEL_PACKAGE.replace('.', '/'));
    assertNotNull(url, MODEL_PACKAGE);
    File[] files =
        new File(url.toURI())
            .listFiles((dir, name) -> name.endsWith(".class") && !name.contains("$"));
    assertNotNull(files);
    List<Class<?>> models = new ArrayList<>();
    for (File file : files) {
      models.add(Class.forName(MODEL_PACKAGE + "." + file.getName().replace(".class", "")));
    }
    return models;
  }
}
