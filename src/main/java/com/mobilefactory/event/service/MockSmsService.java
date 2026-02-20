package com.mobilefactory.event.service;

import org.springframework.stereotype.Service;

@Service
public class MockSmsService implements SmsService {
    @Override
    public void sendSms(String phoneNumber, String message) {
        System.out.println("================ SMS SEND ================");
        System.out.println("To: " + phoneNumber);
        System.out.println("Content: " + message);
        System.out.println("==========================================");
    }
}
