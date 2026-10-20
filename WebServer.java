import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
    // Stores currently logged-in users
    

/**
 * WebServer - the browser application.
 * Technology: the web server that is BUILT INTO Java (com.sun.net.httpserver.HttpServer)
 * + plain HTML forms + a little CSS. No JavaScript, no framework, nothing to install.
 * It calls the same RailwaySystem methods as the console, so it uses the same MySQL database.
 */
public class WebServer {
    private static final int PORT = 8081;
    private static final RailwaySystem system = new RailwaySystem();
    private static final Map<String, UserAuth.AuthUser> sessions =
            new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        try {
    DBConnection.testConnection();

    UserAuth.initializeTable();

    System.out.println("User authentication table initialized!");

} catch (SQLException e) {
    System.out.println("CANNOT CONNECT TO DATABASE: " + e.getMessage());
    return;
}
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", WebServer::handle);
        server.start();
        System.out.println("Web server started. Open this address in your browser:  http://localhost:" + PORT);
        System.out.println("Press Ctrl+C in this window to stop the server.");
    }

    // ------------------------------------------------------------------ routing
    private static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        System.out.println("REQUEST PATH = " + path);
        boolean post = ex.getRequestMethod().equalsIgnoreCase("POST");
        try {
            Map<String, String> form = post
                    ? parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                    : parseForm(ex.getRequestURI().getRawQuery());
            String html;
            switch (path) {
                case "/":             html = homePage(); break;
                case "/trains":       html = trainsPage(); break;
                case "/passengers":   html = passengersPage(form, post); break;
                case "/book":         html = bookPage(form, post); break;
                case "/cancel":       html = cancelPage(form, post); break;
                case "/reservations": html = reservationsPage(form); break;
                case "/waiting":      html = waitingPage(); break;
                case "/platforms":html = platformsPage(form, post);break;
                case "/register": html = registerPage(form, post);break;
               case "/login": html = loginPage(form, post, ex); break;
               case "/dashboard": html = userDashboard(ex); break;
               case "/passenger-register":html = passengerRegistrationPage(form, post, ex);break;
                case "/logout": html = logoutPage(ex); break;
                case "/admin": html = adminLoginPage(form, post, ex); break;
                case "/admin-dashboard":html = adminDashboard(ex); break;
                case "/add-train":html = addTrainPage(form, post, ex);break;
                case "/delay-train":html = delayTrainPage(form, post, ex);break;
                case "/confirm-departure":html = confirmDeparturePage(form, post, ex);break;
                case "/departure-delay":html = departureDelayPage(form, post, ex);break;
                default:
                    send(ex, 404, page("Not found", "<p class='err'>Page not found.</p>"));
                    return;
            }
            send(ex, 200, html);
        } catch (SQLException e) {
            send(ex, 500, page("Database error", "<p class='err'>Database error: " + esc(e.getMessage()) + "</p>"));
        } catch (Exception e) {
            send(ex, 500, page("Error", "<p class='err'>Error: " + esc(String.valueOf(e.getMessage())) + "</p>"));
        }
    }

    // ------------------------------------------------------------------ pages
    
