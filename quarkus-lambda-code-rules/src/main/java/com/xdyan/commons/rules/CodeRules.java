package com.xdyan.commons.rules;

/**
 * The rules of the code of the Quarkus lambdas: resources of this jar, read by the Checkstyle and PMD plugins
 * configured in quarkus-lambda-dependencies. The format is google-java-format (Spotless), not configured here.
 *
 * <p>Only the paths of the resources: the jar has this class because Maven Central requires its javadoc.
 */
public final class CodeRules {

  /** Names, imports and braces. */
  public static final String CHECKSTYLE = "quarkus-lambda/checkstyle.xml";

  /** Bugs and bad practices. */
  public static final String PMD_RULESET = "quarkus-lambda/pmd-ruleset.xml";

  private CodeRules() {}
}
