package com.ashwin.backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

/**
 * PortfolioServer - Lightweight, zero-dependency Java Backend for Ashwin Sinha's Portfolio.
 * 
 * Capabilities:
 * 1. Serves the portfolio website on http://localhost:8080
 * 2. REST API: GET /api/status - Health check
 * 3. REST API: GET /api/profile - Profile metadata & bio
 * 4. REST API: GET /api/projects - Projects catalog
 * 5. REST API: POST /api/contact - Saves inquiries to messages.json
 * 6. REST API: GET /api/contact - View received inquiries
 */
public class PortfolioServer {

    private static final int PORT = 8080;
    private static final Path MESSAGES_FILE = Paths.get("messages.json");
    private static final Path STATIC_DIR = Paths.get(".."); // points to root where index.html resides

    public static void main(String[] args) throws IOException {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Virtual threads in Java 21+

        // API Endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/profile", new ProfileHandler());
        server.createContext("/api/projects", new ProjectsHandler());
        server.createContext("/api/contact", new ContactHandler());

        // Static File Serving (HTML/CSS/JS)
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==================================================");
        System.out.println("  Ashwin Sinha - Portfolio Java Backend Server    ");
        System.out.println("==================================================");
        System.out.println("  Server is LIVE at: http://localhost:" + port);
        System.out.println("  API Status:        http://localhost:" + port + "/api/status");
        System.out.println("  API Profile:       http://localhost:" + port + "/api/profile");
        System.out.println("  API Projects:      http://localhost:" + port + "/api/projects");
        System.out.println("  Contact Endpoint:  http://localhost:" + port + "/api/contact");
        System.out.println("==================================================");
        System.out.println("Press Ctrl+C to stop the server.\n");
    }

    // =========================================================================
    // API: Status
    // =========================================================================
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String json = """
                {
                  "status": "UP",
                  "service": "Ashwin Sinha Portfolio Backend",
                  "runtime": "Java %s",
                  "timestamp": "%s"
                }
                """.formatted(System.getProperty("java.version"), LocalDateTime.now());

            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // API: Profile
    // =========================================================================
    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String json = """
                {
                  "name": "Ashwin Sinha",
                  "title": "Java Full Stack & Backend Developer",
                  "location": "Gorakhpur, Uttar Pradesh, India",
                  "email": "ashwinsinha764@gmail.com",
                  "phone": "+91 6307139892",
                  "education": {
                    "degree": "Bachelor of Computer Applications (BCA)",
                    "institution": "Institute of Technology and Management",
                    "years": "2024 - 2027"
                  },
                  "skills": {
                    "languages": ["Java", "SQL", "C", "C++", "JavaScript", "HTML", "CSS"],
                    "frameworks": ["Spring Boot", "Spring", "Hibernate", "JDBC", "REST APIs"],
                    "core": ["Data Structures & Algorithms", "Object-Oriented Programming (OOP)", "DBMS", "Operating Systems"],
                    "databases": ["MySQL"],
                    "tools": ["Git", "GitHub", "VS Code", "Eclipse"]
                  }
                }
                """;

            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // API: Projects
    // =========================================================================
    static class ProjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String json = """
                [
                  {
                    "id": "hotel-mgmt",
                    "title": "Hotel Management System",
                    "stack": ["Java", "JDBC", "MySQL", "OOPs"],
                    "summary": "Desktop backend application managing room reservations, customer records, and billing with transactional consistency.",
                    "highlights": [
                      "Structured around modular OOP design separating model, database access (DAO), and business logic.",
                      "Integrated JDBC connection pooling and transactions with MySQL to ensure booking data consistency.",
                      "Designed billing calculation and room status state tracking."
                    ]
                  },
                  {
                    "id": "hospital-mgmt",
                    "title": "Hospital Management System",
                    "stack": ["Java", "MySQL", "DBMS"],
                    "summary": "Healthcare administration system tracking patient medical records, doctor schedules, and appointments.",
                    "highlights": [
                      "Structured normalized MySQL schema to prevent patient records redundancy.",
                      "Optimized SQL join queries to ensure quick appointment retrieval and minimal latency.",
                      "Implemented strict input validation and boundary checks."
                    ]
                  },
                  {
                    "id": "myntra-clone",
                    "title": "Myntra E-Commerce Clone",
                    "stack": ["HTML5", "CSS3", "JavaScript"],
                    "summary": "Responsive e-commerce frontend replicating core user flows of the Myntra shopping experience.",
                    "highlights": [
                      "Clean CSS Flexbox & CSS Grid multi-device layout.",
                      "Dynamic JavaScript cart calculations and item filtering."
                    ]
                  }
                ]
                """;

            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // API: Contact (POST to save, GET to read)
    // =========================================================================
    static class ContactHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String name = extractField(body, "name");
                String email = extractField(body, "email");
                String subject = extractField(body, "subject");
                String message = extractField(body, "message");

