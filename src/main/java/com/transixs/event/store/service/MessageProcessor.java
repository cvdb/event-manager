package com.transixs.event.store.service;

import com.transixs.mq.IMessageProcessor;
import com.transixs.json.schemas.TransactionEventListner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageProcessor implements IMessageProcessor{

  private static final Logger log = LogManager.getLogger(MessageProcessor.class.getName());

  public void process(String message) {
    try {
      TransactionEventListner tel = TransactionEventListner.fromJson(message);
      EventListnerManager.INSTANCE.addListner(tel);
    } catch(Exception e) {
      log.error("FAILED to process message " + message, e);
    }
  }

}
