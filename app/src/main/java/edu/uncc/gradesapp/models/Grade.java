package edu.uncc.gradesapp.models;

import java.util.HashMap;
import java.util.Objects;

public class Grade {

    String createdByName, createdByUid, courseName, courseNumber, letterGrade, semesterNameYear, docId;
    Double creditHours, numericGrade;

    public Grade() {}

    public Grade(HashMap<String, Object> gradesHashMap) {

        this.createdByName = (String) gradesHashMap.get("CreatedByName");
        this.createdByUid = (String) gradesHashMap.get("CreatedByUid");
        this.docId = (String) gradesHashMap.get("DocumentId");

        HashMap<String, Object> courseHashMap = (HashMap<String, Object>) gradesHashMap.get("CourseInfo");
        HashMap<String, Object> semesterHashMap = (HashMap<String, Object>) gradesHashMap.get("SemesterInfo");
        HashMap<String, Object> letterGradeHashMap = (HashMap<String, Object>) gradesHashMap.get("LetterGradeInfo");

        this.semesterNameYear = (String) semesterHashMap.get("name");

        this.courseName = (String) courseHashMap.get("name");
        this.courseNumber = (String) courseHashMap.get("number");
        this.creditHours = (Double) courseHashMap.get("hours");

        this.letterGrade = (String) letterGradeHashMap.get("letterGrade");
        this.numericGrade = (Double) letterGradeHashMap.get("numericGrade");
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByUid() {
        return createdByUid;
    }

    public void setCreatedByUid(String createdByUid) {
        this.createdByUid = createdByUid;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) {
        this.courseNumber = courseNumber;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public String getSemesterNameYear() {
        return semesterNameYear;
    }

    public void setSemesterNameYear(String semesterNameYear) {
        this.semesterNameYear = semesterNameYear;
    }

    public Double getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(Double creditHours) {
        this.creditHours = creditHours;
    }

    public Double getNumericGrade() {
        return numericGrade;
    }

    public void setNumericGrade(Double numericGrade) {
        this.numericGrade = numericGrade;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    @Override
    public String toString() {
        return "Grade{" +
                "createdByName='" + createdByName + '\'' +
                ", createdByUid='" + createdByUid + '\'' +
                ", courseName='" + courseName + '\'' +
                ", courseNumber='" + courseNumber + '\'' +
                ", letterGrade='" + letterGrade + '\'' +
                ", semesterNameYear='" + semesterNameYear + '\'' +
                ", docId='" + docId + '\'' +
                ", creditHours=" + creditHours +
                ", numericGrade=" + numericGrade +
                '}';
    }

    /*
    HashMap<String, Object> semester;
    HashMap<String, Object> course;
    HashMap<String, Object> letterGrade;

    this.semester = (HashMap<String, Object>) gradesHashMap.get("SemesterInfo");
    */
}
