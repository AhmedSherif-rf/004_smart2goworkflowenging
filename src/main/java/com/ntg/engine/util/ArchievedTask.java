package com.ntg.engine.util;

import com.fasterxml.jackson.annotation.JsonView;
import com.ntg.engine.entites.ToDoList;

import java.util.List;

public class ArchievedTask {

    @JsonView(View.Summary.class)
    private ToDoList task;
    @JsonView(View.Summary.class)
    private List udas;

    public ToDoList getTask() {
        return task;
    }

    public void setTask(ToDoList task) {
        this.task = task;
    }

    public List getUdas() {
        return udas;
    }

    public void setUdas(List udas) {
        this.udas = udas;
    }
}
