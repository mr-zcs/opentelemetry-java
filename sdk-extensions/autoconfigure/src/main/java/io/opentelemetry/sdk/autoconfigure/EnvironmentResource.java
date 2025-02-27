/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Collections;

/**
 * Factory for a {@link Resource} which parses the standard "otel.resource.attributes" system
 * property or OTEL_RESOURCE_ATTRIBUTES environment variable. Will also use
 * OTEL_SERVICE_NAME/otel.service.name to specifically set the service name.
 *
 * @deprecated Use {@link AutoConfiguredOpenTelemetrySdk#getResource()}.
 */
@Deprecated
public final class EnvironmentResource {

  // Visible for testing
  static final String ATTRIBUTE_PROPERTY = "otel.resource.attributes";
  static final String SERVICE_NAME_PROPERTY = "otel.service.name";

  /**
   * Returns a {@link Resource} which contains information from the standard
   * "otel.resource.attributes"/"otel.service.name" system properties or
   * OTEL_RESOURCE_ATTRIBUTES/OTEL_SERVICE_NAME environment variables.
   *
   * @deprecated Use the information retrievable from {@link
   *     AutoConfiguredOpenTelemetrySdk#getResource()}.
   */
  @Deprecated
  public static Resource get() {
    return create(DefaultConfigProperties.get(Collections.emptyMap()));
  }

  static Resource create(ConfigProperties config) {
    return Resource.create(getAttributes(config), ResourceAttributes.SCHEMA_URL);
  }

  // visible for testing
  static Attributes getAttributes(ConfigProperties configProperties) {
    AttributesBuilder resourceAttributes = Attributes.builder();
    configProperties.getMap(ATTRIBUTE_PROPERTY).forEach(resourceAttributes::put);
    String serviceName = configProperties.getString(SERVICE_NAME_PROPERTY);
    if (serviceName != null) {
      resourceAttributes.put(ResourceAttributes.SERVICE_NAME, serviceName);
    }
    return resourceAttributes.build();
  }

  private EnvironmentResource() {}
}
