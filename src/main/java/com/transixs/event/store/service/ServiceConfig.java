package com.transixs.event.store.service;

import com.transixs.config.manager.IConfig;
import com.transixs.config.manager.ConfigResolver;

public enum ServiceConfig implements IConfig {

  DATASOURCE_URL, DATASOURCE_USERNAME, DATASOURCE_PASSWORD;

  @Override
  public String getValue() {
    return ConfigResolver.getProperty(this);
  }

}


