<?php
// --- 1. SECURITY CHECK ---
$valid_api_key = "sk_scholartrack_8f92a3b4c5d6e7f8";

// Get headers (Apache/Nginx)
$headers = getallheaders();
$client_key = isset($headers['X-API-KEY']) ? $headers['X-API-KEY'] : '';

// Reject if key doesn't match
if ($client_key !== $valid_api_key) {
    header('HTTP/1.1 401 Unauthorized');
    echo json_encode(["status" => "error", "message" => "Unauthorized: Invalid API Key"]);
    exit(); // Stop execution immediately
}

// --- 2. DATABASE CONNECTION (Existing Code) ---
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "scholarship_finder";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
?>