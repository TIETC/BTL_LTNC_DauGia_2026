package vn.edu.uet.daugia.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity {
    protected String id;
    protected LocalDateTime createdAt;

    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public abstract String getInfo();
}