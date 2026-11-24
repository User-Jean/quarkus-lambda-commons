package com.xdyan.commons.http.error;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RegisterForReflection
public class ErrorMessage {
  private String name;
  private String message;
  private Integer code;
  private List<Detail> errors;

  @Getter
  @Setter
  @ToString
  @RegisterForReflection
  public static class Detail {
    private String message;
    private String component;
  }
}
