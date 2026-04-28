package model;
public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String email;

    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
}