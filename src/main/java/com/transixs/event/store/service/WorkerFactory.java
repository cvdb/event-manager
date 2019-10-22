package com.transixs.event.store.service;

import java.util.List;
import java.util.Arrays;

import com.transixs.mq.IWorkerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorkerFactory implements IWorkerFactory {

  private static final Logger log = LogManager.getLogger(WorkerFactory.class.getName());

  public Object getWorker(Class clazz) {
    return new TransactionEventsWorker();
  }

  public List<Class> getWorkerClasses() {
    return Arrays.asList(TransactionEventsWorker.class);
  }

}
