
public class TestDB {

    public static void main(String[] args) {

        try {

            DBConnection.testConnection();

        } catch (Exception e) {

            System.out.println(
                "Database Error: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}