package com.transixs.event.store.service;

import com.transixs.mq.IMessageProcessorFactory;
import com.transixs.mq.IMessageProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageProcessorFactory implements IMessageProcessorFactory {

  private static final Logger log = LogManager.getLogger(MessageProcessorFactory.class.getName());

  public IMessageProcessor getMessageProcessor() {
    return new MessageProcessor();
  }

}
