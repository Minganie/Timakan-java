<?php
require_once("../private.php");

$result = json_encode(array());
if(isset($_GET['stations']) && !empty($_GET['stations'])) {
    $type = $_GET['stations'];
    try {
        switch ($type) {
            case "water":
                $table = "water_stations";
                $props = "'serial', serial, 'name', name";
                break;
            case "weather":
                $table = "weather_stations";
                $props = "'serial', serial, 'name', name, 'altitude', altitude, 'publisher', publisher, 'url', url";
                break;
            case "camera":
                $table = "camera_stations";
                $props = "'serial', serial, 'name', name, 'ip', ip";
                break;
            case "tide":
                $table = "tide_stations";
                $props = "'serial', serial, 'name', name, 'url', url";
                break;
            default:
               throw new Exception("Can't figure out which type of station '$type' is supposed to be.");
        }
        $dbh = new PDO('pgsql:host=localhost;dbname=timakan', $USER, $PW);
        if($dbh) {
            $sql = 
            "SELECT row_to_json(fc) AS geojson 
            FROM 
            (SELECT 'FeatureCollection' As type, 
                array_to_json(array_agg(f)) As features 
            FROM 
                (SELECT 'Feature' As type, 
                    ST_AsGeoJSON((lg.geom),15,0)::json As geometry,
                    json_build_object($props) As properties
                FROM $table As lg) As f ) As fc";

            $stmt = $dbh->prepare($sql);
            $i = 0;
            if ($stmt->execute()) {
                while ($row = $stmt->fetch()) {
                    $result = $row[0];
                    ++$i;
                }
            }
            if($i > 1)
                throw new Exception('More than one line returned by database.');
        } else {
            error_log("Connection failed");
            error_log($e->getMessage());
        }
    } catch(Exception $e) {
        $result = json_encode(array());
        error_log($e->getMessage());
    }
}
echo $result;
?>