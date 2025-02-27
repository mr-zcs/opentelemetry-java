plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")

  id("org.unbroken-dome.test-sets")
}

description = "OpenTelemetry SDK Auto-configuration"
otelJava.moduleName.set("io.opentelemetry.sdk.autoconfigure")

testSets {
  create("testConfigError")
  create("testFullConfig")
  create("testInitializeRegistersGlobal")
  create("testJaeger")
  create("testOtlpGrpc")
  create("testOtlpHttp")
  create("testPrometheus")
  create("testResourceDisabledByProperty")
  create("testResourceDisabledByEnv")
  create("testZipkin")
}

dependencies {
  api(project(":sdk:all"))
  api(project(":sdk:metrics"))
  api(project(":sdk-extensions:autoconfigure-spi"))

  implementation(project(":semconv"))

  compileOnly(project(":exporters:jaeger"))
  compileOnly(project(":exporters:logging"))
  compileOnly(project(":exporters:otlp:all"))
  compileOnly(project(":exporters:otlp:metrics"))
  compileOnly(project(":exporters:otlp-http:trace"))
  compileOnly(project(":exporters:otlp-http:metrics"))
  compileOnly(project(":exporters:prometheus"))
  compileOnly(project(":exporters:zipkin"))

  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(path = ":sdk:trace-shaded-deps"))

  testImplementation(project(":sdk:testing"))
  testImplementation("com.linecorp.armeria:armeria-junit5")
  testImplementation("com.linecorp.armeria:armeria-grpc")
  testRuntimeOnly("io.grpc:grpc-netty-shaded")
  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testRuntimeOnly("org.slf4j:slf4j-simple")

  add("testFullConfigImplementation", project(":extensions:aws"))
  add("testFullConfigImplementation", project(":extensions:trace-propagators"))
  add("testFullConfigImplementation", project(":exporters:jaeger"))
  add("testFullConfigImplementation", project(":exporters:logging"))
  add("testFullConfigImplementation", project(":exporters:otlp:all"))
  add("testFullConfigImplementation", project(":exporters:otlp:metrics"))
  add("testFullConfigImplementation", project(":exporters:prometheus"))
  add("testFullConfigImplementation", project(":exporters:zipkin"))
  add("testFullConfigImplementation", project(":sdk-extensions:resources"))

  add("testOtlpGrpcImplementation", project(":exporters:otlp:all"))
  add("testOtlpGrpcImplementation", project(":exporters:otlp:metrics"))
  add("testOtlpGrpcImplementation", "org.bouncycastle:bcpkix-jdk15on")
  add("testOtlpHttpImplementation", project(":exporters:otlp-http:trace"))
  add("testOtlpHttpImplementation", project(":exporters:otlp-http:metrics"))
  add("testOtlpHttpImplementation", "com.squareup.okhttp3:okhttp")
  add("testOtlpHttpImplementation", "com.squareup.okhttp3:okhttp-tls")
  add("testOtlpHttpImplementation", "org.bouncycastle:bcpkix-jdk15on")

  add("testJaegerImplementation", project(":exporters:jaeger"))
  add("testJaegerImplementation", project(":exporters:jaeger-proto"))

  add("testZipkinImplementation", project(":exporters:zipkin"))

  add("testConfigErrorImplementation", project(":extensions:trace-propagators"))
  add("testConfigErrorImplementation", project(":exporters:jaeger"))
  add("testConfigErrorImplementation", project(":exporters:logging"))
  add("testConfigErrorImplementation", project(":exporters:otlp:all"))
  add("testConfigErrorImplementation", project(":exporters:otlp:metrics"))
  add("testConfigErrorImplementation", project(":exporters:prometheus"))
  add("testConfigErrorImplementation", project(":exporters:zipkin"))
  add("testConfigErrorImplementation", "org.junit-pioneer:junit-pioneer")

  add("testPrometheusImplementation", project(":exporters:prometheus"))

  add("testResourceDisabledByPropertyImplementation", project(":sdk-extensions:resources"))
  add("testResourceDisabledByEnvImplementation", project(":sdk-extensions:resources"))
}

tasks {
  val testConfigError by existing(Test::class)

  val testFullConfig by existing(Test::class) {
    environment("OTEL_METRICS_EXPORTER", "otlp")
    environment("OTEL_RESOURCE_ATTRIBUTES", "service.name=test,cat=meow")
    environment("OTEL_PROPAGATORS", "tracecontext,baggage,b3,b3multi,jaeger,ottrace,xray,test")
    environment("OTEL_BSP_SCHEDULE_DELAY", "10")
    environment("OTEL_IMR_EXPORT_INTERVAL", "10")
    environment("OTEL_EXPORTER_OTLP_HEADERS", "cat=meow,dog=bark")
    environment("OTEL_EXPORTER_OTLP_TIMEOUT", "5000")
    environment("OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT", "2")
    environment("OTEL_TEST_CONFIGURED", "true")
    environment("OTEL_TEST_WRAPPED", "1")
  }

  val testInitializeRegistersGlobal by existing(Test::class) {
    environment("OTEL_TRACES_EXPORTER", "none")
  }

  val testJaeger by existing(Test::class) {
    environment("OTEL_TRACES_EXPORTER", "jaeger")
    environment("OTEL_BSP_SCHEDULE_DELAY", "10")
  }

  val testOtlpGrpc by existing(Test::class) {
    environment("OTEL_METRICS_EXPORTER", "otlp")
  }

  val testOtlpHttp by existing(Test::class) {
    environment("OTEL_METRICS_EXPORTER", "otlp")
  }

  val testZipkin by existing(Test::class) {
    environment("OTEL_TRACES_EXPORTER", "zipkin")
    environment("OTEL_BSP_SCHEDULE_DELAY", "10")
  }

  val testPrometheus by existing(Test::class) {
    environment("OTEL_TRACES_EXPORTER", "none")
    environment("OTEL_METRICS_EXPORTER", "prometheus")
    environment("OTEL_IMR_EXPORT_INTERVAL", "10")
  }

  val testResourceDisabledByProperty by existing(Test::class) {
    jvmArgs("-Dotel.java.disabled.resource-providers=io.opentelemetry.sdk.extension.resources.OsResourceProvider,io.opentelemetry.sdk.extension.resources.ProcessResourceProvider")
    // Properties win, this is ignored.
    environment("OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS", "io.opentelemetry.sdk.extension.resources.ProcessRuntimeResourceProvider")
    environment("OTEL_TRACES_EXPORTER", "none")
    environment("OTEL_METRICS_EXPORTER", "none")
  }

  val testResourceDisabledByEnv by existing(Test::class) {
    environment("OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS", "io.opentelemetry.sdk.extension.resources.OsResourceProvider,io.opentelemetry.sdk.extension.resources.ProcessResourceProvider")
    environment("OTEL_TRACES_EXPORTER", "none")
    environment("OTEL_METRICS_EXPORTER", "none")
  }

  check {
    dependsOn(
      testConfigError,
      testFullConfig,
      testInitializeRegistersGlobal,
      testJaeger,
      testOtlpGrpc,
      testOtlpHttp,
      testPrometheus,
      testZipkin,
      testResourceDisabledByProperty,
      testResourceDisabledByEnv
    )
  }
}
