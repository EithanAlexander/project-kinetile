package com.projectkinetile.physicsengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** OpenAPI / Swagger metadata for the read-only REST API. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI physicsEngineOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Project Kinetile Physics Engine API")
                .description(
                    "Read-only analytics and infrastructure catalog for piezoelectric tile"
                        + " feasibility data.")
                .version("v1"));
  }
}
