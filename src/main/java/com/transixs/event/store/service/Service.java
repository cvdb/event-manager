package com.transixs.event.store.service;

import com.transixs.mq.MessageQueueAPI;

public class Service {
  public static void main( String[] args ) {
    MessageQueueAPI.INSTANCE.receivePubSub(new MessageProcessorFactory(), "register-transaction-event-listner");
    MessageQueueAPI.INSTANCE.receive(new WorkerFactory());
    EventDispatchManager.INSTANCE.dispatchFailed();
    EventListnerManager.INSTANCE.loadTransactionListners();
    MessageQueueAPI.INSTANCE.waitUntilShutdown();
  }
}
