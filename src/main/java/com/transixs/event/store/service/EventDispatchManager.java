package com.transixs.event.store.service;

import com.transixs.event.store.service.data.TransactionEventListnersDto;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.transixs.json.schemas.Request;
import com.transixs.json.schemas.TransactionEventListner;
import com.transixs.json.schemas.TransactionEvent;
import com.transixs.json.schemas.TransactionEvents;
import com.transixs.json.schemas.TransactionCommit;
import com.transixs.json.schemas.TransactionData;
import com.transixs.json.schemas.IdentifierType;
import com.transixs.json.schemas.ConfigData;
import com.transixs.json.schemas.Response;
import com.transixs.mq.MessageQueueAPI;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.Jdbi;
import com.transixs.event.store.service.data.CommitDao;
import com.transixs.event.store.service.data.CommitDto;
import com.transixs.event.store.service.data.PayloadDto;
import java.lang.Runtime;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum EventDispatchManager {

  INSTANCE;

  private final Logger log = LogManager.getLogger(EventDispatchManager.class.getName());
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        executor.shutdown();
      }
    });
  }

  private final Jdbi getJdbi() {
    return Datasource.INSTANCE.getJdbi();
  }

  private final void updateDispatched(long checkpointNumber) {
    getJdbi().useExtension(CommitDao.class, dao -> {
      long timestamp = new Date().getTime();
      dao.updateDispatched(timestamp, checkpointNumber);
    });
  }

  private final List<TransactionCommit> fetchNonDispatched() {
    List<CommitDto> commits = getJdbi().withExtension(CommitDao.class, dao -> dao.getNonDispatched());
    List<TransactionCommit> nonDispatchedCommits = new ArrayList<>();

    for(CommitDto dto : commits) {
      PayloadDto p = PayloadDto.fromJson(dto.getPayload());
      TransactionCommit tc = new TransactionCommit();
      tc.setCommitId(dto.getCommitId());
      tc.setTxnReference(dto.getTxnReference());
      tc.setCommitSequence(dto.getCommitSequence());
      tc.setRevision(dto.getRevision());
      tc.setMetaData(p.getMetaData());
      tc.setEvents(p.getEvents());
      nonDispatchedCommits.add(tc);
    }

    return nonDispatchedCommits;
  }

  protected final void dispatchCommit(TransactionCommit commit) {
    executor.execute(() -> runDispatchCommit(commit));
  }

  // this will be called on start-up to ensure we dispatch any
  // commits that failed to be dispatched. Since we run in Kubernetes
  // any service that dies will be auto-restarted.
  protected final void dispatchFailed() {
    List<TransactionCommit> nonDispatchedCommits = fetchNonDispatched();
    for (TransactionCommit commit : nonDispatchedCommits) {
      dispatchCommit(commit); 
    }
  }

  private final String TRANSACTION_EVENT_RESOURCE = "transaction";
  private final String TRANSACTION_EVENT_ACTION = "event";

  // NB - TODO - add an alert to monitor dispatch failures.
  // TODO - if the info is useful we could send the WHOLE commit here. Perhaps the register
  // listner request should have an option to indicate wither event or commit here.
  private final void runDispatchCommit(TransactionCommit commit) {
    try {
      Map<String, List<TransactionEvent>> serviceEvents = EventListnerManager.INSTANCE.getEventsToDispatch(commit);
      for(Map.Entry<String, List<TransactionEvent>> entry : serviceEvents.entrySet()) {
        dispatchEvents(entry.getKey(), entry.getValue());
      }
      updateDispatched(commit.getCheckpointNumber());
    } catch(Exception e) {
      log.error("DISPATCH-FAILURE - failed to display commit", e);
    }
  }

  private void dispatchEvents(String serviceName, List<TransactionEvent> events) {
    log.info("dispatching transaction events to service: " + serviceName);
    TransactionEvents txnEvents = new TransactionEvents();
    txnEvents.setTxnReference(events.get(0).getTxnReference()); // Will always e at least 1
    for(TransactionEvent e : events) {
      txnEvents.getEvents().add(e.getEventType());
    }
    MessageQueueAPI.INSTANCE.sendAsync(serviceName, TRANSACTION_EVENT_RESOURCE, TRANSACTION_EVENT_ACTION, new Request(txnEvents).toString());
  }

}


