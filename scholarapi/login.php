<?php
require 'db_connect.php';
header('Content-Type: application/json');

$input = file_get_contents("php://input");
$data = json_decode($input, true);

if (isset($data['email'], $data['pass'])) {
    $email = $data['email'];
    $pass = $data['pass'];

    // FIX: Added 'email' to the SELECT list
    $stmt = $conn->prepare("SELECT id, f_name, l_name, email, password, role FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        if (password_verify($pass, $row['password'])) {
            echo json_encode([
                "status" => "success",
                "message" => "Login successful",
                "user_id" => strval($row['id']),
                "f_name" => $row['f_name'],
                "l_name" => $row['l_name'],
                "email"  => $row['email'], // Now this will actually contain the email
                "role"   => $row['role']
            ]);
        } else {
            echo json_encode(["status" => "error", "message" => "Invalid credentials"]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "User not found"]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Invalid input"]);
}
$conn->close();
?>