import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class RailwayGUI extends JFrame {

    private JPanel content;
private RailwaySystem railwaySystem = new RailwaySystem();
    // ================= CONSTRUCTOR =================

    public RailwayGUI() {

        setTitle("Payanam Junction - Railway Reservation System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(25, 45, 80));

        JLabel title = new JLabel(
            "PAYANAM JUNCTION - RAILWAY RESERVATION"
        );
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title);

        // Sidebar - 9 buttons
        JPanel sidebar = new JPanel(new GridLayout(0, 1, 5, 10));
        sidebar.setBackground(new Color(35, 55, 90));

        String[] menuItems = {
            "Dashboard",
            "Add Train",
            "View Trains",
            "Add Passenger",
            "Book Ticket",
            "View Reservations",
            "Cancel Ticket",
            "Platform Management",
            "Exit"
        };

        content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        showDashboard();

        // Create sidebar buttons
        for (String item : menuItems) {

            JButton button = new JButton(item);
            button.setFont(new Font("Arial", Font.BOLD, 15));
            button.setBackground(new Color(230, 235, 245));
            button.setFocusPainted(false);

            sidebar.add(button);
            

            if (item.equals("Dashboard")) {
                button.addActionListener(e -> showDashboard());
            }

            if (item.equals("Add Train")) {
                button.addActionListener(e -> showAddTrainForm());
            }

            if (item.equals("View Trains")) {
                button.addActionListener(e -> viewTrains());
            }

            if (item.equals("Add Passenger")) {
                button.addActionListener(e -> showAddPassengerForm());
            }

            if (item.equals("Book Ticket")) {
                button.addActionListener(e -> showBookTicketForm());
            }

            if (item.equals("View Reservations")) {
                button.addActionListener(e -> viewReservations());
            }
            // DELAY TRAIN BUTTON

// CONFIRM DEPARTURE BUTTON

            // NEW CANCEL TICKET BUTTON
            if (item.equals("Cancel Ticket")) {
                button.addActionListener(e -> showCancelTicketForm());
            }

            if (item.equals("Platform Management")) {
    button.addActionListener(e -> showPlatformManagement());
}
if (item.equals("Delay Train")) {
    button.addActionListener(e -> {
        showDelayTrain();
    });
}
if (item.equals("Confirm Departure")) {
    button.addActionListener(e -> {
        String trainId = JOptionPane.showInputDialog(
            this,
            "Enter Train ID:"
        );

        if (trainId != null && !trainId.trim().isEmpty()) {
            showConfirmDeparture(trainId.trim());
        }
    });
}

            

            if (item.equals("Exit")) {
                button.addActionListener(e -> System.exit(0));
            }
        }

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(content, BorderLayout.CENTER);

        add(mainPanel);
    }

    // ================= DASHBOARD =================

    private void showDashboard() {

        content.removeAll();

        JLabel welcome = new JLabel(
            "Welcome to Payanam Junction!",
            SwingConstants.CENTER
        );
        welcome.setFont(new Font("Arial", Font.BOLD, 28));
 
        JLabel description = new JLabel(
            "Railway Reservation and Platform Allocation System",
            SwingConstants.CENTER
        );
        description.setFont(new Font("Arial", Font.PLAIN, 18));

        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.setBackground(Color.WHITE);

        centerPanel.add(welcome);
        centerPanel.add(description);

        content.add(centerPanel, BorderLayout.CENTER);

        content.revalidate();
        content.repaint();
    }

    // ================= ADD TRAIN FORM =================

    private void showAddTrainForm() {

        content.removeAll();

        JLabel heading = new JLabel(
            "ADD NEW TRAIN",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 15));
        formPanel.setBorder(
            BorderFactory.createEmptyBorder(30, 80, 30, 80)
        );
        formPanel.setBackground(Color.WHITE);

        JTextField trainName = new JTextField();
        JTextField origin = new JTextField();
        JTextField destination = new JTextField();
        JTextField arrivalTime = new JTextField();
        JTextField departureTime = new JTextField();
        JTextField trainLength = new JTextField();

        JComboBox<String> seatClass = new JComboBox<>(
            new String[]{
                "Sleeper",
                "AC",
                "General",
                "First Class"
            }
        );

        formPanel.add(new JLabel("Train Name:"));
        formPanel.add(trainName);

        formPanel.add(new JLabel("Origin:"));
        formPanel.add(origin);

        formPanel.add(new JLabel("Destination:"));
        formPanel.add(destination);

        formPanel.add(new JLabel("Arrival Time (HH:MM):"));
        formPanel.add(arrivalTime);

        formPanel.add(new JLabel("Departure Time (HH:MM):"));
        formPanel.add(departureTime);

        formPanel.add(new JLabel("Train Length (metres):"));
        formPanel.add(trainLength);

        formPanel.add(new JLabel("Seat Class:"));
        formPanel.add(seatClass);

        JButton saveButton = new JButton("Save Train");
        JButton clearButton = new JButton("Clear");

        formPanel.add(saveButton);
        formPanel.add(clearButton);

        saveButton.addActionListener(e -> {

            String name = trainName.getText().trim();
            String from = origin.getText().trim();
            String to = destination.getText().trim();
            String arrival = arrivalTime.getText().trim();
            String departure = departureTime.getText().trim();
            String length = trainLength.getText().trim();
            String seat = (String) seatClass.getSelectedItem();

            if (name.isEmpty() || from.isEmpty() || to.isEmpty()
                    || arrival.isEmpty() || departure.isEmpty()
                    || length.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please fill all fields!"
                );
                return;
            }

            if (name.length() > 60 || from.length() > 40
                    || to.length() > 40) {

                JOptionPane.showMessageDialog(
                    this,
                    "Train name must be at most 60 characters. "
                    + "Origin and destination must be at most 40 characters."
                );
                return;
            }

            try {

                Time arrivalSql = Time.valueOf(arrival + ":00");
                Time departureSql = Time.valueOf(departure + ":00");

                int lengthValue = Integer.parseInt(length);

                if (lengthValue <= 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Train length must be positive."
                    );
                    return;
                }

                if (!departureSql.after(arrivalSql)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Departure time must be later than arrival time."
                    );
                    return;
                }

                addTrainToDatabase(
                    name,
                    from,
                    to,
                    arrivalSql,
                    departureSql,
                    lengthValue,
                    seat
                );

            } catch (IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    "Invalid input! Enter time in HH:MM format "
                    + "and a valid train length."
                );
            }
        });

        clearButton.addActionListener(e -> {

            trainName.setText("");
            origin.setText("");
            destination.setText("");
            arrivalTime.setText("");
            departureTime.setText("");
            trainLength.setText("");
            seatClass.setSelectedIndex(0);
        });

        content.add(heading, BorderLayout.NORTH);
        content.add(formPanel, BorderLayout.CENTER);

        content.revalidate();
        content.repaint();
    }

    // ================= SAVE TRAIN TO MYSQL =================

    
