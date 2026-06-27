package com.enterprise.app.entity;

/**
 * Maps to the access_role lookup table.
 * Using an enum avoids a full JPA entity for a simple lookup.
 * The ordinal is stored as-is; values are 1=READ, 2=WRITE, 3=ADMIN.
 */
public enum AccessRole {
    READ,    // id = 1
    WRITE,   // id = 2
    ADMIN    // id = 3
}
