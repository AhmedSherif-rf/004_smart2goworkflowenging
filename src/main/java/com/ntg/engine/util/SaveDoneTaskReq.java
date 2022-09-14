package com.ntg.engine.util;

import com.ntg.engine.entites.ToDoList;

public class SaveDoneTaskReq {

    private ToDoList DoneTask ;
    //private Notifications notification;
    private ArchievedTask ArchTask;
    private String route_if;
    private String date ;

    public ToDoList getDoneTask() {
        return DoneTask;
    }

    public void setDoneTask(ToDoList doneTask) {
        DoneTask = doneTask;
    }

    public ArchievedTask getArchTask() {
        return ArchTask;
    }

    public void setArchTask(ArchievedTask archTask) {
        ArchTask = archTask;
    }

    public String getRoute_if() {
        return route_if;
    }

    public void setRoute_if(String route_if) {
        this.route_if = route_if;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
