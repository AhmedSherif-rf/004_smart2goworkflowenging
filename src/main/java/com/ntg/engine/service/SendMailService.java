package com.ntg.engine.service;

import com.ntg.engine.entites.SendMail;

import javax.mail.MessagingException;

public interface SendMailService {

    public void sendMail(SendMail sendMailData, byte[] attachments)
            throws MessagingException;
}
