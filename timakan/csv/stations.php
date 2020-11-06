<?php
require_once('../private.php');
$stations = array();
try {
    $dbh = new PDO('pgsql:host=localhost;dbname=timakan', $USER, $PW);
    if($dbh) {
        $sql = "
        SELECT ws.gid, 
            ws.name, 
            st_astext(ws.geom) as wkt, 
            st_y(ws.geom) as lat, 
            st_x(ws.geom) as long, 
            srs.auth_name || ':' || st_srid(ws.geom) as srs_code, 
            substring(srs.srtext from '^.+?\[\"(.+?)\".+$') as srs_name
        FROM water_stations as ws
            JOIN spatial_ref_sys AS srs ON st_srid(ws.geom)=srs.srid
        ";
        $stmt = $dbh->prepare($sql);
        if ($stmt->execute()) {
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $stations[] = $row;
            }
        } else {
            error_log(print_r($dbh->errorInfo()), true);
        }
    }
} catch(Exception $e) {
    $stations = array();
    error_log($e->getMessage());
}
echo json_encode($stations);
?>