private void addTrainToDatabase(
        String name,
        String origin,
        String destination,
        Time arrival,
        Time departure,
        int length,
        String seat) {

    String sql = "INSERT INTO trains "
            + "(train_name, origin, destination, arrival_time, "
            + "departure_time, train_length, seat_class, "
            + "arrival_delay_min, departure_delay_min) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)";

    try (
        Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {

        ps.setString(1, name);
        ps.setString(2, origin);
        ps.setString(3, destination);
        ps.setString(4, arrival.toString());
        ps.setString(5, departure.toString());
        ps.setInt(6, length);
        ps.setString(7, seat);

        int result = ps.executeUpdate();

        if (result > 0) {

            JOptionPane.showMessageDialog(
                this,
                "Train added successfully!"
            );

            viewTrains();
        }

    } catch (SQLException e) {

        e.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            "Database Error: " + e.getMessage()
        );
    }
}


    // ================= VIEW TRAINS =================

    
private void viewTrains() {

    String[] columns = {
        "Train ID",
        "Train Name",
        "Origin",
        "Destination",
        "Arrival Time",
        "Departure Time",
        "Train Length",
        "Seat Class"
    };

    DefaultTableModel model = new DefaultTableModel(columns, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    String sql = "SELECT train_id, train_name, origin, destination, "
        + "arrival_time, departure_time, train_length, seat_class "
        + "FROM trains";

    try (
        Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {

        while (rs.next()) {

            Object[] row = {
                rs.getInt("train_id"),
                rs.getString("train_name"),
                rs.getString("origin"),
                rs.getString("destination"),

                // Read SQLite TEXT as String
                rs.getString("arrival_time"),
                rs.getString("departure_time"),

                rs.getInt("train_length"),
                rs.getString("seat_class")
            };

            model.addRow(row);
        }

        JTable table = new JTable(model);

        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel heading = new JLabel(
            "TRAIN DETAILS",
            SwingConstants.CENTER
        );

        heading.setFont(new Font("Arial", Font.BOLD, 24));

        content.removeAll();

        content.add(heading, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        content.revalidate();
        content.repaint();

    } catch (Exception e) {

        e.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            "Unable to load trains: " + e.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}


    // ================= ADD PASSENGER FORM =================

    private void showAddPassengerForm() {

        content.removeAll();

        JLabel heading = new JLabel(
            "PASSENGER REGISTRATION",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 15));

        formPanel.setBorder(
            BorderFactory.createEmptyBorder(50, 100, 50, 100)
        );
        formPanel.setBackground(Color.WHITE);

        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();
        JTextField phoneField = new JTextField();

        JButton saveButton = new JButton("Register Passenger");
        JButton clearButton = new JButton("Clear");

        formPanel.add(new JLabel("Passenger Name:"));
        formPanel.add(nameField);

        formPanel.add(new JLabel("Age:"));
        formPanel.add(ageField);

        formPanel.add(new JLabel("Phone Number:"));
        formPanel.add(phoneField);

        formPanel.add(saveButton);
        formPanel.add(clearButton);

        saveButton.addActionListener(e -> {

            String name = nameField.getText().trim();
            String age = ageField.getText().trim();
            String phone = phoneField.getText().trim();

            if (name.isEmpty() || age.isEmpty() || phone.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please fill all fields!"
                );
                return;
            }

            if (name.length() > 60) {

                JOptionPane.showMessageDialog(
                    this,
                    "Passenger name must not exceed 60 characters."
                );
                return;
            }

            try {

                int passengerAge = Integer.parseInt(age);

                if (passengerAge <= 0 || passengerAge > 120) {

                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid age between 1 and 120."
                    );
                    return;
                }

                if (!phone.matches("[0-9]{10,15}")) {

                    JOptionPane.showMessageDialog(
                        this,
                        "Phone number must contain 10 to 15 digits."
                    );
                    return;
                }

                addPassengerToDatabase(
                    name,
                    passengerAge,
                    phone
                );

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    "Age must be a valid number."
                );
            }
        });

        clearButton.addActionListener(e -> {

            nameField.setText("");
            ageField.setText("");
            phoneField.setText("");
        });

        content.add(heading, BorderLayout.NORTH);
        content.add(formPanel, BorderLayout.CENTER);

        content.revalidate();
        content.repaint();
    }

    // ================= SAVE PASSENGER TO MYSQL =================

    private void addPassengerToDatabase(
            String name,
            int age,
            String phone) {

        String sql = "INSERT INTO passengers "
            + "(name, age, phone) VALUES (?, ?, ?)";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, phone);

            int result = ps.executeUpdate();

            if (result > 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Passenger registered successfully!"
                );

                showDashboard();
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to register passenger: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ================= BOOK TICKET FORM =================

    private void showBookTicketForm() {

        content.removeAll();

        JLabel heading = new JLabel(
            "BOOK TICKET",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 12, 18));

        formPanel.setBorder(
            BorderFactory.createEmptyBorder(70, 100, 70, 100)
        );
        formPanel.setBackground(Color.WHITE);

        JComboBox<TrainOption> trainCombo = new JComboBox<>();
        JComboBox<PassengerOption> passengerCombo = new JComboBox<>();

        JButton bookButton = new JButton("Book Ticket");
        JButton clearButton = new JButton("Clear");

        formPanel.add(new JLabel("Select Train:"));
        formPanel.add(trainCombo);

        formPanel.add(new JLabel("Select Passenger:"));
        formPanel.add(passengerCombo);

        formPanel.add(bookButton);
        formPanel.add(clearButton);

        loadTrainsIntoComboBox(trainCombo);
        loadPassengersIntoComboBox(passengerCombo);

        bookButton.addActionListener(e -> {

            TrainOption train =
                (TrainOption) trainCombo.getSelectedItem();

            PassengerOption passenger =
                (PassengerOption) passengerCombo.getSelectedItem();

            if (train == null) {

                JOptionPane.showMessageDialog(
                    this,
                    "No train available. Please add a train first."
                );
                return;
            }

            if (passenger == null) {

                JOptionPane.showMessageDialog(
                    this,
                    "No passenger available. Please register a passenger first."
                );
                return;
            }

            bookTicketInDatabase(train.id, passenger.id);
        });

        clearButton.addActionListener(e -> {

            if (trainCombo.getItemCount() > 0) {
                trainCombo.setSelectedIndex(0);
            }

            if (passengerCombo.getItemCount() > 0) {
                passengerCombo.setSelectedIndex(0);
            }
        });

        content.add(heading, BorderLayout.NORTH);
        content.add(formPanel, BorderLayout.CENTER);

        content.revalidate();
        content.repaint();
    }

    // ================= LOAD TRAINS INTO COMBOBOX =================

    private void loadTrainsIntoComboBox(
            JComboBox<TrainOption> comboBox) {

        String sql = "SELECT train_id, train_name, origin, destination "
            + "FROM trains";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                comboBox.addItem(
                    new TrainOption(
                        rs.getInt("train_id"),
                        rs.getString("train_name"),
                        rs.getString("origin"),
                        rs.getString("destination")
                    )
                );
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to load trains: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ================= LOAD PASSENGERS INTO COMBOBOX =================

    private void loadPassengersIntoComboBox(
            JComboBox<PassengerOption> comboBox) {

        String sql = "SELECT passenger_id, name, phone FROM passengers";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                comboBox.addItem(
                    new PassengerOption(
                        rs.getInt("passenger_id"),
                        rs.getString("name"),
                        rs.getString("phone")
                    )
                );
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to load passengers: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ================= SAVE BOOKING TO MYSQL =================

    
private void bookTicketInDatabase(int trainId, int passengerId) {

    String checkTrainSql =
        "SELECT train_id FROM trains WHERE train_id = ?";

    String bookedSeatsSql =
        "SELECT seat_no FROM reservations " +
        "WHERE train_id = ? AND status = 'CONFIRMED' " +
        "AND seat_no IS NOT NULL";

    String insertSql =
        "INSERT INTO reservations " +
        "(train_id, passenger_id, seat_no, status, booked_at) " +
        "VALUES (?, ?, ?, ?, datetime('now','localtime'))";

    try (Connection conn = DBConnection.getConnection()) {

        conn.setAutoCommit(false);

        try {
            // Check whether the train exists.
            try (PreparedStatement ps =
                    conn.prepareStatement(checkTrainSql)) {

                ps.setInt(1, trainId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException(
                            "Selected train does not exist."
                        );
                    }
                }
            }

            // Find occupied seats.
            Set<Integer> occupiedSeats = new HashSet<>();

            try (PreparedStatement ps =
                    conn.prepareStatement(bookedSeatsSql)) {

                ps.setInt(1, trainId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupiedSeats.add(rs.getInt("seat_no"));
                    }
                }
            }

            // Allocate the first available seat.
            int availableSeat = -1;

            for (int seat = 1; seat <= 10; seat++) {
                if (!occupiedSeats.contains(seat)) {
                    availableSeat = seat;
                    break;
                }
            }

            String bookingStatus =
                (availableSeat == -1) ? "WAITING" : "CONFIRMED";

            // Save reservation.
            try (PreparedStatement ps =
                    conn.prepareStatement(insertSql)) {

                ps.setInt(1, trainId);
                ps.setInt(2, passengerId);

                if (availableSeat == -1) {
                    ps.setNull(3, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(3, availableSeat);
                }

                ps.setString(4, bookingStatus);

                ps.executeUpdate();
            }

            conn.commit();

            if (availableSeat != -1) {
                JOptionPane.showMessageDialog(
                    this,
                    "Ticket booked successfully!\n" +
                    "Train ID: " + trainId + "\n" +
                    "Passenger ID: " + passengerId + "\n" +
                    "Seat Number: " + availableSeat + "\n" +
                    "Status: CONFIRMED"
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "All 10 seats are booked.\n" +
                    "Passenger added to waiting list.\n" +
                    "Status: WAITING"
                );
            }

        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        }

    } catch (Exception ex) {
        ex.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            "Unable to book ticket: " + ex.getMessage(),
            "Booking Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
    // ================= VIEW RESERVATIONS =================

    private void viewReservations() {

        String[] columns = {
            "Reservation ID",
            "Train ID",
            "Train Name",
            "Passenger ID",
            "Passenger Name",
            "Seat Number",
            "Status",
            "Booked At"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql =
            "SELECT r.reservation_id, r.train_id, t.train_name, "
            + "r.passenger_id, p.name AS passenger_name, "
            + "r.seat_no, r.status, r.booked_at "
            + "FROM reservations r "
            + "JOIN trains t ON r.train_id = t.train_id "
            + "JOIN passengers p ON r.passenger_id = p.passenger_id "
            + "ORDER BY r.reservation_id DESC";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                Object seatValue = rs.getObject("seat_no");

                Object[] row = {
                    rs.getInt("reservation_id"),
                    rs.getInt("train_id"),
                    rs.getString("train_name"),
                    rs.getInt("passenger_id"),
                    rs.getString("passenger_name"),
                    seatValue == null ? "N/A" : seatValue,
                    rs.getString("status"),
                    rs.getTimestamp("booked_at")
                };

                model.addRow(row);
            }

            JTable table = new JTable(model);
            table.setRowHeight(30);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            JLabel heading = new JLabel(
                "RESERVATION DETAILS",
                SwingConstants.CENTER
            );
            heading.setFont(new Font("Arial", Font.BOLD, 24));

            content.removeAll();

            content.add(heading, BorderLayout.NORTH);
            content.add(new JScrollPane(table), BorderLayout.CENTER);

            content.revalidate();
            content.repaint();

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Unable to load reservations: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ================= CANCEL TICKET FORM =================

    private void showCancelTicketForm() {

        content.removeAll();

        JLabel heading = new JLabel(
            "CANCEL TICKET",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 20));

        formPanel.setBorder(
            BorderFactory.createEmptyBorder(100, 100, 100, 100)
        );
        formPanel.setBackground(Color.WHITE);

        JTextField reservationIdField = new JTextField();

        JButton cancelButton = new JButton("Cancel Ticket");
        JButton clearButton = new JButton("Clear");

        formPanel.add(new JLabel("Enter Reservation ID:"));
        formPanel.add(reservationIdField);

        formPanel.add(cancelButton);
        formPanel.add(clearButton);

        cancelButton.addActionListener(e -> {

            String idText = reservationIdField.getText().trim();

            if (idText.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please enter Reservation ID."
                );
                return;
            }

            try {

                int reservationId = Integer.parseInt(idText);

                if (reservationId <= 0) {

                    JOptionPane.showMessageDialog(
                        this,
                        "Reservation ID must be positive."
                    );
                    return;
                }

                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to cancel Reservation ID "
                    + reservationId + "?",
                    "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    cancelTicketInDatabase(reservationId);
                }

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid Reservation ID."
                );
            }
        });

        clearButton.addActionListener(e ->
            reservationIdField.setText("")
        );

        content.add(heading, BorderLayout.NORTH);
        content.add(formPanel, BorderLayout.CENTER);

        content.revalidate();
        content.repaint();
    }

    // ================= CANCEL TICKET IN MYSQL =================

    private void cancelTicketInDatabase(int reservationId) {

        Connection conn = null;

        try {

            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int trainId;
            int seatNo;
            String status;

            /*
             * First find which train this reservation belongs to.
             */
            String findReservationSql =
                "SELECT train_id FROM reservations "
                + "WHERE reservation_id = ?";

            try (PreparedStatement ps =
                    conn.prepareStatement(findReservationSql)) {

                ps.setInt(1, reservationId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new Exception(
                            "Reservation ID not found."
                        );
                    }

                    trainId = rs.getInt("train_id");
                }
            }

            /*
             * Lock the train row.
             * Booking and cancellation for this train are serialized.
             */
            String lockTrainSql =
    "SELECT train_id FROM trains "
    + "WHERE train_id = ?";

            try (PreparedStatement ps =
                    conn.prepareStatement(lockTrainSql)) {

                ps.setInt(1, trainId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new Exception("Train not found.");
                    }
                }
            }

            /*
             * Read the reservation again after locking the train.
             */
            String checkSql =
    "SELECT seat_no, status FROM reservations "
    + "WHERE reservation_id = ?";

            try (PreparedStatement ps =
                    conn.prepareStatement(checkSql)) {

                ps.setInt(1, reservationId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new Exception(
                            "Reservation not found."
                        );
                    }

                    Object seatValue = rs.getObject("seat_no");

                    if (seatValue == null) {
                        seatNo = -1;
                    } else {
                        seatNo = rs.getInt("seat_no");
                    }

                    status = rs.getString("status");
                }
            }

            if ("CANCELLED".equals(status)) {

                throw new Exception(
                    "This ticket has already been cancelled."
                );
            }

            /*
             * Release the reservation.
             *
             * Setting seat_no to NULL is important because the
             * database has a UNIQUE constraint on (train_id, seat_no).
             */
            String cancelSql =
                "UPDATE reservations "
                + "SET status = 'CANCELLED', seat_no = NULL "
                + "WHERE reservation_id = ?";

            try (PreparedStatement ps =
                    conn.prepareStatement(cancelSql)) {

                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }

            int promotedReservationId = -1;

            /*
             * If the cancelled ticket was CONFIRMED,
             * give its seat to the first waiting passenger.
             */
            if ("CONFIRMED".equals(status) && seatNo != -1) {

                String waitingSql =
    "SELECT reservation_id FROM reservations "
    + "WHERE train_id = ? AND status = 'WAITING' "
    + "ORDER BY booked_at ASC, reservation_id ASC "
    + "LIMIT 1";

                try (PreparedStatement ps =
                        conn.prepareStatement(waitingSql)) {

                    ps.setInt(1, trainId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            promotedReservationId =
                                rs.getInt("reservation_id");
                        }
                    }
                }

                /*
                 * Assign the released seat to the waiting passenger.
                 */
                if (promotedReservationId != -1) {

                    String promoteSql =
                        "UPDATE reservations "
                        + "SET seat_no = ?, status = 'CONFIRMED' "
                        + "WHERE reservation_id = ? "
                        + "AND status = 'WAITING'";

                    try (PreparedStatement ps =
                            conn.prepareStatement(promoteSql)) {

                        ps.setInt(1, seatNo);
                        ps.setInt(2, promotedReservationId);

                        int updated = ps.executeUpdate();

                        if (updated != 1) {
                            throw new Exception(
                                "Unable to promote waiting passenger."
                            );
                        }
                    }
                }
            }

            conn.commit();

            String message =
                "Ticket cancelled successfully!\n"
                + "Reservation ID: " + reservationId;

            if (promotedReservationId != -1) {

                message +=
                    "\n\nWaiting passenger promoted successfully!"
                    + "\nNew Reservation ID: "
                    + promotedReservationId
                    + "\nAssigned Seat Number: " + seatNo;

            } else if ("CONFIRMED".equals(status) && seatNo != -1) {

                message +=
                    "\n\nThe seat is now available.";
            }

            JOptionPane.showMessageDialog(
                this,
                message
            );

            viewReservations();

        } catch (Exception ex) {

            if (conn != null) {

                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Unable to cancel ticket: " + ex.getMessage(),
                "Cancellation Error",
                JOptionPane.ERROR_MESSAGE
            );

        } finally {

            if (conn != null) {

                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception closeEx) {
                    // Connection cleanup.
                }
            }
        }
    }

    // ================= TRAIN DROPDOWN DATA CLASS =================

    private static class TrainOption {

        int id;
        String name;
        String origin;
        String destination;

        TrainOption(
                int id,
                String name,
                String origin,
                String destination) {

            this.id = id;
            this.name = name;
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        public String toString() {

            return id + " - " + name
                + " (" + origin + " to " + destination + ")";
        }
    }

    // ================= PASSENGER DROPDOWN DATA CLASS =================

    private static class PassengerOption {

        int id;
        String name;
        String phone;

        PassengerOption(int id, String name, String phone) {

            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        @Override
        public String toString() {

            return id + " - " + name + " (" + phone + ")";
        }
    }
    // ================= PLATFORM MANAGEMENT =================

private void showPlatformManagement() {

    content.removeAll();

    JLabel heading = new JLabel(
        "PLATFORM REQUEST MANAGEMENT",
        SwingConstants.CENTER
    );

    heading.setFont(new Font("Arial", Font.BOLD, 24));

    JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 15));

    formPanel.setBorder(
        BorderFactory.createEmptyBorder(30, 50, 30, 50)
    );

    JTextField trainIdField = new JTextField();

    JComboBox<String> requestType = new JComboBox<>(
        new String[]{"SCHEDULED", "EMERGENCY"}
    );

    JButton addRequestButton = new JButton("Add Platform Request");

    JButton allocateButton = new JButton("Allocate Platforms");

    JButton refreshButton = new JButton("Refresh Status");

    JTextArea output = new JTextArea(15, 40);

    output.setEditable(false);

    output.setFont(new Font("Monospaced", Font.PLAIN, 13));

    JScrollPane scrollPane = new JScrollPane(output);

    formPanel.add(new JLabel("Train ID:"));
    formPanel.add(trainIdField);

    formPanel.add(new JLabel("Request Type:"));
    formPanel.add(requestType);

    formPanel.add(addRequestButton);
    formPanel.add(allocateButton);

    formPanel.add(refreshButton);

    // ADD PLATFORM REQUEST

    addRequestButton.addActionListener(e -> {

        try {

            int trainId = Integer.parseInt(
                trainIdField.getText().trim()
            );

            String type = requestType.getSelectedItem().toString();

            String result = railwaySystem.addPlatformRequest(
                trainId,
                type
            );

            JOptionPane.showMessageDialog(this, result);

            trainIdField.setText("");

            refreshPlatformStatus(output);

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Please enter a valid Train ID."
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Error: " + ex.getMessage()
            );
        }
    });

    // ALLOCATE PLATFORMS

    allocateButton.addActionListener(e -> {

        try {

            String result = railwaySystem.allocatePlatforms();

            JOptionPane.showMessageDialog(this, result);

            refreshPlatformStatus(output);

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Allocation Error: " + ex.getMessage()
            );
        }
    });

    // REFRESH STATUS

    refreshButton.addActionListener(e -> {

        refreshPlatformStatus(output);

    });

    content.add(heading, BorderLayout.NORTH);
    content.add(formPanel, BorderLayout.CENTER);
    content.add(scrollPane, BorderLayout.SOUTH);

    content.revalidate();
    content.repaint();

}

