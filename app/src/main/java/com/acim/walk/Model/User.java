package com.acim.walk.Model;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import javax.annotation.Nullable;

@IgnoreExtraProperties
public class User {

    private String email;
    private String userId;
    private String username;
    private Integer steps;
    @Nullable
    private String matchId;

    public User() { steps = 0; }

    public User(String email, String userId, String username, Integer steps){
        this.email = email;
        this.userId = userId;
        this.username = username;
        this.steps = steps;
    }

    public User(String email, String userId, String username){
        this.email = email;
        this.userId = userId;
        this.username = username;
        this.steps = 0;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public String getMatchId() { return matchId; }

    public void setMatchId(String matchId) { this.matchId = matchId; }

}
