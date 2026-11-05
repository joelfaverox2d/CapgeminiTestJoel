package com.capgemini.test.code.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificationClient", url = "${external.service.url}")
public interface NotificationClient {

  @PostMapping(value = "/email")
  void sendEmail(@RequestBody EmailRequest request);

  @PostMapping(value = "/sms")
  void sendSms(@RequestBody SmsRequest request);
}
