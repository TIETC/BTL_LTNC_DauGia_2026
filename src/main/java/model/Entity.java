package model;

import java.util.Date;
import java.util.UUID; // Thư viện dùng để tạo chuỗi ID ngẫu nhiên không bao giờ trùng lặp
public abstract class Entity {
    protected String id;
    protected Date createdAt;
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }
    public String getId() {
        return id;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
}