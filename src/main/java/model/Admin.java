package model;

public class Admin extends User {
    public Admin(String username, String password, String email) {
        super(username, password, email);
    }
    public void banUser(User user) {
        System.out.println("Admin " + this.username + " đã khóa tài khoản của: " + user.getUsername());
    }
}