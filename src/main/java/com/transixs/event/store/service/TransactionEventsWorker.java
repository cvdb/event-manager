package com.transixs.event.store.service;

import com.transixs.json.ServiceException;
import com.transixs.json.schemas.ServiceError;
import com.transixs.json.schemas.TransactionCommit;
import com.transixs.json.schemas.TransactionData;
import com.transixs.json.schemas.Request;
import com.transixs.json.schemas.Response;
import com.transixs.json.JsonHelper;
import com.transixs.mq.WorkResult;
import com.transixs.mq.Resource;
import com.transixs.mq.Action;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Resource("transaction-events")
public class TransactionEventsWorker {

  private static final Logger log = LogManager.getLogger(TransactionEventsWorker.class.getName());

  @Action("fetch")
  public WorkResult fetchData(String message) {
    try {
      Request request = Request.fromJson(message);
      TransactionData data = request.getMessageData(TransactionData.class);
      data = TransactionEventsManager.INSTANCE.fetchTransactionData(data.getTxnReference());
      return WorkResult.COMPLETE(Response.toResult(request, data));
    } catch(Exception e) {
      return WorkResult.COMPLETE(Response.toResult(e, "failed to fetch transaction data"));
    }
  }

  @Action("commit")
  public WorkResult commit(String message) {
    try {
      Request request = Request.fromJson(message);
      TransactionCommit commit = request.getMessageData(TransactionCommit.class);
      TransactionEventsManager.INSTANCE.commit(commit);
      return WorkResult.COMPLETE(Response.toResult(request, "events committed successfully"));
    } catch(CommitException cex) {
      return handleCommitException(cex);
    } catch(Exception e) {
      return WorkResult.COMPLETE(Response.toResult(e, "failed to commit events"));
    }
  }

  private WorkResult handleCommitException(CommitException cex) {
    if (cex.getType().equals(CommitExceptionType.CONCURRENCY)) {
      return concurrencyError(cex);
    } else if (cex.getType().equals(CommitExceptionType.DUPLICATE)) {
      return duplicateCommitError(cex);
    } else {
      return WorkResult.COMPLETE(Response.toResult(cex, "failed to commit events"));
    }
  }

  private WorkResult concurrencyError(CommitException cex) {
    // In this case we need to return the latest transaction data so caller can re-try
    ServiceException se = new ServiceException(ServiceError.CONCURRENCY_ERROR, cex);
    Response resp = new Response(se);
    resp.setMessageData(cex.getTransactionData());
    return WorkResult.COMPLETE(resp.toString());
  }

  private WorkResult duplicateCommitError(CommitException cex) {
    // Nothing the caller needs to do, simply indicate it was a duplicate commit
    ServiceException se = new ServiceException(ServiceError.DUPLICATE_COMMIT, cex);
    Response resp = new Response(se);
    return WorkResult.COMPLETE(resp.toString());
  }

}

