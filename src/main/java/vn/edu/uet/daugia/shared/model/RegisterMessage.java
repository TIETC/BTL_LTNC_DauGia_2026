package vn.edu.uet.daugia.shared.model;

public class RegisterMessage {

    private String type;

    private String username;

    private String password;

    public RegisterMessage(
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