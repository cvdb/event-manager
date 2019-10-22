package com.transixs.event.store.service;

import com.transixs.config.manager.EnvVarHelper;
import com.transixs.json.schemas.TransactionCommit;
import com.transixs.json.schemas.TransactionEvent;
import com.transixs.json.schemas.TransactionEventType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// @Disabled("Disabled - clashes with other tests")
class TransactionEventsManagerTests {

  @BeforeAll
  static void initAll() {
    Map<String, String> newenv = new HashMap();
    newenv.put(ServiceConfig.DATASOURCE_URL.name(), "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL;");
    newenv.put(ServiceConfig.DATASOURCE_USERNAME.name(), "");
    newenv.put(ServiceConfig.DATASOURCE_PASSWORD.name(), "");
    try {
      EnvVarHelper.setEnv(newenv);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  @Test
  void commit_success() {
    TransactionCommit commit = new TransactionCommit();

    commit.setCommitId("12345");
    commit.setTxnReference(12345);
    commit.setCommitSequence(1);
    commit.setRevision(1);
    commit.getMetaData().put("one", "one");

    TransactionEvent e1 = new TransactionEvent();
    e1.setEventType(TransactionEventType.TransactionCompleted);
    e1.getEventData().put("e1_key", "e1_value");
    commit.getEvents().add(e1);

    try {
      TransactionEventsManager.INSTANCE.commit(commit);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  @Test
  void commit_duplicate() {
    TransactionCommit commit = new TransactionCommit();

    commit.setCommitId("12345");
    commit.setTxnReference(12345);
    commit.setCommitSequence(1);
    commit.setRevision(1);
    commit.getMetaData().put("one", "one");

    TransactionEvent e1 = new TransactionEvent();
    e1.setEventType(TransactionEventType.TransactionCompleted);
    e1.getEventData().put("e1_key", "e1_value");
    commit.getEvents().add(e1);

    try {
      TransactionEventsManager.INSTANCE.commit(commit);
      TransactionEventsManager.INSTANCE.commit(commit);
    } catch(CommitException e){
      assertEquals(e.getType(), CommitExceptionType.DUPLICATE, "expected DUPLICATE exception type");
    } catch(Exception ex){
      ex.printStackTrace();
    }
  }

  @Test
  void commit_concurrent() {
    TransactionCommit commit = new TransactionCommit();

    commit.setCommitId("22222");
    commit.setTxnReference(22222);
    commit.setCommitSequence(2);
    commit.setRevision(1);
    commit.getMetaData().put("one", "one");

    TransactionEvent e1 = new TransactionEvent();
    e1.setEventType(TransactionEventType.TransactionCompleted);
    e1.getEventData().put("e1_key", "e1_value");
    commit.getEvents().add(e1);

    try {
      TransactionEventsManager.INSTANCE.commit(commit);
      commit.setCommitId("33333");
      TransactionEventsManager.INSTANCE.commit(commit);
    } catch(CommitException e){
      assertEquals(e.getType(), CommitExceptionType.CONCURRENCY, "expected CONCURRENCY exception type");
    } catch(Exception ex){
      ex.printStackTrace();
    }
  }

}
