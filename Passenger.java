/** Passenger - ENCAPSULATION: fields are private and only changed through validated setters. */
public class Passenger {
    private int passengerId;
    private String name;
    private int age;
    private String phone;

    // CONSTRUCTOR: creates a Passenger object (class -> object)
    public Passenger(int passengerId, String name, int age, String phone) {
        this.passengerId = passengerId;
        setName(name);
        setAge(age);
        setPhone(phone);
    }

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getPhone() { return phone; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Passenger name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setAge(int age) {
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120.");
        }
        this.age = age;
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("[0-9]{6,15}")) {
            throw new IllegalArgumentException("Phone must contain 6 to 15 digits.");
        }
        this.phone = phone.trim();
    }

    @Override
    public String toString() {
        return passengerId + " - " + name + " (age " + age + ", " + phone + ")";
    }
}
