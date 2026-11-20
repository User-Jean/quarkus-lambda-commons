package com.xdyan.commons.http.error;

import jakarta.ws.rs.BadRequestException;
import lombok.Getter;

import java.util.List;

/**
 * 400 with one error per field (e.g. each invalid field of a form), so the form can mark all of them.
 */
@Getter
public class InvalidFieldsException extends BadRequestException {

  private final List<String> messages;

  public InvalidFieldsException(List<String> messages) {
    super(String.join("; ", messages));
    this.messages = List.copyOf(messages);
  }
}
