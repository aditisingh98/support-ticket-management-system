package com.support.ticket.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.support.ticket.domain.Priority;

@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateTicketRequest {

    private String title;
    private boolean titlePresent;

    private String description;
    private boolean descriptionPresent;

    private Priority priority;
    private boolean priorityPresent;

    private String assignee;
    private boolean assigneePresent;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonSetter("priority")
    public void setPriority(Priority priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    @JsonSetter("assignee")
    public void setAssignee(String assignee) {
        this.assignee = assignee;
        this.assigneePresent = true;
    }

    public String getTitle() {
        return title;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public String getAssignee() {
        return assignee;
    }

    public boolean isAssigneePresent() {
        return assigneePresent;
    }

    public boolean hasAnyUpdatableField() {
        return titlePresent || descriptionPresent || priorityPresent || assigneePresent;
    }
}
