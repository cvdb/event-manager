package com.transixs.event.store.service;

import com.transixs.event.store.service.data.*;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import com.zaxxer.hikari.HikariDataSource;

public enum Datasource {

  INSTANCE;

  private Jdbi jdbi = null;

  public synchronized Jdbi getJdbi() {
    if (jdbi != null) {
      return jdbi;
    }
    jdbi = Jdbi.create(getDataSource());
    jdbi.installPlugin(new SqlObjectPlugin());
    createTables();
    return jdbi;
  }

  private HikariDataSource getDataSource() {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(ServiceConfig.DATASOURCE_URL.getValue());
    ds.setUsername(ServiceConfig.DATASOURCE_USERNAME.getValue());
    ds.setPassword(ServiceConfig.DATASOURCE_PASSWORD.getValue());
    return ds;
  }

  private void createTables() {
    jdbi.useExtension(CommitDao.class, dao -> {
      try {
        dao.createTable();
      } catch(Exception e){
        // ignore
      }
    });
  }

}