// ================= DELAY TRAIN =================

private void showDelayTrain() {

    content.removeAll();

    JLabel heading = new JLabel(
        "DELAY TRAIN MANAGEMENT",
        SwingConstants.CENTER
    );

    heading.setFont(new Font("Arial", Font.BOLD, 24));

    JPanel panel = new JPanel(new GridLayout(3, 2, 10, 15));

    panel.setBorder(
        BorderFactory.createEmptyBorder(40, 50, 40, 50)
    );

    JTextField trainIdField = new JTextField();
    JTextField delayField = new JTextField();

    JButton delayButton = new JButton("Delay Train");

    panel.add(new JLabel("Train ID:"));
    panel.add(trainIdField);

    panel.add(new JLabel("Delay (Minutes):"));
    panel.add(delayField);

    panel.add(new JLabel(""));
    panel.add(delayButton);

    delayButton.addActionListener(e -> {

        try {

            String trainId = trainIdField.getText().trim();

            if (trainId.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this, "Please enter Train ID."
                );
                return;
            }

            int delayMinutes = Integer.parseInt(
                delayField.getText().trim()
            );

            if (delayMinutes <= 0) {
                JOptionPane.showMessageDialog(
                    this, "Delay must be greater than zero."
                );
                return;
            }

            String result = railwaySystem.delayTrain(
                trainId, delayMinutes
            );

            JOptionPane.showMessageDialog(this, result);

            trainIdField.setText("");
            delayField.setText("");

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                this, "Enter a valid delay in minutes."
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this, "Error: " + ex.getMessage()
            );
        }
    });

    content.add(heading, BorderLayout.NORTH);
    content.add(panel, BorderLayout.CENTER);

    content.revalidate();
    content.repaint();
    content.getParent().revalidate();
