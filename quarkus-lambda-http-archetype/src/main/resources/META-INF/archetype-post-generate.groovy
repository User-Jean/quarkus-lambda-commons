// Turns the example project of archetype-resources into the new one: the same replacements on every file, so the
// project of the archetype is a real one that builds and passes its tests (com.example, quarkus-lambda-http-template,
// /api/v1). Runs after the files are copied, also in the test of the archetype.
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

Path dir = Paths.get(request.outputDirectory, request.artifactId)
String groupId = request.groupId
String artifactId = request.artifactId
String pkg = request.properties.getProperty('package') ?: groupId
String rootPath = request.properties.getProperty('rootPath') ?: '/api/v1'
String commonsVersion = request.archetypeVersion

// The validationRegex of archetype-metadata.xml only applies when the values are asked (not with -B): checked here
def errors = []
if (!(artifactId ==~ /[a-z][a-z0-9-]*[a-z0-9]/)) {
  errors << "artifactId must be kebab-case, e.g. lambda-http-orders (was ${artifactId})"
}
if (!(groupId ==~ /[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+/)) {
  errors << "groupId must be like com.acme.orders (was ${groupId})"
}
if (!(pkg ==~ /[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+/)) {
  errors << "package must be like com.acme.orders (was ${pkg})"
}
if (!(rootPath ==~ /(\/[a-z0-9-]+)+/)) {
  errors << "rootPath must be like /orders/v1 (was ${rootPath})"
}
if (errors) {
  dir.toFile().deleteDir()
  throw new IllegalArgumentException("Project not created: " + errors.join('; '))
}

Files.move(dir.resolve('__gitignore__'), dir.resolve('.gitignore'))
dir.resolve('mvnw').toFile().setExecutable(true, false)

// In order: the code (<package>.app) and the generated API (<groupId>.api, apiPackage of the parent) before the rest
def replacements = [
    [~/\bcom\.example\.app\b/, pkg + '.app'],
    [~/\bcom\.example\.api\b/, groupId + '.api'],
    [~/\bcom\/example\//, groupId.replace('.', '/') + '/'],
    [~/\bcom\.example(?=[.";<\s])/, groupId],
    [~/quarkus-lambda-http-template/, artifactId],
    [~/Quarkus Lambda HTTP Template/, artifactId],
    [~/\/api\/v1(?=[\/"\s`]|$)/, rootPath],
    [~/QUARKUS_LAMBDA_COMMONS_VERSION/, commonsVersion],
]

Files.walk(dir).withCloseable { paths ->
  paths.filter { Files.isRegularFile(it) }.forEach { Path file ->
    String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    String result = text
    replacements.each { pattern, value ->
      result = result.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(value))
    }
    if (result != text) {
      Files.write(file, result.getBytes(StandardCharsets.UTF_8))
    }
  }
}

println ""
println "${artifactId}: code in ${pkg}.app, API in ${groupId}.api, root path ${rootPath}, quarkus-lambda-platform ${commonsVersion}"
println "Still to set, each marked CHANGE_ME (README.md, Primeros pasos):"
Files.walk(dir).withCloseable { paths ->
  paths.filter { Files.isRegularFile(it) && !it.toString().endsWith('.md') }.forEach { Path file ->
    Files.readAllLines(file, StandardCharsets.UTF_8).eachWithIndex { line, i ->
      if (line.contains('CHANGE_ME')) {
        println "  ${dir.relativize(file)}:${i + 1}: ${line.trim()}"
      }
    }
  }
}
println "Then: cd ${artifactId} && ./mvnw clean verify"
