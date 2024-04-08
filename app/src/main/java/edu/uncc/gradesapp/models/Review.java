package edu.uncc.gradesapp.models;

import com.google.firebase.Timestamp;

public class Review {

    String reviewText, createdBy, createdByUid;
    com.google.firebase.Timestamp createdOn;

    public Review() {
    }

    public Review(String reviewText, String createdBy, String createdByUid, com.google.firebase.Timestamp createdOn) {
        this.reviewText = reviewText;
        this.createdBy = createdBy;
        this.createdByUid = createdByUid;
        this.createdOn = createdOn;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByUid() {
        return createdByUid;
    }

    public void setCreatedByUid(String createdByUid) {
        this.createdByUid = createdByUid;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public String toString() {
        return "Review{" +
                "reviewText='" + reviewText + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdByUid='" + createdByUid + '\'' +
                ", createdOn=" + createdOn +
                '}';
    }
}
