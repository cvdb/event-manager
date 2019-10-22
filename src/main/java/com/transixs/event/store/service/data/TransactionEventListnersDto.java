package com.transixs.event.store.service.data;

import com.transixs.json.schemas.TransactionEventListner;
import com.transixs.json.JsonHelper;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class TransactionEventListnersDto {

  private List<TransactionEventListner> eventListners = new ArrayList<>();

  public static TransactionEventListnersDto fromJson(String json) {
    return JsonHelper.fromJson(json, TransactionEventListnersDto.class);
  }

  public void setEventListners(List<TransactionEventListner> eventListners) {
    this.eventListners = eventListners;
  }

  public List<TransactionEventListner> getEventListners() {
    return eventListners;
  }
 
  @Override
  public String toString() {
    return JsonHelper.toJson(this);
  }

}

