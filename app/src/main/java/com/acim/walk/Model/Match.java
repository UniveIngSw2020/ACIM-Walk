package com.acim.walk.Model;
import com.google.common.collect.ImmutableCollection;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.type.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

@IgnoreExtraProperties
public class Match {

    private String matchId;
    private @ServerTimestamp Date startDate;
    private @ServerTimestamp Date endDate;
    private Boolean isOver;
    private Collection<User> participants;

    public Match() {}

    public Match(String matchId, Date startDate, Date endDate) {
        this.matchId = matchId;
        this.isOver = false;
        this.startDate = startDate;
        this.endDate = endDate;
        this.participants = new ArrayList<>();
    }

    public Match(String matchId, Date startDate, Date endDate, Collection<User> participants){
        this.matchId = matchId;
        this.isOver = false;
        this.startDate = startDate;
        this.endDate = endDate;
        //TODO: check if this.participants = new ArrayList<User>(participants)
        this.participants = participants;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date date) { this.startDate = date; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date date) { this.endDate = date; }

    public Boolean getIsOver() { return isOver; }
    public void setIsOver(Boolean isOver) { this.isOver = isOver; }

    public Collection<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public Boolean addParticipant(User participant){
        if(this.participants == null) {
            this.participants = new ArrayList<>();
        }
        this.participants.add(participant);
        return true;
    }
}