package model.entity;

import java.time.LocalDateTime;

public abstract class Entity {
    protected String id;
    protected LocalDateTime createdAt;

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }
}