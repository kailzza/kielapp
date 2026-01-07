<?php
require 'db_connect.php';
header('Content-Type: application/json');

$input = file_get_contents("php://input");
$data = json_decode($input, true);

if (isset($data['f_name'], $data['l_name'], $data['email'], $data['pass'])) {
    
    $f_name = $data['f_name'];
    $l_name = $data['l_name'];
    $email = $data['email'];
    $pass = $data['pass'];

    // Check if email already exists
    $checkQuery = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $checkQuery->bind_param("s", $email);
    $checkQuery->execute();
    $checkQuery->store_result();

    if ($checkQuery->num_rows > 0) {
        echo json_encode(["status" => "error", "message" => "Email already exists"]);
    } else {
        // Hash password
        $hashed_password = password_hash($pass, PASSWORD_DEFAULT);

        $stmt = $conn->prepare("INSERT INTO users (f_name, l_name, email, password) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("ssss", $f_name, $l_name, $email, $hashed_password);

        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "User registered successfully"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Registration failed"]);
        }
        $stmt->close();
    }
    $checkQuery->close();
} else {
    echo json_encode(["status" => "error", "message" => "Invalid input"]);
}
$conn->close();
?>