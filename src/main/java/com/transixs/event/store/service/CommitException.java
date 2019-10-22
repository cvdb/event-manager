package com.transixs.event.store.service;

import com.transixs.json.schemas.TransactionData;

public class CommitException extends Exception {

  private TransactionData transactionData;
  private CommitExceptionType type = CommitExceptionType.OTHER;

  public CommitException(String message) {
    super(message);
  }

  public CommitException(String message, Throwable cause) {
    super(message, cause);
  }

  public void setTransactionData(TransactionData transactionData) {
    this.transactionData = transactionData;
  }

  public TransactionData getTransactionData() {
    return transactionData;
  }

  public void setType(CommitExceptionType type) {
    this.type = type;
  }

  public CommitExceptionType getType() {
    return type;
  }

}
