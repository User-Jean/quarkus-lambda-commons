package com.xdyan.commons.xray;

/**
 * An exception caused by the request, not by the lambda (e.g. a 404 or a 409): its subsegment is
 * marked as error instead of fault, so it doesn't count as a failure of the lambda.
 */
public interface ClientError {}
