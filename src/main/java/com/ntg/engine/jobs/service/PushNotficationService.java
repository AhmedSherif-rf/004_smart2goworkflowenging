package com.ntg.engine.jobs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@Service
public class PushNotficationService {

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wf_pushNotification}")
    String wf_pushNotificationURL;


    public void sendTaskPushNotfication(String assignDesc, long to_EmployeeID, Long taskId, String companyName) {
        try {
            HttpHeaders headersRoute = new HttpHeaders();
            headersRoute.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headersRoute.set("SessionToken", commonCachingFun.BackEndLogin(companyName));
            headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");
            Map<String, String> mapToBeSent = new Hashtable<String, String>();
            mapToBeSent.put("message", assignDesc);
            mapToBeSent.put("toUserId", String.valueOf(to_EmployeeID));
            mapToBeSent.put("taskId", String.valueOf(taskId));
            mapToBeSent.put("taskTableName", "sa_todo_list");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(mapToBeSent, headersRoute);
            restTemplate.exchange(wf_pushNotificationURL, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            System.out.println("War : sendTaskPushNotfication Error " + e.getMessage());
            //NTGMessageOperation.PrintErrorTrace(e);
        }
    }


    //@AddedBy:Aya.Ramadan => Dev-00002285:Push notification to group members
    public void sendTaskPushNotficationToGroup(String assignDesc, List<Long> to_EmployeeIDs, Long taskId, String companyName) {
        try {
            HttpHeaders headersRoute = new HttpHeaders();
            headersRoute.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headersRoute.set("SessionToken", commonCachingFun.BackEndLogin(companyName));
            headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");
            Map<String, String> mapToBeSent = new Hashtable<String, String>();
            mapToBeSent.put("message", assignDesc);
            mapToBeSent.put("taskId", String.valueOf(taskId));
            mapToBeSent.put("taskTableName", "sa_todo_list");
            for (Long empId : to_EmployeeIDs) {
                mapToBeSent.put("toUserId", String.valueOf(empId));
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(mapToBeSent, headersRoute);
                restTemplate.exchange(wf_pushNotificationURL, HttpMethod.POST, entity, Map.class);
            }
        } catch (Exception e) {
            System.out.println("War : sendTaskPushNotfication Error " + e.getMessage());
            //NTGMessageOperation.PrintErrorTrace(e);
        }
    }
}
