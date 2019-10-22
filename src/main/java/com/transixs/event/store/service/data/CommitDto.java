package com.transixs.event.store.service.data;

import com.transixs.json.JsonHelper;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class CommitDto {

  private long txnReference;
  private long checkpointNumber;
  private String commitId;
  private int commitSequence;
  private int revision;
  private int items;
  private String payload;

  public void setTxnReference(long txnReference) {
    this.txnReference = txnReference;
  }

  public long getTxnReference() {
    return txnReference;
  }

  public void setCheckpointNumber(long checkpointNumber) {
    this.checkpointNumber = checkpointNumber;
  }

  public long getCheckpointNumber() {
    return checkpointNumber;
  }

  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }

  public String getCommitId() {
    return commitId;
  }

  public void setCommitSequence(int commitSequence) {
    this.commitSequence = commitSequence;
  }

  public int getCommitSequence() {
    return commitSequence;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public int getRevision() {
    return revision;
  }

  public void setItems(int items) {
    this.items = items;
  }

  public int getItems() {
    return items;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return JsonHelper.toJson(this);
  }

}