private static String homePage() {
    return page("Home",
        "<div class='welcome'>"
        + "<h1>Welcome to Payanam Junction</h1>"
        + "<p>Your Railway Reservation System</p>"

        + "<div class='login-container'>"

        + "<div class='login-card'>"
        + "<h2>User Login</h2>"
        + "<p>Book train tickets and manage your reservations.</p>"
        + "<a class='login-btn' href='/login'>User Login</a>"
        + "<p>New user? <a href='/register'>Register here</a></p>"
        + "</div>"

        + "<div class='login-card'>"
        + "<h2>Admin Login</h2>"
        + "<p>Manage trains, passengers, reservations and platforms.</p>"
        + "<a class='login-btn admin-btn' href='/admin'>Admin Login</a>"
        + "</div>"

        + "</div>"
        + "</div>");
}


    private static String trainsPage() throws SQLException {
        StringBuilder sb = new StringBuilder("<table><tr><th>ID</th><th>Train</th><th>Route</th><th>Arrival</th>"
                + "<th>Departure</th><th>Length (m)</th><th>Free seats</th><th>Waiting</th><th>Delay (arr/dep)</th></tr>");
        for (TrainService t : system.getTrains()) {
            sb.append("<tr><td>").append(t.getTrainId()).append("</td><td>").append(esc(t.getTrainName()))
              .append("</td><td>").append(esc(t.getOrigin())).append(" &rarr; ").append(esc(t.getDestination()))
              .append("</td><td>").append(RailwaySystem.formatTime(t.getEffectiveArrival()))
              .append("</td><td>").append(RailwaySystem.formatTime(t.getEffectiveDeparture()))
              .append("</td><td>").append(t.getTrainLength())
              .append("</td><td>").append(t.availableSeats()).append(" / ").append(TrainService.TOTAL_SEATS)
              .append("</td><td>").append(t.waitingCount())
              .append("</td><td>+").append(t.getArrivalDelay()).append(" / +").append(t.getDepartureDelay())
              .append("</td></tr>");
        }
        return page("Trains", sb.append("</table>").toString());
    }

    private static String passengersPage(Map<String, String> f, boolean post) throws SQLException {
        String msg = "";
        if (post) {
            try {
                msg = system.addPassenger(val(f, "name"), Integer.parseInt(val(f, "age")), val(f, "phone"));
            } catch (NumberFormatException e) {
                msg = "ERROR: Age must be a number.";
            }
        }
        StringBuilder sb = new StringBuilder(message(msg));
        sb.append("<h3>Register a passenger</h3><form method='post' action='/passengers'>")
          .append("<label>Name</label><input name='name' required>")
          .append("<label>Age</label><input name='age' type='number' min='1' max='120' required>")
          .append("<label>Phone (digits)</label><input name='phone' required>")
          .append("<button>Register</button></form><h3>Registered passengers</h3>")
          .append("<table><tr><th>ID</th><th>Name</th><th>Age</th><th>Phone</th></tr>");
        for (Passenger p : system.getPassengers()) {
            sb.append("<tr><td>").append(p.getPassengerId()).append("</td><td>").append(esc(p.getName()))
              .append("</td><td>").append(p.getAge()).append("</td><td>").append(esc(p.getPhone())).append("</td></tr>");
        }
        return page("Passenger registration", sb.append("</table>").toString());
    }

    private static String bookPage(Map<String, String> f, boolean post) throws SQLException {
        String msg = "";
        if (post) {
            try {
                msg = system.bookTicket(Integer.parseInt(val(f, "train_id")), Integer.parseInt(val(f, "passenger_id")));
            } catch (NumberFormatException e) {
                msg = "ERROR: Please choose a train and a passenger.";
            }
        }
        StringBuilder sb = new StringBuilder(message(msg));
        sb.append("<form method='post' action='/book'><label>Train</label><select name='train_id'>");
        for (TrainService t : system.getTrains()) {
            sb.append("<option value='").append(t.getTrainId()).append("'>").append(t.getTrainId()).append(" - ")
              .append(esc(t.getTrainName())).append(" (").append(t.availableSeats()).append(" free, ")
              .append(t.waitingCount()).append(" waiting)</option>");
        }
        sb.append("</select><label>Passenger</label><select name='passenger_id'>");
        for (Passenger p : system.getPassengers()) {
            sb.append("<option value='").append(p.getPassengerId()).append("'>").append(p.getPassengerId())
              .append(" - ").append(esc(p.getName())).append("</option>");
        }
        sb.append("</select><button>Book ticket</button></form>")
          .append("<p>If the train is full, the passenger joins the FIFO waiting list automatically.</p>");
        return page("Book a ticket", sb.toString());
    }

    private static String cancelPage(Map<String, String> f, boolean post) throws SQLException {
        String msg = "";
        if (post) {
            try {
                msg = system.cancelTicket(Integer.parseInt(val(f, "reservation_id")));
            } catch (NumberFormatException e) {
                msg = "ERROR: Invalid reservation id.";
            }
        }
        StringBuilder sb = new StringBuilder(message(msg));
        sb.append("<p>Active reservations (click Cancel to cancel one):</p>")
          .append("<table><tr><th>ResID</th><th>Train</th><th>Passenger</th><th>Seat</th><th>Status</th><th></th></tr>");
        for (Reservation r : system.getReservations(0)) {
            if (!r.isActive()) continue;
            sb.append("<tr><td>").append(r.getReservationId()).append("</td><td>").append(r.getTrainId())
              .append("</td><td>").append(esc(r.getPassengerName())).append("</td><td>").append(r.seatText())
              .append("</td><td>").append(r.getStatus()).append("</td><td>")
              .append("<form method='post' action='/cancel' style='margin:0'><input type='hidden' name='reservation_id' value='")
              .append(r.getReservationId()).append("'><button class='small'>Cancel</button></form></td></tr>");
        }
        return page("Cancel a ticket", sb.append("</table>").toString());
    }

    private static String reservationsPage(Map<String, String> f) throws SQLException {
        int trainId = 0;
        try { trainId = Integer.parseInt(val(f, "train_id")); } catch (NumberFormatException e) { /* show all */ }
        StringBuilder sb = new StringBuilder("<form method='get' action='/reservations'><label>Show train</label>"
                + "<select name='train_id'><option value='0'>All trains</option>");
        for (TrainService t : system.getTrains()) {
            sb.append("<option value='").append(t.getTrainId()).append("'")
              .append(t.getTrainId() == trainId ? " selected" : "").append(">").append(t.getTrainId()).append(" - ")
              .append(esc(t.getTrainName())).append("</option>");
        }
        sb.append("</select><button>Show</button></form>")
          .append("<table><tr><th>ResID</th><th>Train</th><th>Passenger</th><th>Seat</th><th>Status</th><th>Booked at</th></tr>");
        for (Reservation r : system.getReservations(trainId)) {
            sb.append("<tr><td>").append(r.getReservationId()).append("</td><td>").append(r.getTrainId())
              .append("</td><td>").append(esc(r.getPassengerName())).append("</td><td>").append(r.seatText())
              .append("</td><td class='").append(r.getStatus().toLowerCase()).append("'>").append(r.getStatus())
              .append("</td><td>").append(esc(r.getBookedAt())).append("</td></tr>");
        }
        return page("Reservations", sb.append("</table>").toString());
    }

    private static String waitingPage() throws SQLException {
        StringBuilder sb = new StringBuilder("<table><tr><th>Train</th><th>Queue position</th><th>ResID</th><th>Passenger</th></tr>");
        List<Reservation> list = system.getWaitingList(0);
        for (Reservation r : list) {
            sb.append("<tr><td>").append(r.getTrainId()).append("</td><td>").append(r.getWaitingPosition())
              .append("</td><td>").append(r.getReservationId()).append("</td><td>").append(esc(r.getPassengerName()))
              .append("</td></tr>");
        }
        sb.append("</table>");
        if (list.isEmpty()) sb.append("<p>The waiting list is empty.</p>");
        return page("Waiting lists (first in, first out)", sb.toString());
    }

    
