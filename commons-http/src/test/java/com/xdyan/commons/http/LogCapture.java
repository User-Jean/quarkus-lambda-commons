package com.xdyan.commons.http;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Keeps the log records of a category (com.xdyan.commons by default) while a test runs, with the
 * MDC of each one.
 */
public class LogCapture extends Handler implements AutoCloseable {

  private final Logger logger;

  public final List<org.jboss.logmanager.ExtLogRecord> records = new CopyOnWriteArrayList<>();

  private LogCapture(String category) {
    this.logger = Logger.getLogger(category);
  }

  public static LogCapture start() {
    return start("com.xdyan.commons");
  }

  /**
   * The records of a category, also below the level of the app (ERROR in application.properties)
   * when the category logs them.
   */
  public static LogCapture start(String category) {
    var capture = new LogCapture(category);
    capture.logger.addHandler(capture);
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
  public void flush() {}

  @Override
  public void close() {
    logger.removeHandler(this);
  }
}