content.getParent().repaint();
}


public String showConfirmDeparture(String trainId) {

    try {

        String sql = "UPDATE trains " +
                     "SET departure_status = 'DEPARTED' " +
                     "WHERE train_id = ? " +
                     "AND departure_status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, trainId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                return "Departure confirmed successfully for " + trainId;
            }

            return "Train not found or departure already confirmed.";
        }

    } catch (Exception e) {
        return "Departure Error: " + e.getMessage();
    }
}

// ================= REFRESH PLATFORM STATUS =================

private void refreshPlatformStatus(JTextArea output) {

    try {

        StringBuilder sb = new StringBuilder();

        sb.append("========== PLATFORM STATUS ==========\n\n");

        List<Platform> platforms =
            railwaySystem.getPlatformStatus();

        for (Platform p : platforms) {

            sb.append("Platform: ")
              .append(p.getName())
              .append("\n");

            sb.append("Length: ")
              .append(p.getLengthMeters())
              .append(" metres\n\n");
        }

        sb.append("========== PLATFORM REQUESTS ==========\n\n");

        List<PlatformRequest> requests =
            railwaySystem.getPlatformRequests();

        for (PlatformRequest r : requests) {

            sb.append("Request ID: ")
              .append(r.getRequestId())
              .append("\n");

            sb.append("Train ID: ")
              .append(r.getTrainId())
              .append("\n");

            sb.append("Type: ")
              .append(r.getTypeName())
              .append("\n");

            sb.append("Status: ")
              .append(r.getStatus())
              .append("\n");

            sb.append("Platform ID: ")
              .append(r.getPlatformId())
              .append("\n");

            sb.append("--------------------------------------\n");
        }

        output.setText(sb.toString());

    } catch (Exception ex) {

        JOptionPane.showMessageDialog(
            this,
            "Unable to load platform status: " + ex.getMessage()
        );
    }
}

    // ================= MAIN METHOD =================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            RailwayGUI gui = new RailwayGUI();
            gui.setVisible(true);
        });
    }
}