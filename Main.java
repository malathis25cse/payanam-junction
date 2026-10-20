import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/** Main - the console (menu) application. It only asks questions and prints; RailwaySystem does the work. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final RailwaySystem system = new RailwaySystem();

    public static void main(String[] args) {
        System.out.println("Connecting to MySQL database railway_reservation ...");
        try {
            DBConnection.testConnection();
        } catch (SQLException e) {
            System.out.println("CANNOT CONNECT TO DATABASE: " + e.getMessage());
            System.out.println("Check: MySQL is running, the SQL script was run, DB_PASSWORD in DBConnection.java is correct,");
            System.out.println("       and the mysql-connector-j jar is in the lib folder.");
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice: ");
            try {
                switch (choice) {
                    case 1:  addTrain(); break;
                    case 2:  viewTrains(); break;
                    case 3:  addPassenger(); break;
                    case 4:  viewPassengers(); break;
                    case 5:  bookTicket(); break;
                    case 6:  cancelTicket(); break;
                    case 7:  viewReservations(); break;
                    case 8:  manageWaitingList(); break;
                    case 9:  addPlatformRequest(); break;
                    case 10: System.out.println(system.allocatePlatforms()); break;
                    case 11: viewPlatformStatus(); break;
                    case 12: recordDelay(); break;
                    case 13: System.out.println(system.confirmArrival(readInt("Train ID that arrived: "))); break;
                    case 14: System.out.println(system.confirmDeparture(readInt("Train ID that departed: "))); break;
                    case 0:  running = false; break;
                    default: System.out.println("Invalid choice. Enter a number from the menu.");
                }
            } catch (SQLException e) {
                System.out.println("DATABASE ERROR: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("========== RAILWAY RESERVATION & PLATFORM ALLOCATION - Payanam Junction ==========");
        System.out.println(" 1. Add train                     8. Manage waiting list");
        System.out.println(" 2. View trains                   9. Add platform request");
        System.out.println(" 3. Add passenger                10. Allocate platforms");
        System.out.println(" 4. View passengers              11. View platform status");
        System.out.println(" 5. Book ticket                  12. Record train delay");
        System.out.println(" 6. Cancel ticket                13. Confirm train arrival");
        System.out.println(" 7. View reservations            14. Confirm train departure");
        System.out.println(" 0. Exit");
    }

    // ---------------- input helpers ----------------
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    // ---------------- menu actions ----------------
    private static void addTrain() throws SQLException {
        String name = readText("Train name: ");
        String origin = readText("Origin: ");
        String destination = readText("Destination: ");
        String arrival = readText("Arrival time (HH:MM): ");
        String departure = readText("Departure time (HH:MM): ");
        int length = readInt("Train length in metres: ");
        System.out.println(system.addTrain(name, origin, destination, arrival, departure, length));
    }

    private static void viewTrains() throws SQLException {
        List<TrainService> list = system.getTrains();
        System.out.println();
        System.out.printf("%-5s %-19s %-34s %-6s %-6s %-5s %-6s %-8s %s%n",
                "ID", "Train", "Route", "Arr", "Dep", "Len", "Seats", "Waiting", "Delay(arr/dep)");
        for (TrainService t : list) {
            System.out.printf("%-5d %-19s %-34s %-6s %-6s %-5d %-6s %-8d +%d/+%d%n",
                    t.getTrainId(), t.getTrainName(), t.getOrigin() + " -> " + t.getDestination(),
                    RailwaySystem.formatTime(t.getEffectiveArrival()), RailwaySystem.formatTime(t.getEffectiveDeparture()),
                    t.getTrainLength(), t.availableSeats() + "/" + TrainService.TOTAL_SEATS, t.waitingCount(),
                    t.getArrivalDelay(), t.getDepartureDelay());
        }
    }

    private static void addPassenger() throws SQLException {
        String name = readText("Passenger name: ");
        int age = readInt("Age: ");
        String phone = readText("Phone (digits only): ");
        System.out.println(system.addPassenger(name, age, phone));
    }

    private static void viewPassengers() throws SQLException {
        System.out.printf("%n%-5s %-20s %-5s %s%n", "ID", "Name", "Age", "Phone");
        for (Passenger p : system.getPassengers()) {
            System.out.printf("%-5d %-20s %-5d %s%n", p.getPassengerId(), p.getName(), p.getAge(), p.getPhone());
        }
    }

    private static void bookTicket() throws SQLException {
        viewTrains();
        int trainId = readInt("Train ID to book: ");
        int passengerId = readInt("Passenger ID (see option 4): ");
        System.out.println(system.bookTicket(trainId, passengerId));
    }

    private static void cancelTicket() throws SQLException {
        int id = readInt("Reservation ID to cancel (see option 7): ");
        System.out.println(system.cancelTicket(id));
    }

    private static void viewReservations() throws SQLException {
        int trainId = readInt("Train ID (0 = all trains): ");
        printReservations(system.getReservations(trainId));
    }

    private static void printReservations(List<Reservation> list) {
        System.out.printf("%n%-7s %-6s %-6s %-20s %-6s %-10s %s%n",
                "ResID", "Train", "PassID", "Passenger", "Seat", "Status", "Booked at");
        for (Reservation r : list) {
            System.out.printf("%-7d %-6d %-6d %-20s %-6s %-10s %s%n", r.getReservationId(), r.getTrainId(),
                    r.getPassengerId(), r.getPassengerName(), r.seatText(), r.getStatus(), r.getBookedAt());
        }
        System.out.println("Total rows: " + list.size());
    }

    private static void manageWaitingList() throws SQLException {
        System.out.println("  1. View waiting list of one train");
        System.out.println("  2. View waiting lists of all trains");
        System.out.println("  3. Cancel a waiting passenger");
        System.out.println("  4. Promote waiting passengers into free seats now");
        int choice = readInt("  Waiting-list choice: ");
        if (choice == 1 || choice == 2) {
            int trainId = (choice == 1) ? readInt("Train ID: ") : 0;
            List<Reservation> list = system.getWaitingList(trainId);
            System.out.printf("%n%-7s %-6s %-20s %s%n", "ResID", "Train", "Passenger", "Queue position");
            for (Reservation r : list) {
                System.out.printf("%-7d %-6d %-20s %d%n", r.getReservationId(), r.getTrainId(),
                        r.getPassengerName(), r.getWaitingPosition());
            }
            if (list.isEmpty()) System.out.println("(waiting list is empty)");
        } else if (choice == 3) {
            System.out.println(system.cancelTicket(readInt("Reservation ID of the waiting passenger: ")));
        } else if (choice == 4) {
            System.out.println(system.promoteWaiting(readInt("Train ID: ")));
        } else {
            System.out.println("Invalid waiting-list choice.");
        }
    }

    private static void addPlatformRequest() throws SQLException {
        viewTrains();
        int trainId = readInt("Train ID that needs a platform: ");
        String type = readText("Type (E = Emergency, S = Scheduled): ");
        System.out.println(system.addPlatformRequest(trainId, type));
    }

    private static void viewPlatformStatus() throws SQLException {
        List<Platform> list = system.getPlatformStatus();
        System.out.println();
        for (Platform p : list) {
            System.out.println(p.getName() + " (" + p.getLengthMeters() + " m): " + p.getStatusText());
            for (PlatformRequest b : p.getBookings()) {
                System.out.println("      " + b.getStatus() + "  Train " + b.getTrainId() + " " + b.getTrain().getTrainName()
                        + "  " + RailwaySystem.formatTime(b.getStart()) + " - " + RailwaySystem.formatTime(b.getEnd()));
            }
        }
        System.out.println(system.getNextReleaseSummary(list));
        System.out.println("\nAll platform requests:");
        System.out.printf("%-6s %-6s %-19s %-10s %-10s %s%n", "ReqID", "Train", "Name", "Type", "Status", "Platform");
        for (PlatformRequest r : system.getPlatformRequests()) {
            System.out.printf("%-6d %-6d %-19s %-10s %-10s %s%n", r.getRequestId(), r.getTrainId(),
                    r.getTrain().getTrainName(), r.getTypeName(), r.getStatus(),
                    r.getPlatformId() == 0 ? "-" : String.valueOf(r.getPlatformId()));
        }
    }

    private static void recordDelay() throws SQLException {
    String trainId = String.valueOf(readInt("Train ID that is delayed: "));
    int minutes = readInt("Delay in minutes: ");

    System.out.println(system.delayTrain(trainId, minutes));
}
}
