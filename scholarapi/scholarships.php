<?php
require 'db_connect.php';
header('Content-Type: application/json');

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

// Added s.latitude and s.longitude to the SELECT statement
$sql = "SELECT 
            s.scholarship_id, 
            s.scholarship_name, 
            s.provider_name, 
            s.deadline_date, 
            s.scholarship_notes,
            s.latitude,
            s.longitude,
            COALESCE(a.application_status, 'SUBMITTED') as application_status,
            CASE WHEN a.app_id IS NOT NULL THEN 1 ELSE 0 END as is_applied
        FROM scholarships s
        LEFT JOIN applications a ON s.scholarship_id = a.scholarship_id AND a.user_id = $user_id";

$result = $conn->query($sql);

$scholarships = array();
if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $row['scholarship_id'] = strval($row['scholarship_id']);
        
        // Ensure lat/long are actually doubles (float) in the JSON
        $row['latitude'] = floatval($row['latitude']);
        $row['longitude'] = floatval($row['longitude']);

        if ($row['is_applied'] == 0) {
             $row['application_status'] = "SUBMITTED"; 
        }
        $scholarships[] = $row;
    }
}

echo json_encode($scholarships);
$conn->close();
?>