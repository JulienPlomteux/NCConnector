package com.plomteux.ncconnector.entity;

public enum RoomType {
    INSIDE("inside"),
    OCEANVIEW("oceanView"),
    BALCONY("balcony");


    private final String fieldName;

    RoomType(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}