package com.transixs.event.store.service;

import com.transixs.event.store.service.data.CommitDao;
import com.transixs.event.store.service.data.CommitDto;
import com.transixs.event.store.service.data.PayloadDto;
import com.transixs.json.schemas.TransactionData;
import com.transixs.json.schemas.TransactionCommit;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum TransactionEventsManager {

  INSTANCE;

  private static final Logger log = LogManager.getLogger(TransactionEventsManager.class.getName());
  private static final int CONCURRENCY_ERROR_CODE = 23505;

  private final Jdbi getJdbi() {
    return Datasource.INSTANCE.getJdbi();
  }

  protected final TransactionData fetchTransactionData(long txnReference) {
    return getJdbi().withExtension(CommitDao.class, dao -> {
      List<CommitDto> commits = dao.getCommits(txnReference);
      TransactionData td = new TransactionData();
      td.setTxnReference(txnReference);
      for(CommitDto dto : commits) {
        PayloadDto p = PayloadDto.fromJson(dto.getPayload());
        TransactionCommit tc = new TransactionCommit();
        tc.setCommitId(dto.getCommitId());
        tc.setTxnReference(dto.getTxnReference());
        tc.setCommitSequence(dto.getCommitSequence());
        tc.setRevision(dto.getRevision());
        tc.setMetaData(p.getMetaData());
        tc.setEvents(p.getEvents());
        td.getCommits().add(tc);
      }
      return td;
    });
  }

  protected final void commit(TransactionCommit commit) throws CommitException {
    getJdbi().useExtension(CommitDao.class, dao -> {
      try {
        CommitDto c = new CommitDto();
        PayloadDto p = new PayloadDto();
        c.setTxnReference(commit.getTxnReference());
        c.setCommitId(commit.getCommitId());
        c.setCommitSequence(commit.getCommitSequence() + 1);
        c.setRevision(commit.getRevision() + commit.getEvents().size());
        c.setItems(commit.getEvents().size());
        p.setMetaData(commit.getMetaData());
        p.setEvents(commit.getEvents());
        c.setPayload(p.toString());
        long checkpointNumber = dao.insert(c);
        commit.setCheckpointNumber(checkpointNumber);
        asyncDispatch(commit);
      } catch(Exception e) {
        handleException(e, commit, dao);
      }
    });
  }

  private void asyncDispatch(TransactionCommit commit) {
    EventDispatchManager.INSTANCE.dispatchCommit(commit);
  }

  protected final void handleException(Exception e, TransactionCommit commit, CommitDao dao) throws CommitException {
    // NB - If its a duplicate there is nothing the calling code can do.
    // but ignore the message. 
    // BUT if its a concurrency exception the calling code can OPT to retry.
    // This is why we return the latest transaction data
    // obviously if its an OTHER the calling cde can report a system error
    CommitException cex = new CommitException("failed to commit transaction", e);         
    cex.setType(CommitExceptionType.OTHER);

    if (e.getCause() instanceof java.sql.SQLException) {
      int errorcode = ((java.sql.SQLException)e.getCause()).getErrorCode();
      if (errorcode == CONCURRENCY_ERROR_CODE) {
        int duplicate = dao.isDuplicateCommit(commit.getTxnReference(), commit.getCommitId());
        if (duplicate >= 1) {
          cex.setType(CommitExceptionType.DUPLICATE);
          throw cex;
        } else {
          // Include latest transaction data so caller can evaluate retry
          cex.setTransactionData(fetchTransactionData(commit.getTxnReference()));
          cex.setType(CommitExceptionType.CONCURRENCY);
          throw cex;
        }
      }
    }

    throw cex;
  }

}

