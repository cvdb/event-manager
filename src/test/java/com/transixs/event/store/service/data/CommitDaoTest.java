package com.transixs.event.store.service.data;

import com.transixs.json.schemas.TransactionCommit;
import com.transixs.json.schemas.TransactionEvent;
import com.transixs.json.schemas.TransactionEventType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Disabled("Disabled - clashes with other tests")
class CommitDaoTests {

  private static final Logger log = LogManager.getLogger(CommitDaoTests.class.getName());

  //==========================================
  // NOTE: singleton enum instances in other tests 
  // will keep in memory DB so this will fail
  //==========================================
  // @Test
  // void create_table_many_times() {
  //   Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
  //   jdbi.installPlugin(new SqlObjectPlugin());

  //   jdbi.useExtension(CommitDao.class, dao -> {
  //     dao.createTable();
  //     try {
  //       dao.createTable();
  //       fail("should throw an exception");
  //     } catch(Exception e){
  //     }
  //   });
  // }

  private void createTable() {
    Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.useExtension(CommitDao.class, dao -> {
      try {
        dao.createTable();
      } catch(Exception e){
      }
    });
  }

  // @Test
  // void insert_duplicate_commit() {
  //   Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
  //   jdbi.installPlugin(new SqlObjectPlugin());

  //   createTable();

  //   jdbi.useExtension(CommitDao.class, dao -> {
  //     insert(jdbi, "11111", 1);
  //     try {
  //       insert(jdbi, "11111", 1);
  //       fail("expect exception here");
  //     } catch(Exception e){
  //       if (e.getCause() instanceof java.sql.SQLException) {
  //         int errorcode = ((java.sql.SQLException)e.getCause()).getErrorCode();
  //         assertEquals(23505, errorcode, "expected 23505 error code");
  //         int dup = dao.isDuplicateCommit(1234568, "11111");
  //         assertEquals(1, dup, "expected duplicate to be 1");
  //       }
  //     }
  //     List<CommitDto> commits = dao.getCommits(1234568);
  //     for(CommitDto dto : commits) {
  //       System.out.println(dto.toString());
  //     }
  //   });
  // }

  // @Test
  // void insert_duplicate_commit_seq() {
  //   Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
  //   jdbi.installPlugin(new SqlObjectPlugin());

  //   createTable();

  //   jdbi.useExtension(CommitDao.class, dao -> {
  //     insert(jdbi, "11111", 1);
  //     try {
  //       insert(jdbi, "22222", 1);
  //       fail("expect exception here");
  //     } catch(Exception e){
  //       if (e.getCause() instanceof java.sql.SQLException) {
  //         int errorcode = ((java.sql.SQLException)e.getCause()).getErrorCode();
  //         assertEquals(23505, errorcode, "expected 23505 error code");
  //         int dup = dao.isDuplicateCommit(1234568, "22222");
  //         assertEquals(0, dup, "expected duplicate to be 1");
  //       }
  //     }
  //   });
  // }

  @Test
  void insert_data_too_long() {
    Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
    jdbi.installPlugin(new SqlObjectPlugin());

    jdbi.useExtension(CommitDao.class, dao -> {
      dao.createTable();
      // insert(jdbi, "111111111111111111111111111111111111111111111111111111111111111111", 1);
      try {
        insert(jdbi, "222222222222222222222222222222222222222222222222222222222222222", 1);
      } catch(Exception e){
        log.error("SHOULD BE MASKED: ", e);
      }
    });
  }

  private void insert(Jdbi jdbi, String commitId, int seq) {
    jdbi.useExtension(CommitDao.class, dao -> {
      CommitDto c = new CommitDto();
      PayloadDto p = new PayloadDto();
      c.setTxnReference(1234567 + seq);
      c.setCommitId(commitId);
      c.setCommitSequence(seq);
      c.setRevision(2);
      c.setItems(2);
      p.getMetaData().put("key", "value");

      TransactionEvent e1 = new TransactionEvent();
      e1.setEventType(TransactionEventType.TransactionCompleted);
      e1.getEventData().put("e1_key", "e1_value");
      p.getEvents().add(e1);

      TransactionEvent e2 = new TransactionEvent();
      e2.setEventType(TransactionEventType.TransactionCompleted);
      e2.getEventData().put("e2_key", "e2_value");
      e2.getEventData().put("carddata", "4564456445644564");
      p.getEvents().add(e2);

      c.setPayload(p.toString());
      dao.insert(c);
    });
  }

}
