package com.transixs.event.store.service.data;

import com.transixs.json.JsonHelper;
import com.transixs.json.schemas.TransactionEvent;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class PayloadDto {

  private Map<String, String> metaData = new HashMap<>();
  private List<TransactionEvent> events = new ArrayList<>();

  public static PayloadDto fromJson(String json) {
    return JsonHelper.fromJson(json, PayloadDto.class);
  }

  public void setMetaData(Map<String, String> metaData) {
    this.metaData = metaData;
  }

  public Map<String, String> getMetaData() {
    return metaData;
  }

  public void setEvents(List<TransactionEvent> events) {
    this.events = events;
  }

  public List<TransactionEvent> getEvents() {
    return events;
  }
 
  @Override
  public String toString() {
    return JsonHelper.toJson(this);
  }

}

