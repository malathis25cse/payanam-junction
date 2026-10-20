
public class CreateAdmin {

    public static void main(String[] args) {

        try {
            UserAuth.initializeTable();

            boolean created = UserAuth.createAdmin(
                "Administrator",
                "admin@gmail.com",
                "9876543210",
                "Admin@12345"
            );

            if (created) {
                System.out.println("Admin account created successfully!");
            } else {
                System.out.println("Admin account not created. Email may already exist.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