                if (name == null || name.isBlank() || email == null || email.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"error\": \"Name and email are required\"}");
                    return;
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String entry = """
                    {"timestamp": "%s", "name": "%s", "email": "%s", "subject": "%s", "message": "%s"}
                    """.formatted(
                        escape(timestamp),
                        escape(name),
                        escape(email),
                        escape(subject != null ? subject : ""),
                        escape(message != null ? message : "")
                ).trim();

                saveMessage(entry);
                System.out.println("[NEW MESSAGE RECEIVED] From: " + name + " (" + email + ") - " + subject);

                String response = """
                    {
                      "success": true,
                      "message": "Thanks %s! Your message has been received by Ashwin's Java backend.",
                      "receivedAt": "%s"
                    }
                    """.formatted(name, timestamp);

                sendJsonResponse(exchange, 201, response);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String messagesJson = readMessages();
                sendJsonResponse(exchange, 200, messagesJson);
                return;
            }

            exchange.sendResponseHeaders(405, -1);
        }

        private synchronized void saveMessage(String entry) {
            try {
                if (!Files.exists(MESSAGES_FILE)) {
                    Files.writeString(MESSAGES_FILE, "[\n  " + entry + "\n]", StandardCharsets.UTF_8);
                } else {
                    String content = Files.readString(MESSAGES_FILE, StandardCharsets.UTF_8).trim();
                    if (content.endsWith("]")) {
                        content = content.substring(0, content.length() - 1).trim();
                        if (content.length() > 1 && !content.endsWith("[")) {
                            content += ",\n  " + entry + "\n]";
                        } else {
                            content += "\n  " + entry + "\n]";
                        }
                        Files.writeString(MESSAGES_FILE, content, StandardCharsets.UTF_8);
                    } else {
                        Files.writeString(MESSAGES_FILE, "[\n  " + entry + "\n]", StandardCharsets.UTF_8);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error saving message: " + e.getMessage());
            }
        }

        private synchronized String readMessages() {
            try {
                if (Files.exists(MESSAGES_FILE)) {
                    return Files.readString(MESSAGES_FILE, StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {}
            return "[]";
        }

        private String extractField(String payload, String field) {
            // Supports JSON: "field": "value" or URL-encoded: field=value
            if (payload.contains("\"" + field + "\"")) {
                int keyIdx = payload.indexOf("\"" + field + "\"");
                int colonIdx = payload.indexOf(":", keyIdx);
                if (colonIdx != -1) {
                    int valStart = payload.indexOf("\"", colonIdx) + 1;
                    int valEnd = payload.indexOf("\"", valStart);
                    if (valStart > 0 && valEnd > valStart) {
                        return payload.substring(valStart, valEnd);
                    }
                }
            }
            // URL-encoded fallback
            for (String pair : payload.split("&")) {
                String[] parts = pair.split("=");
                if (parts.length >= 2 && parts[0].trim().equalsIgnoreCase(field)) {
                    return URLDecoder.decode(parts[1].trim(), StandardCharsets.UTF_8);
                }
            }
            return "";
        }

        private String escape(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");
        }
    }

    // =========================================================================
    // Static File Handler (Serves index.html, CSS, JS)
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr == null || pathStr.equals("/") || pathStr.isBlank()) {
                pathStr = "index.html";
            } else if (pathStr.startsWith("/")) {
                pathStr = pathStr.substring(1);
            }

            // Look in current dir, then in parent dir
            Path filePath = Paths.get(pathStr);
            if (!Files.exists(filePath)) {
                filePath = STATIC_DIR.resolve(pathStr).normalize();
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                // Fallback to index.html
                filePath = Paths.get("index.html");
                if (!Files.exists(filePath)) {
                    filePath = STATIC_DIR.resolve("index.html").normalize();
                }
            }

            if (!Files.exists(filePath)) {
                String notFound = "<h1>404 Not Found</h1><p>File not found on Ashwin's Java Server.</p>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = getMimeType(filePath.getFileName().toString());
            byte[] fileBytes = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getMimeType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
            if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            if (filename.endsWith(".pdf")) return "application/pdf";
            return "text/plain";
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
