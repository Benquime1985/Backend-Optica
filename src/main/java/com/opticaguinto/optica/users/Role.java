package com.opticaguinto.optica.users;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;
import java.util.Locale;

public enum Role {
    ADMIN,
    RECEPCION,
    OPTOMETRISTA;

    @JsonCreator
    public static Role from(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        for (Role role : values()) {
            if (role.name().equals(normalized)) return role;
        }
        throw new IllegalArgumentException("Rol inválido: " + value);
    }
}
