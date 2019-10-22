package com.transixs.event.store.service;

import com.transixs.event.store.service.data.TransactionEventListnersDto;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.transixs.json.schemas.TransactionEventListner;
import com.transixs.json.schemas.TransactionEvent;
import com.transixs.json.schemas.TransactionCommit;
import com.transixs.json.schemas.TransactionData;
import com.transixs.json.schemas.IdentifierType;
import com.transixs.json.schemas.ConfigData;
import com.transixs.json.schemas.Request;
import com.transixs.json.schemas.Response;
import com.transixs.mq.MessageQueueAPI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.lang.Runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum EventListnerManager {

  INSTANCE;

  private final Logger log = LogManager.getLogger(EventListnerManager.class.getName());
  private final Map<String, TransactionEventListner> eventListners = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> saverHandle; 

  {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        scheduler.shutdown();
      }
    });
  }

  /*================================================================
   * NB NB NB NB
   * -----------
   *  Events are dispatched to a WORK QUEUE and there may be many 
   *  instances of each service running concurrently. Each service
   *  instance will register an event listner on startup
   *
   ================================================================*/
  protected void addListner(TransactionEventListner listner) {
    if (listner == null || listner.getEvents() == null || listner.getEvents().isEmpty()) {
      log.error("Failed to add transaction event listner. Listner has no events.");
      return;
    }
    eventListners.put(listner.getServiceName(), listner);
    scheduleSaving();
  }

  // Do this so we can batch saving
  // in case multiple services start up within short timeframe
  private void scheduleSaving() {
    if (saverHandle != null) {
      saverHandle.cancel(false); 
    }

    TransactionEventListnersDto dto = new TransactionEventListnersDto();
    eventListners.forEach((serviceName, listner) -> {
      dto.getEventListners().add(listner);
    });
    //TODO: is storing this in the DB secure?
    //TODO: also why not save it in this services DB?
    ConfigData cd = new ConfigData(IdentifierType.NONE, "NONE", "transaction.event.listners", dto.toString());

    final Runnable saver = new Runnable() {
      public void run() { 
        Response response = Response.fromJson(MessageQueueAPI.INSTANCE.sendSync("config-service", "config", "upsert", new Request(cd).toString()));
        try {
          response.raiseOnError();
        } catch(Exception e){
          log.error("failed to save transaction listners", e);
        }
      }
    };

    saverHandle = scheduler.schedule(saver, 10, TimeUnit.SECONDS);
  }

  // THis is called on start-up to load previously saved listners
  protected void loadTransactionListners() {
    final Runnable loader = new Runnable() {
      public void run() { 
        try {
          ConfigData cd = new ConfigData(IdentifierType.NONE, "NONE", "transaction.event.listners");
          Response response = Response.fromJson(MessageQueueAPI.INSTANCE.sendSync("config-service", "config", "fetch-exact", new Request(cd).toString()));
          cd = response.getMessageData(ConfigData.class);     
          TransactionEventListnersDto dto = null;
          if (cd.getSinglePropertyValue() != null) {
            dto = TransactionEventListnersDto.fromJson(cd.getSinglePropertyValue());
          }
          if (dto != null && dto.getEventListners() != null) {
            dto.getEventListners().forEach(listner -> eventListners.put(listner.getServiceName(), listner)); 
          } 
        } catch(Exception e){
          log.error("failed to load transaction listners", e);
        }
      }
    };

    scheduler.schedule(loader, 5, TimeUnit.SECONDS);
  }

  protected Map<String, List<TransactionEvent>> getEventsToDispatch(TransactionCommit commit) {
    Map<String, List<TransactionEvent>> serviceEvents = new HashMap<>();

    if (commit == null) {
      return serviceEvents;
    }

    for (TransactionEventListner listner : eventListners.values()) {
      for(TransactionEvent event : commit.getEvents()) {
        if (eventForListner(event, listner)) {
          if (!serviceEvents.containsKey(listner.getServiceName())) {
            serviceEvents.put(listner.getServiceName(), new ArrayList<TransactionEvent>()); 
          }
          serviceEvents.get(listner.getServiceName()).add(event);
        }
      }
    }

    return serviceEvents;
  }

  private boolean eventForListner(TransactionEvent event, TransactionEventListner listner) {
    if (listner == null || listner.getEvents() == null || listner.getEvents().isEmpty() ||
        event == null || event.getEventType() == null) {
      return false; 
    } 
    return listner.getEvents().contains(event.getEventType());
  }

}


