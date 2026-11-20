package com.xdyan.commons.http.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Formats of the API: dates as "dd/MM/yyyy", date times as "dd/MM/yyyy HH:mm" and hours as "HH:mm", in
 * the zone of the Clock. Add it to the MapStruct mappers ({@code uses = DateTimeMapper.class}) for every
 * LocalDate, LocalDateTime and LocalTime field.
 */
@ApplicationScoped
public class DateTimeMapper {

  // STRICT so that dates like 30/02/2025 are rejected instead of moved to the last day of the month
  public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
      .withResolverStyle(ResolverStyle.STRICT);
  public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
      .withResolverStyle(ResolverStyle.STRICT);
  public static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm")
      .withResolverStyle(ResolverStyle.STRICT);

  public String format(LocalDate value) {
    return value == null ? null : value.format(DATE);
  }

  public LocalDate parseDate(String value) {
    return value == null ? null : LocalDate.parse(value, DATE);
  }

  public String format(LocalDateTime value) {
    return value == null ? null : value.format(DATE_TIME);
  }

  public LocalDateTime parseDateTime(String value) {
    return value == null ? null : LocalDateTime.parse(value, DATE_TIME);
  }

  public String format(LocalTime value) {
    return value == null ? null : value.format(HOUR);
  }

  public LocalTime parseHour(String value) {
    return value == null ? null : LocalTime.parse(value, HOUR);
  }
}
