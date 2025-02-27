import net.ltgt.gradle.errorprone.CheckSeverity.*

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")

  id("otel.jmh-conventions")

  // TODO(anuraaga): Enable animalsniffer by the time we are getting ready to release a stable
  // version. Long/DoubleAdder are not part of Android API 21 which is our current target.
  // id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry SDK Metrics"
otelJava.moduleName.set("io.opentelemetry.sdk.metrics")

dependencies {
  api(project(":api:metrics"))
  api(project(":sdk:common"))

  annotationProcessor("com.google.auto.value:auto-value")

  testAnnotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":sdk:metrics-testing"))
  testImplementation(project(":sdk:testing"))
  testImplementation("com.google.guava:guava")

  jmh(project(":sdk:trace"))
}

testing {
  suites {
    val debugEnabledTest by registering(JvmTestSuite::class) {
      targets {
        all {
          testTask.configure {
            jvmArgs("-Dotel.experimental.sdk.metrics.debug=true")
          }
        }
      }
    }
  }
}

tasks {
  named("check") {
    dependsOn(testing.suites)
  }
}
