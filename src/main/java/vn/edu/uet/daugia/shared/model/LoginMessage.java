package vn.edu.uet.daugia.shared.model;

public class LoginMessage {

    private String type;

    private String username;

    private String password;

    public LoginMessage() {
    }

    public LoginMessage(
            String type,
            String username,
            String password
    ) {
        this.type = type;
        this.username = username;
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}