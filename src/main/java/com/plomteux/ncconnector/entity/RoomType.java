package com.plomteux.ncconnector.entity;

public enum RoomType {
    INSIDE("inside"),
    OCEAN_VIEW("oceanView"),
    MINI_SUITE("miniSuite"),
    STUDIO("studio"),
    SUITE("suite"),
    HAVEN("haven"),
    SPA("spa");

    private final String fieldName;

    RoomType(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}