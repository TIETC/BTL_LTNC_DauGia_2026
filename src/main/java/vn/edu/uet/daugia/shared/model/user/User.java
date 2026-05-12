package vn.edu.uet.daugia.shared.model.user;
import vn.edu.uet.daugia.shared.model.entity.Entity;

public abstract class User extends Entity {
    protected String username;
    protected String email;
    protected String password;
    protected boolean isActive;

    public User(String username, String email, String password) {
        super();
        this.username = username;
        this.email = email;
        this.password = password;
        this.isActive = true;
    }

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
    public abstract String getRole();

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    @Override
    public String getInfo() {
        return String.format("[%s] %s (%s)", getRole(), username, email);
    }
}