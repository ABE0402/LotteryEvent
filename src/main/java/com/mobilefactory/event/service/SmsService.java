package com.mobilefactory.event.service;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}
