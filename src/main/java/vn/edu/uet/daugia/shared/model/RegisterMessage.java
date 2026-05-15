package vn.edu.uet.daugia.shared.model;

public class RegisterMessage {

    private String type;
    private String username;
    private String password;
    private String role; // THÊM MỚI

    public RegisterMessage(String type, String username, String password, String role) {
        this.type = type;
        this.username = username;
        this.password = password;
        this.role = role; // THÊM MỚI
    }

    public String getType() { return type; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; } // THÊM MỚI
}