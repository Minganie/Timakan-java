<?php
libxml_use_internal_errors(true);

function parseTides($url) {
    $dom = new DOMDocument;
    $html = file_get_contents($url);
    $dom->loadHTML($html);
    $xpath = new DOMXpath($dom);
    
    $parent = "/html/body/div[1]/div[3]/div/div/div/div/div[3]/div[3]/table/tbody/tr[1]";
    $tds = $xpath->query($parent."/td");
    $jsonwannabe = array();
    if($tds->length != 24) {
        error_log("Error while parsing tides for url '$url': found {$tds->length} hours");
    } else {
        $i = 0;
        foreach($tds as $td) {
            $jsonwannabe[] = array($i++, floatval($td->textContent));
        }
    }
    return json_encode($jsonwannabe);
}

function parseEnvCan($url) {
    $dom = new DOMDocument;
    $html = file_get_contents($url);
    $dom->loadHTML($html);
    $xpath = new DOMXpath($dom);
    
    $parent = "";
    $tds = $xpath->query($parent."/td");
    $jsonwannabe = array();
    // if($tds->length != 24) {
        // error_log("Error while parsing tides for url '$url': found {$tds->length} hours");
    // } else {
        // $i = 0;
        // foreach($tds as $td) {
            // $jsonwannabe[] = array($i++, floatval($td->textContent));
        // }
    // }
    return json_encode($jsonwannabe);
}

function parseWU($url) {
    $dom = new DOMDocument;
    $html = file_get_contents($url);
    $dom->loadHTML($html);
    $xpath = new DOMXpath($dom);
    
    $parent = "";
    $tds = $xpath->query($parent."/td");
    $jsonwannabe = array();
    // if($tds->length != 24) {
        // error_log("Error while parsing tides for url '$url': found {$tds->length} hours");
    // } else {
        // $i = 0;
        // foreach($tds as $td) {
            // $jsonwannabe[] = array($i++, floatval($td->textContent));
        // }
    // }
    return json_encode($jsonwannabe);
}
?>