package com.xdyan.commons.http;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Keeps the log records of com.xdyan.commons while a test runs, with the MDC of each one.
 */
public class LogCapture extends Handler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger("com.xdyan.commons");

  public final List<org.jboss.logmanager.ExtLogRecord> records = new CopyOnWriteArrayList<>();

  public static LogCapture start() {
    var capture = new LogCapture();
    LOGGER.addHandler(capture);
    return capture;
  }

  @Override
  public void publish(LogRecord record) {
    if (isLoggable(record) && record instanceof org.jboss.logmanager.ExtLogRecord ext) {
      ext.copyMdc();
      records.add(ext);
    }
  }

  public List<String> messages() {
    return records.stream().map(r -> r.getLevel() + " " + r.getFormattedMessage()).toList();
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
    LOGGER.removeHandler(this);
  }
}
