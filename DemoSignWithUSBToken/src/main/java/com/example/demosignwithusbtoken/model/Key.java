package com.example.demosignwithusbtoken.model;

import com.sun.istack.internal.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Key {
    private String alias;
    private String serialNumber;
    private String information;
    @Nullable
    private String extension;

    public Key(String alias, String serialNumber, String information) {
        this.alias = alias;
        this.serialNumber = serialNumber;
        this.information = information;
    }
}
