package com.projectkinetile.dbinit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for catalog bootstrap behavior. */
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

  private boolean force;
  private String registryResource = "classpath:data/infrastructure-registry.json";

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public String getRegistryResource() {
    return registryResource;
  }

  public void setRegistryResource(String registryResource) {
    this.registryResource = registryResource;
  }
}
