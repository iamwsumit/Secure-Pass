package com.sumit.passwordmanager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final long createdTime;
    private final long updateTime;
    private final String title;
    private final String comment;
    private final String content;
    private final String username;

    public Model(long createdTime, long updateTime, String title, String content, String comment, String username) {
        this.createdTime = createdTime;
        this.updateTime = updateTime;
        this.content = content;
        this.comment = comment;
        this.title = title;
        this.username = username;
    }

    @NonNull
    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        list.add(title);
        if (!username.isEmpty())
            list.add("Username : " + username);
        list.add("Password : " + content);
        if (!comment.isEmpty())
            list.add("Note : " + comment);
        return String.join("\n", list);
    }

    public String getUsername() {
        return username;
    }

    public String getComment() {
        return comment;
    }

    public String getTitle() {
        return title;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public String getContent() {
        return content;
    }
}