private static String platformsPage(
        Map<String, String> form, boolean post) throws SQLException {

    StringBuilder sb = new StringBuilder();

    // Handle platform request and allocation actions
    if (post) {
        String action = form.getOrDefault("action", "");

        try {
            if ("request".equals(action)) {

                int trainId = Integer.parseInt(
                        form.getOrDefault("trainId", "").trim());

                String type = form.getOrDefault("requestType", "SCHEDULED");

                if (!type.equals("EMERGENCY") &&
                    !type.equals("SCHEDULED")) {
                    throw new IllegalArgumentException("Invalid train type.");
                }

                sb.append("<p class='success'>")
                  .append(esc(system.addPlatformRequest(trainId, type)))
                  .append("</p>");

            } else if ("allocate".equals(action)) {

                sb.append("<p class='success'>")
                  .append(esc(system.allocatePlatforms()))
                  .append("</p>");
            }

        } catch (Exception e) {
            sb.append("<p class='err'>")
              .append(esc(e.getMessage()))
              .append("</p>");
        }
    }

    // Platform request form
    sb.append("<h2>Platform Management</h2>");

    sb.append("<h3>Request Platform for Train</h3>");

    sb.append("<form method='post' action='/platforms'>");

    sb.append("<label>Train ID</label>")
      .append("<input type='number' name='trainId' required>");

    sb.append("<label>Train Type</label>")
      .append("<select name='requestType' required>")
      .append("<option value='SCHEDULED'>Scheduled Train</option>")
      .append("<option value='EMERGENCY'>Emergency Train</option>")
      .append("</select>");

    sb.append("<input type='hidden' name='action' value='request'>");

    sb.append("<button type='submit'>Add Platform Request</button>");

    sb.append("</form>");

    // Allocate platform button
    sb.append("<form method='post' action='/platforms'>")
      .append("<input type='hidden' name='action' value='allocate'>")
      .append("<button type='submit'>Allocate Platforms</button>")
      .append("</form>");

    // Platform status table
    List<Platform> platforms = system.getPlatformStatus();

    sb.append("<h3>Platform Availability</h3>");

    sb.append("<table><tr>")
      .append("<th>Platform</th>")
      .append("<th>Length</th>")
      .append("<th>Status</th>")
      .append("<th>Booked Time Slots</th>")
      .append("</tr>");

    for (Platform p : platforms) {

        sb.append("<tr><td>")
          .append(esc(p.getName()))
          .append("</td><td>")
          .append(p.getLengthMeters())
          .append(" m</td><td>")
          .append(esc(p.getStatusText()))
          .append("</td><td>");

        for (PlatformRequest b : p.getBookings()) {

            sb.append(esc(b.getStatus()))
              .append(": Train ")
              .append(b.getTrainId())
              .append(" ")
              .append(esc(b.getTrain().getTrainName()))
              .append(" ")
              .append(RailwaySystem.formatTime(b.getStart()))
              .append("-")
              .append(RailwaySystem.formatTime(b.getEnd()))
              .append("<br>");
        }

        sb.append("</td></tr>");
    }

    sb.append("</table>");

    sb.append("<p>")
      .append(esc(system.getNextReleaseSummary(platforms)))
      .append("</p>");

    // All platform requests
    sb.append("<h3>Platform Requests</h3>");

    sb.append("<table><tr>")
      .append("<th>Request ID</th>")
      .append("<th>Train ID</th>")
      .append("<th>Train Name</th>")
      .append("<th>Train Type</th>")
      .append("<th>Status</th>")
      .append("<th>Platform</th>")
      .append("</tr>");

    for (PlatformRequest r : system.getPlatformRequests()) {

        sb.append("<tr><td>")
          .append(r.getRequestId())
          .append("</td><td>")
          .append(r.getTrainId())
          .append("</td><td>")
          .append(esc(r.getTrain().getTrainName()))
          .append("</td><td>")
          .append(esc(r.getTypeName()))
          .append("</td><td>")
          .append(esc(r.getStatus()))
          .append("</td><td>")
          .append(r.getPlatformId() == 0
                  ? "-"
                  : String.valueOf(r.getPlatformId()))
          .append("</td></tr>");
    }

    sb.append("</table>");

    sb.append("<p><a href='/admin-dashboard'>Back to Admin Dashboard</a></p>");

    return page("Platform Management", sb.toString());
}

    
private static String registerPage(
        Map<String, String> form, boolean post) {

    String message = "";

    if (post) {
        String name = val(form, "name");
        String email = val(form, "email");
        String phone = val(form, "phone");
        String password = val(form, "password");

        try {
            boolean registered = UserAuth.registerUser(
                    name, email, phone, password);

            if (registered) {
                message = "Registration successful! You can now log in.";
            } else {
                message = "Registration failed. Check your details or email.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            message = "Registration error: " + e.getMessage();
        }
    }

    return page("User Registration",
        "<h2>Create Your Account</h2>" +
        "<p>" + esc(message) + "</p>" +
        "<form method='post' action='/register'>" +
        "<label>Full Name</label><br>" +
        "<input type='text' name='name' required><br><br>" +
        "<label>Email</label><br>" +
        "<input type='email' name='email' required><br><br>" +
        "<label>Phone Number</label><br>" +
        "<input type='text' name='phone' required><br><br>" +
        "<label>Password</label><br>" +
        "<input type='password' name='password' required><br><br>" +
        "<button type='submit'>Register</button>" +
        "</form>"
    );
}


private static String loginPage(
        Map<String, String> form, boolean post, HttpExchange ex) {

    String message = "";

    if (post) {
        String email = val(form, "email");
        String password = val(form, "password");

        try {
            UserAuth.AuthUser user = UserAuth.login(email, password);

            if (user != null && user.isUser()) {

                // Create a unique session ID
                String sessionId = UUID.randomUUID().toString();

                // Store the logged-in user
                sessions.put(sessionId, user);

                // Send session cookie to the browser
                ex.getResponseHeaders().add(
                    "Set-Cookie",
                    "PJ_SESSION=" + sessionId
                    + "; Path=/; HttpOnly; SameSite=Lax"
                );

                // Redirect to user dashboard
                return page("Login Successful",
                    "<h2>Welcome, " + esc(user.getName()) + "!</h2>"
                    + "<p>Login successful. Opening your dashboard...</p>"
                    + "<meta http-equiv='refresh' content='1;url=/dashboard'>"
                    + "<p><a href='/dashboard'>Click here if not redirected</a></p>");

            } else {
                message = "Invalid user email or password.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            message = "Login error: " + esc(e.getMessage());
        }
    }

    // Display login form
    return page("User Login",
        "<h2>User Login</h2>"
        + "<p>" + message + "</p>"
        + "<form method='post' action='/login'>"
        + "<label>Email</label><br>"
        + "<input type='email' name='email' required><br><br>"
        + "<label>Password</label><br>"
        + "<input type='password' name='password' required><br><br>"
        + "<button type='submit'>Login</button>"
        + "</form>"
        + "<p>New user? <a href='/register'>Register here</a></p>"
    );
}



private static String userDashboard(HttpExchange ex) {

    String sessionId = null;

    // Get browser cookie
    String cookie = ex.getRequestHeaders().getFirst("Cookie");

    if (cookie != null) {
        for (String item : cookie.split(";")) {
            item = item.trim();

            if (item.startsWith("PJ_SESSION=")) {
                sessionId = item.substring("PJ_SESSION=".length());
                break;
            }
        }
    }

    // Find logged-in user
    UserAuth.AuthUser user =
            sessionId == null ? null : sessions.get(sessionId);

    // Check login
    if (user == null || !user.isUser()) {
        return page("Login Required",
                "<h2>Please log in first.</h2>"
                + "<p><a href='/login'>User Login</a></p>");
    }

    // User Dashboard
    return page("User Dashboard",

            // Logout button at top-right
            "<div style='display:flex; justify-content:flex-end; margin-bottom:20px;'>"
            + "<a href='/logout' style='background:#dc3545; color:white;"
            + "padding:10px 22px; border-radius:6px;"
            + "text-decoration:none; font-weight:bold;'>Logout</a>"
            + "</div>"

            // Welcome section
            + "<div style='text-align:center; padding:20px'>"

            + "<h1 style='color:#0b3d6b'>Welcome, "
            + esc(user.getName()) + "!</h1>"

            + "<p>Welcome to Payanam Junction.</p>"
            + "<p>Manage your railway reservations easily.</p>"

            + "</div>"

            // Available Services
            + "<h2>Available Services</h2>"

            + "<div class='dashboard-grid'>"

            // 1. Search Trains
            + "<div class='service-card'>"
            + "<h3>Search Trains</h3>"
            + "<p>Find available trains and check train details.</p>"
            + "<a class='service-btn' href='/trains'>Search Now</a>"
            + "</div>"

            // 2. Book Ticket
            + "<div class='service-card'>"
            + "<h3>Book Ticket</h3>"
            + "<p>Reserve your railway tickets easily.</p>"
            + "<a class='service-btn' href='/book'>Book Now</a>"
            + "</div>"

            // 3. My Reservations
            + "<div class='service-card'>"
            + "<h3>My Reservations</h3>"
            + "<p>View your reservation details.</p>"
            + "<a class='service-btn' href='/reservations'>View Reservations</a>"
            + "</div>"

            // 4. Cancel Ticket
            + "<div class='service-card'>"
            + "<h3>Cancel Ticket</h3>"
            + "<p>Manage and cancel your bookings.</p>"
            + "<a class='service-btn' href='/cancel'>Cancel Ticket</a>"
            + "</div>"

            // 5. Passenger Registration
            + "<div class='service-card'>"
            + "<h3>Passenger Registration</h3>"
            + "<p>Register your passenger details.</p>"
            + "<a class='service-btn' href='/passenger-register'>Register Now</a>"
            + "</div>"

            // 6. Waiting List
            + "<div class='service-card'>"
            + "<h3>Waiting List</h3>"
            + "<p>Check waiting list information.</p>"
            + "<a class='service-btn' href='/waiting'>View Waiting List</a>"
            + "</div>"

            + "</div>"
    );
}


private static String logoutPage(HttpExchange ex) {

    // Get browser cookie
    String cookie = ex.getRequestHeaders().getFirst("Cookie");

    if (cookie != null) {
        for (String item : cookie.split(";")) {
            item = item.trim();

            if (item.startsWith("PJ_SESSION=")) {

                String sessionId =
                        item.substring("PJ_SESSION=".length());

                // Remove user session
                sessions.remove(sessionId);
                break;
            }
        }
    }

    // Clear browser cookie
    ex.getResponseHeaders().add(
        "Set-Cookie",
        "PJ_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
    );

    return page("Logged Out",
        "<h2>You have logged out successfully!</h2>"
        + "<p><a href='/'>Return to Homepage</a></p>");
}


private static String adminLoginPage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) {

    String message = "";

    if (post) {
        String email = val(form, "email");
        String password = val(form, "password");

        try {
            UserAuth.AuthUser admin =
                    UserAuth.login(email, password);

            if (admin != null && admin.isAdmin()) {

                String sessionId =
                        UUID.randomUUID().toString();

                sessions.put(sessionId, admin);

                ex.getResponseHeaders().add(
                    "Set-Cookie",
                    "PJ_SESSION=" + sessionId
                    + "; Path=/; HttpOnly; SameSite=Lax"
                );

                return page("Admin Login Successful",
                    "<h2>Welcome, "
                    + esc(admin.getName()) + "!</h2>"
                    + "<p>Opening Admin Dashboard...</p>"
                    + "<meta http-equiv='refresh' "
                    + "content='1;url=/admin-dashboard'>"
                    + "<p><a href='/admin-dashboard'>"
                    + "Open Admin Dashboard</a></p>"
                );

            } else {
                message = "Invalid admin email or password.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            message = "Login error: " + esc(e.getMessage());
        }
    }

    return page("Admin Login",
        "<h2>Administrator Login</h2>"
        + "<p>" + message + "</p>"
        + "<form method='post' action='/admin'>"
        + "<label>Email</label><br>"
        + "<input type='email' name='email' required><br><br>"
        + "<label>Password</label><br>"
        + "<input type='password' name='password' required><br><br>"
        + "<button type='submit'>Admin Login</button>"
        + "</form>"
        + "<p><a href='/'>Back to Home</a></p>"
    );
}



    // ------------------------------------------------------------------ helpers
    

private static String page(String title, String body) {

    String homeContent = "";

    if ("Home".equals(title)) {
        homeContent =
            "<div class='welcome'>"
            + "<h1>Welcome to Payanam Junction</h1>"
            + "<p>Your Railway Reservation System</p>"
            + "<div class='login-container'>"

            + "<div class='login-card user-card'>"
            + "<h2>User Login</h2>"
            + "<p>Book tickets and manage your reservations.</p>"
            + "<a class='login-btn' href='/login'>User Login</a>"
            + "<p>New user? <a href='/register'>Register here</a></p>"
            + "</div>"

            + "<div class='login-card admin-card'>"
            + "<h2>Admin Login</h2>"
            + "<p>Manage trains, passengers and platforms.</p>"
            + "<a class='login-btn admin-btn' href='/admin'>Admin Login</a>"
            + "</div>"

            + "</div></div>";
    }

    return "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<title>" + esc(title) + " - Payanam Junction</title>"

            + "<style>"

            // General page design
            + "body{font-family:Arial,sans-serif;margin:0;"
            + "background:#f0f4f8;color:#222}"

            + "main{padding:25px;max-width:1100px;margin:auto}"

            // Homepage design
            + ".welcome{text-align:center;padding:35px 10px}"
            + ".welcome h1{color:#0b3d6b;font-size:34px}"
            + ".welcome>p{color:#555;font-size:18px}"

            // Login cards
            + ".login-container{display:flex;justify-content:center;"
            + "gap:30px;flex-wrap:wrap;margin-top:35px}"

            + ".login-card{background:white;width:300px;padding:30px;"
            + "border-radius:15px;box-shadow:0 4px 15px rgba(0,0,0,0.12)}"

            + ".login-card h2{color:#0b3d6b}"
            + ".login-card p{color:#555;line-height:1.6}"

            // Buttons
            + ".login-btn{display:inline-block;background:#0b3d6b;color:white;"
            + "padding:12px 25px;text-decoration:none;border-radius:7px;"
            + "margin-top:15px}"

            + ".login-btn:hover{background:#1766a8}"

            + ".admin-btn{background:#198754}"
            + ".admin-btn:hover{background:#126b40}"

            // Dashboard service links
            + "main a{color:#0b3d6b;font-weight:bold}"

            // Tables
            + "table{border-collapse:collapse;background:white;margin:10px 0}"
            + "th,td{border:1px solid #ccc;padding:8px 10px;text-align:left}"
            + "th{background:#dde7f0}"

            // Forms
            + "label{display:block;margin-top:10px;font-weight:bold}"
            + "input,select{padding:8px;min-width:260px;"
            + "max-width:100%;box-sizing:border-box}"

            + "button{margin-top:12px;padding:9px 18px;"
            + "background:#0b3d6b;color:white;border:0;cursor:pointer}"

            + "button.small{margin:0;padding:4px 10px;background:#b3261e}"

            // Status messages
            + ".ok{background:#d9f2dd;padding:10px;border-left:5px solid #2e7d32}"
            + ".err{background:#fde0de;padding:10px;border-left:5px solid #b3261e}"
            + ".cancelled{color:#b3261e}"
            + ".waiting{color:#b26a00}"
            + ".confirmed{color:#2e7d32}"

            // Mobile design
            
            // Dashboard service cards
            + ".dashboard-grid{display:grid;"
            + "grid-template-columns:repeat(auto-fit,minmax(220px,1fr));"
            + "gap:20px;margin-top:25px}"

            + ".service-card{background:white;padding:25px;"
            + "border-radius:12px;text-align:center;"
            + "box-shadow:0 4px 12px rgba(0,0,0,0.10)}"

            + ".service-card h3{color:#0b3d6b}"
            + ".service-card p{color:#666}"

            + ".service-btn{display:inline-block;background:#0b3d6b;"
            + "color:white;padding:10px 20px;"
            + "border-radius:6px;text-decoration:none}"

            + ".service-btn:hover{background:#1766a8}"

            + "@media(max-width:650px){"
            + ".login-container{flex-direction:column;align-items:center}"
            + ".login-card{width:80%}"
            + "}"

            + "</style></head><body>"

            // No navigation bar

            + "<main>"
            + (homeContent.isEmpty()
                ? "<h2>" + esc(title) + "</h2>" + body
                : homeContent)
            + "</main></body></html>";
}


    private static String message(String msg) {
        if (msg == null || msg.isEmpty()) return "";
        return "<p class='" + (msg.startsWith("OK") ? "ok" : "err") + "'>" + esc(msg) + "</p>";
    }

    private static String val(Map<String, String> f, String key) {
        String v = f.get(key);
        return v == null ? "" : v.trim();
    }

    private static Map<String, String> parseForm(String text) {
        Map<String, String> map = new HashMap<>();
        if (text == null || text.isEmpty()) return map;
        for (String pair : text.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            map.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    /** Escapes < > & so user text can never break the page. */
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void send(HttpExchange ex, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
    


private static String adminDashboard(HttpExchange ex) {

    String cookie = ex.getRequestHeaders().getFirst("Cookie");
    String sessionId = null;

    if (cookie != null) {
        for (String part : cookie.split(";")) {
            part = part.trim();

            if (part.startsWith("PJ_SESSION=")) {
                sessionId = part.substring("PJ_SESSION=".length());
            }
        }
    }

    UserAuth.AuthUser admin = sessions.get(sessionId);

    if (admin == null || !admin.isAdmin()) {
        return page("Access Denied",
                "<p class='err'>Please login as administrator.</p>"
                + "<p><a href='/admin'>Admin Login</a></p>");
    }

    String body =
        "<style>"

        + ".admin-top{"
        + "display:flex;"
        + "justify-content:space-between;"
        + "align-items:center;"
        + "margin-bottom:20px;"
        + "}"

        + ".admin-logout{"
        + "background:#dc3545;"
        + "color:white;"
        + "padding:10px 20px;"
        + "border-radius:6px;"
        + "text-decoration:none;"
        + "font-weight:bold;"
        + "}"

        + ".admin-grid{"
        + "display:grid;"
        + "grid-template-columns:repeat(3,minmax(0,1fr));"
        + "gap:20px;"
        + "margin-top:25px;"
        + "}"

        + ".admin-card{"
        + "background:white;"
        + "padding:22px;"
        + "border-radius:12px;"
        + "box-shadow:0 3px 10px rgba(0,0,0,0.10);"
        + "display:flex;"
        + "flex-direction:column;"
        + "min-height:190px;"
        + "box-sizing:border-box;"
        + "}"

        + ".admin-card h3{"
        + "margin-top:0;"
        + "color:#243b55;"
        + "}"

        + ".admin-card p{"
        + "color:#666;"
        + "flex-grow:1;"
        + "}"

        + ".admin-btn{"
        + "display:inline-block;"
        + "background:#2563eb;"
        + "color:white;"
        + "padding:10px 14px;"
        + "border-radius:6px;"
        + "text-decoration:none;"
        + "text-align:center;"
        + "margin-top:8px;"
        + "}"

        + ".admin-btn:hover{"
        + "background:#1d4ed8;"
        + "}"

        + "@media(max-width:750px){"
        + ".admin-grid{grid-template-columns:repeat(2,minmax(0,1fr));}"
        + "}"

        + "@media(max-width:500px){"
        + ".admin-grid{grid-template-columns:1fr;}"
        + "}"

        + "</style>"

        + "<div class='admin-top'>"
        + "<h2>Admin Dashboard</h2>"
        + "<a class='admin-logout' href='/logout'>Logout</a>"
        + "</div>"

        + "<h3>Welcome, "
        + esc(admin.getName())
        + "!</h3>"

        + "<p>Manage your railway reservation system.</p>"

        + "<div class='admin-grid'>"

        // Train Management
        + "<div class='admin-card'>"
        + "<h3>Train Management</h3>"
        + "<p>Add new trains and view all available trains.</p>"
        + "<a class='admin-btn' href='/add-train'>Add New Train</a>"
        + "<a class='admin-btn' href='/trains'>View All Trains</a>"
        + "</div>"

        // Passenger Management
        + "<div class='admin-card'>"
        + "<h3>Passenger Management</h3>"
        + "<p>View and manage passenger details.</p>"
        + "<a class='admin-btn' href='/passengers'>Manage Passengers</a>"
        + "</div>"

        // Reservation Management
        + "<div class='admin-card'>"
        + "<h3>Reservation Management</h3>"
        + "<p>View and manage railway reservations.</p>"
        + "<a class='admin-btn' href='/reservations'>View Reservations</a>"
        + "</div>"

        // Platform Management
        + "<div class='admin-card'>"
        + "<h3>Platform Management</h3>"
        + "<p>Manage railway platform allocation.</p>"
        + "<a class='admin-btn' href='/platforms'>Manage Platforms</a>"
        + "</div>"

        // Waiting List
        + "<div class='admin-card'>"
        + "<h3>Waiting List</h3>"
        + "<p>View passengers waiting for confirmation.</p>"
        + "<a class='admin-btn' href='/waiting'>View Waiting List</a>"
        + "</div>"

        // Arrival Delay
        + "<div class='admin-card'>"
        + "<h3>Arrival Time Delay</h3>"
        + "<p>Update delayed train arrival times.</p>"
        + "<a class='admin-btn' href='/delay-train'>Update Arrival Delay</a>"
        + "</div>"

        // Departure Delay
        + "<div class='admin-card'>"
        + "<h3>Departure Time Delay</h3>"
        + "<p>Update delayed train departure times.</p>"
        + "<a class='admin-btn' href='/departure-delay'>Update Departure Delay</a>"
        + "</div>"

        // Departure Confirmation
        + "<div class='admin-card'>"
        + "<h3>Departure Confirmation</h3>"
        + "<p>Confirm train departure from platforms.</p>"
        + "<a class='admin-btn' href='/confirm-departure'>Confirm Departure</a>"
        + "</div>"
        + "</div>";

    return page("Admin Dashboard", body);
}


private static String addTrainPage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) throws SQLException {

    // Check administrator session
    String sessionId = null;
    String cookie = ex.getRequestHeaders().getFirst("Cookie");

    if (cookie != null) {
        for (String item : cookie.split(";")) {
            item = item.trim();

            if (item.startsWith("PJ_SESSION=")) {
                sessionId = item.substring("PJ_SESSION=".length());
                break;
            }
        }
    }

    UserAuth.AuthUser admin =
            sessionId == null ? null : sessions.get(sessionId);

    if (admin == null || !admin.isAdmin()) {
        return page("Access Denied",
            "<h2>Administrator login required.</h2>"
            + "<a href='/admin'>Admin Login</a>");
    }

    String message = "";

    if (post) {
        try {
            String name = val(form, "name");
            String origin = val(form, "origin");
            String destination = val(form, "destination");
            String arrival = val(form, "arrival");
            String departure = val(form, "departure");
            int length = Integer.parseInt(val(form, "length"));

            message = system.addTrain(
                name, origin, destination,
                arrival, departure, length
            );

        } catch (NumberFormatException e) {
            message = "ERROR: Please enter a valid train length.";
        }
    }

    return page("Add New Train",
        "<h2>Add New Train</h2>"
        + "<p>" + esc(message) + "</p>"

        + "<form method='post' action='/add-train'>"

        + "<label>Train Name</label><br>"
        + "<input type='text' name='name' required><br><br>"

        + "<label>Origin</label><br>"
        + "<input type='text' name='origin' required><br><br>"

        + "<label>Destination</label><br>"
        + "<input type='text' name='destination' required><br><br>"

        + "<label>Arrival Time (HH:mm)</label><br>"
        + "<input type='time' name='arrival' required><br><br>"

        + "<label>Departure Time (HH:mm)</label><br>"
        + "<input type='time' name='departure' required><br><br>"

        + "<label>Train Length (metres)</label><br>"
        + "<input type='number' name='length' min='1' required><br><br>"

        + "<button type='submit'>Add Train</button>"
        + "</form>"

        + "<p><a href='/trains'>View All Trains</a></p>"
        + "<p><a href='/admin-dashboard'>Back to Dashboard</a></p>"
    );
}

private static String passengerRegistrationPage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) throws SQLException {

    // Check user login session
    String sessionId = null;
    String cookie = ex.getRequestHeaders().getFirst("Cookie");

    if (cookie != null) {
        for (String item : cookie.split(";")) {
            item = item.trim();

            if (item.startsWith("PJ_SESSION=")) {
                sessionId = item.substring("PJ_SESSION=".length());
                break;
            }
        }
    }

    UserAuth.AuthUser user =
            sessionId == null ? null : sessions.get(sessionId);

    if (user == null || !user.isUser()) {
        return page("Login Required",
            "<h2>Please log in first.</h2>"
            + "<a href='/login'>User Login</a>");
    }

    String message = "";

    // Save passenger details
    if (post) {
        try {
            String name = val(form, "name");
            int age = Integer.parseInt(val(form, "age"));
            String phone = val(form, "phone");

            message = system.addPassenger(name, age, phone);

        } catch (NumberFormatException e) {
            message = "ERROR: Please enter a valid age.";
        }
    }

    return page("Passenger Registration",

        "<h2>Passenger Registration</h2>"

        + "<p>" + esc(message) + "</p>"

        + "<form method='post' action='/passenger-register'>"

        + "<label>Passenger Name</label><br>"
        + "<input type='text' name='name' required><br><br>"

        + "<label>Age</label><br>"
        + "<input type='number' name='age' min='1' max='120' required><br><br>"

        + "<label>Phone Number</label><br>"
        + "<input type='tel' name='phone' required><br><br>"

        + "<button type='submit'>Register Passenger</button>"

        + "</form>"

        + "<p><a href='/dashboard'>Back to User Dashboard</a></p>"
    );
}

private static String delayTrainPage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) {

    String sessionId = null;
    String cookie = ex.getRequestHeaders().getFirst("Cookie");

    if (cookie != null) {
        for (String item : cookie.split(";")) {
            item = item.trim();

            if (item.startsWith("PJ_SESSION=")) {
                sessionId = item.substring("PJ_SESSION=".length());
                break;
            }
        }
    }

    UserAuth.AuthUser admin =
            sessionId == null ? null : sessions.get(sessionId);

    if (admin == null || !admin.isAdmin()) {
        return page("Login Required",
                "<h2>Administrator login required.</h2>"
                + "<a href='/admin'>Admin Login</a>");
    }

    String msg = "";

    if (post) {
        try {
            String trainId = val(form, "trainId");
            int delayMinutes =
                    Integer.parseInt(val(form, "delayMinutes"));

            msg = system.delayTrain(trainId, delayMinutes);

        } catch (NumberFormatException e) {
            msg = "Please enter a valid delay in minutes.";
        }
    }

    return page("Arrival Time Delay",

            "<div style='max-width:500px;margin:30px auto;"
            + "background:white;padding:30px;border-radius:12px;"
            + "box-shadow:0 4px 12px rgba(0,0,0,0.10)'>"

            + "<h2 style='color:#0b3d6b;text-align:center'>"
            + "Arrival Time Delay</h2>"

            + message(msg)

            + "<form method='post' action='/delay-train'>"

            + "<label>Train ID</label>"
            + "<input name='trainId' required "
            + "placeholder='Enter Train ID'>"

            + "<label>Delay (Minutes)</label>"
            + "<input type='number' name='delayMinutes' "
            + "min='1' required placeholder='Enter delay minutes'>"

            + "<button type='submit'>Update Arrival Delay</button>"

            + "</form>"

            + "<div style='text-align:center;margin-top:20px'>"
            + "<a href='/admin-dashboard'>Back to Admin Dashboard</a>"
            + "</div>"

            + "</div>"
    );
}
private static String departureDelayPage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) {

    String message = "";

    String cookie = ex.getRequestHeaders().getFirst("Cookie");
    String sessionId = null;

    if (cookie != null) {
        for (String part : cookie.split(";")) {
            part = part.trim();

            if (part.startsWith("PJ_SESSION=")) {
                sessionId = part.substring("PJ_SESSION=".length());
            }
        }
    }

    UserAuth.AuthUser admin = sessions.get(sessionId);

    if (admin == null || !admin.isAdmin()) {
        return page("Access Denied",
                "<p class='err'>Admin login required.</p>");
    }

    if (post) {
        try {
            String trainId = form.getOrDefault("trainId", "").trim();
            int minutes = Integer.parseInt(
                    form.getOrDefault("delayMinutes", "0"));

            message = "<p class='success'>" +
                    esc(system.delayDepartureTrain(trainId, minutes)) +
                    "</p>";

        } catch (Exception e) {
            message = "<p class='err'>" +
                    esc(e.getMessage()) + "</p>";
        }
    }

    String body =
            "<h2>Departure Time Delay</h2>" +
            message +
            "<form method='post' action='/departure-delay'>" +
            "<label>Train ID</label>" +
            "<input type='text' name='trainId' required>" +
            "<label>Delay (Minutes)</label>" +
            "<input type='number' name='delayMinutes' min='1' required>" +
            "<button type='submit'>Update Departure Delay</button>" +
            "</form>" +
            "<p><a href='/admin-dashboard'>Back to Admin Dashboard</a></p>";

    return page("Departure Time Delay", body);
}
private static String confirmDeparturePage(
        Map<String, String> form,
        boolean post,
        HttpExchange ex) {

    String message = "";

    if (post) {
        try {
            String cookie = ex.getRequestHeaders()
                    .getFirst("Cookie");

            String sessionId = null;

            if (cookie != null) {
                for (String part : cookie.split(";")) {
                    part = part.trim();

                    if (part.startsWith("PJ_SESSION=")) {
                        sessionId = part.substring("PJ_SESSION=".length());
                    }
                }
            }

            UserAuth.AuthUser admin = sessions.get(sessionId);

            if (admin == null || !admin.isAdmin()) {
                return page("Access Denied",
                        "<p class='err'>Admin login required.</p>");
            }

            String trainId = form.getOrDefault("trainId", "").trim();

            if (trainId.isEmpty()) {
                message = "<p class='err'>Please enter Train ID.</p>";
            } else {
                message = "<p class='success'>" +
                        esc(system.confirmDeparture(trainId)) +
                        "</p>";
            }

        } catch (Exception e) {
            message = "<p class='err'>" +
                    esc(e.getMessage()) + "</p>";
        }
    }

    String body =
            "<h2>Confirm Train Departure</h2>" +
            message +
            "<form method='post' action='/confirm-departure'>" +
            "<label>Train ID</label>" +
            "<input type='text' name='trainId' required>" +
            "<button type='submit'>Confirm Departure</button>" +
            "</form>" +
            "<p><a href='/admin-dashboard'>Back to Admin Dashboard</a></p>";

    return page("Confirm Departure", body);
}



}
