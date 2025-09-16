package com.petd.paymentcrawl.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

public interface TiktokCallApi {
  Map<String, String> createParameters ();
  TiktokApiResponse callApi()  throws JsonProcessingException ;
}
