<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <title>Hydrométéorologie Charlevoix</title>
  <!-- Leaflet -->
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.3.4/dist/leaflet.css" integrity="sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==" crossorigin=""/>
  <script src="https://unpkg.com/leaflet@1.3.4/dist/leaflet.js" integrity="sha512-nMMmRyTVoLYqjP9hrbed9S+FzjZHW5gY1TWCHA5ckwXZBadntCNs8kEqAWdrb9O7rxbCaA4lKTIWjDXZxflOcA==" crossorigin=""></script>
  <script src="js/leaflet-providers.js"></script>
  <script src="js/leaflet.ajax.min.js"></script>
  
  <!-- Fonts -->
  <link href="https://fonts.googleapis.com/css?family=Open+Sans|Roboto:300,400,700&display=swap" rel="stylesheet"> 
  
  <!-- Highcharts -->
  <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
  <script src="https://code.highcharts.com/highcharts.js"></script>
  <script src="https://code.highcharts.com/modules/data.js"></script>
  
  <!-- Hydrometeo -->
  <script src="js/timakan.js"></script>
  <link rel='stylesheet' media='screen and (min-width: 600px)' href='css/pc.css' />
  <link rel='stylesheet' media='only screen and (max-width: 600px)' href='css/mobile.css' />
</head>
<body>
<?php
require_once('private.php');
require_once('parsers.php');

$waterHtml = '';
$weatherHtml = '';
$cameraHtml = '';
try {
    $dbh = new PDO('pgsql:host=localhost;dbname=timakan', $USER, $PW);
    if($dbh) {
		// Water stations
		$waterSql = "SELECT name, serial FROM water_stations ORDER BY name";
		$waterStmt = $dbh->prepare($waterSql);
        if ($waterStmt->execute()) {
            while ($row = $waterStmt->fetch(PDO::FETCH_ASSOC)) {
				$waterHtml.="<a href='water/{$row['serial']}'>{$row['name']}</a>";
			}
        } else {
            error_log(print_r($dbh->errorInfo()), true);
        }
		
		// Cameras
		$cameraSql = "SELECT name, serial FROM camera_stations ORDER BY name";
		$cameraStmt = $dbh->prepare($cameraSql);
        if ($cameraStmt->execute()) {
            while ($row = $cameraStmt->fetch(PDO::FETCH_ASSOC)) {
				$cameraHtml.="<a href='surv_img/{$row['serial']}.jpeg'>{$row['name']}</a>";
			}
		
        } else {
            error_log(print_r($dbh->errorInfo()), true);
        }
		
		// Weather stations
		$weatherSql = "SELECT name, serial, publisher, url FROM weather_stations ORDER BY name";
		$weatherStmt = $dbh->prepare($weatherSql);
        if ($weatherStmt->execute()) {
            while ($row = $weatherStmt->fetch(PDO::FETCH_ASSOC)) {
				switch($row['publisher']) {
					case 'Info-Climat':
						$t = <<<EOT
<form action="http://www.cgfv.gouv.qc.ca/climat/donnees/OQMultiple.asp" method="post">
	<input type="text" style="display:none;" id="cle" name="cle" value="{$row['serial']}"></input>
    <input type="text" style="display:none;" id="type_graphique" name="type_graphique" value="Sommaire+climatologique"></input>
    <input type="text" style="display:none;" id="une_cle" name="une_cle" value="{$row['serial']}"></input>
    <button type="submit">{$row['name']}</button>
</form>
EOT;
						$weatherHtml.=$t;
						break;
					case "Environnement Canada":
						$weatherHtml .= "<a href='{$row['url']}'>{$row['name']}</a>";
						break;
					case "Weather Underground":
						$weatherHtml .= "<a href='https://www.wunderground.com/dashboard/pws/{$row['serial']}'>{$row['name']}</a>";
				}
			}
        } else {
            error_log(print_r($dbh->errorInfo()), true);
        }
    }
} catch(Exception $e) {
    error_log($e->getMessage());
}
?>
<header>
  <div class="overlay"></div>
  <div class="block full title">
    <h1>HYDROMÉTÉOROLOGIE<br>DE CHARLEVOIX</h1>
    <h2>Surveillance et suivi du climat</h2>
  </div>
  <div class="block full station dropdown">
    <h2>STATIONS HYDROLOGIQUES</h2>
	<div class="dropContent"><?= $waterHtml; ?></div>
  </div>
  <div class="block wrapper">
    <div class="block half station dropdown">
      <h2>STATIONS MÉTÉOROLOGIQUES</h2>
	<div class="dropContent"><?= $weatherHtml; ?></div>
    </div>
    <div class="block half camera dropdown">
      <h2>CAMÉRAS DE SURVEILLANCE</h2>
	<div class="dropContent"><?= $cameraHtml; ?></div>
    </div>
  </div>
</header>
<div id="map_section">
  <h2>STATIONS HYDROLOGIQUES ET NIVEAU DE L'EAU</h2>
  <div>
    <div id="map"></div>
  </div>
  <footer><p>La municipalité de Baie-Saint-Paul n’est en aucun cas responsable de l’utilisation que font les usagers de ce service et des données qui y sont présentées. Les informations contenues sur la carte interactive sont fournies à titre indicatif. La municipalité de Baie-Saint-Paul ne peut garantir l'exactitude, la précision ou l'exhaustivité des données. Par conséquent, La municipalité de Baie-Saint-Paul décline toute responsabilité pour toute imprécision, inexactitude ou omission portant sur les informations présentées et pour tout problème ou erreur qui peut en découler.</p></footer>
</div>
<div id="quake_section">
  <a href="http://www.seismescanada.rncan.gc.ca/recent/maps-cartes/index-fr.php?tpl_region=charlevoix" id="quake" class="block">
    <h2><img src="icons/quake.png" width=47 height=47 />ACTIVITÉ SISMIQUE</h2>
  </a>
  <div id="tide">
    <h2>TABLE DES MARÉES</h2>
    <div id="tide_graph"><div></div></div>
  </div>
</div>
<footer>
  <div><div class="dot"></div><div class="dot"></div><div class="dot"></div></div>
  <div class="overlay"></div>
</footer>
<script>
$(function() {
  var col20 = '#0000ff';
  var col100= '#ff0000';
  var name20='<div style="display: inline-block; margin: 0 0.5em; width: 1em; height: 1em; background-color:' + col20 + '"></div>0-20 ans';
  var name100='<div style="display: inline-block; margin: 0 0.5em; width: 1em; height: 1em; background-color:' + col100 + '"></div>0-100 ans';
  var nameHydro='<img src="icons/water_black.png" alt="" width=20 height=20 />Station hydrométrique';
  var nameWeather='<img src="icons/meteo_black.png" alt="" width=20 height=20 />Station météorologique';
  var nameCam='<img src="icons/camera_black.png" alt="" width=20 height=20 />Caméra de surveillance';
  var nameTide='<img src="icons/tide_black.png" alt="" width=20 height=20 />Station marémétrique';
  var vingt = new L.GeoJSON.AJAX("base/vingt.json", {
    style: {
      fillColor: col20,
      color: col20
    }
  });
  var cent = new L.GeoJSON.AJAX("base/cent.json", {
    style: {
      fillColor: col100,
      color: col100
    }
  });
  var map = L.map('map').setView([47.55, -70.50], 10);
  L.tileLayer.provider('Wikimedia').addTo(map);
  vingt.addTo(map);
  cent.addTo(map);

  function getWeatherLinkText(props) {
    return 'Voir la station météorologique ' + props.name + ' sur ' + props.publisher;
  }

  function getWeatherLink(props) {
    // console.log(props);
    switch(props.publisher) {
      case "Info-Climat":
        return getInfoClimatLink(props);
      case "Environnement Canada":
        return '<a href="' + props.url + '">' + getWeatherLinkText(props) + '</a>';
      case "Weather Underground":
        return '<a href="https://www.wunderground.com/dashboard/pws/' + props.serial + '">' + getWeatherLinkText(props) + '</a>';
      default:
        return 'Station météorologique ' + props.name;
    }
  }

  function getInfoClimatLink(props) {
    html = '<form action="http://www.cgfv.gouv.qc.ca/climat/donnees/OQMultiple.asp" method="post">'+
    '<input type="text" style="display:none;" id="cle" name="cle" value="' + props.serial + '"></input>'+
    '<input type="text" style="display:none;" id="type_graphique" name="type_graphique" value="Sommaire+climatologique"></input>'+
    '<input type="text" style="display:none;" id="une_cle" name="une_cle" value="' + props.serial + '"></input>'+
    '<button type="submit">' + getWeatherLinkText(props) + '</button>'+
    '</form>';
    return html;
  }

  var meteoIcon = L.icon({
    iconUrl: 'icons/meteo_black.png',
    iconSize:     [36, 36], // size of the icon
    iconAnchor:   [18, 18], // point of the icon which will correspond to marker's location
    popupAnchor:  [0,   0] // point from which the popup should open relative to the iconAnchor
  });
  var cameraIcon = L.icon({
    iconUrl: 'icons/camera_black.png',
    iconSize:     [36, 36], // size of the icon
    iconAnchor:   [18, 18], // point of the icon which will correspond to marker's location
    popupAnchor:  [0,   0] // point from which the popup should open relative to the iconAnchor
  });
  var waterIcon = L.icon({
    iconUrl: 'icons/water_black.png',
    iconSize:     [36, 36], // size of the icon
    iconAnchor:   [18, 18], // point of the icon which will correspond to marker's location
    popupAnchor:  [0,   0] // point from which the popup should open relative to the iconAnchor
  });
  var tideIcon = L.icon({
    iconUrl: 'icons/tide_black.png',
    iconSize:     [36, 36], // size of the icon
    iconAnchor:   [18, 18], // point of the icon which will correspond to marker's location
    popupAnchor:  [0,   0] // point from which the popup should open relative to the iconAnchor
  });

  // Water level stations
  var waterLayer = new L.GeoJSON.AJAX("base/water", {
    pointToLayer: function (feature, latlng) {
      return L.marker(latlng, {
        icon: waterIcon
      });
    },
    onEachFeature: function onEachFeature(feature, layer) {
      if (feature && feature.properties && feature.properties.name && feature.properties.serial) {
        var s = '<a href="water/' + feature.properties.serial + '">Station de mesure du niveau d\'eau ' + feature.properties.name + '</a>';
        layer.bindPopup(s);
      }
    }
  });
  waterLayer.addTo(map);

  // Weather stations
  var meteoLayer = new L.GeoJSON.AJAX("base/weather", {
    pointToLayer: function (feature, latlng) {
      return L.marker(latlng, {
        icon: meteoIcon
      });
    },
    onEachFeature: function onEachFeature(feature, layer) {
      if (feature && feature.properties && feature.properties.name && feature.properties.publisher) {
        layer.bindPopup(getWeatherLink(feature.properties));
      }
    }
  });
  meteoLayer.addTo(map);

  // Camera stations
  var cameraLayer = new L.GeoJSON.AJAX("base/camera", {
    pointToLayer: function (feature, latlng) {
      return L.marker(latlng, {
        icon: cameraIcon
      });
    },
    onEachFeature: function onEachFeature(feature, layer) {
      if (feature && feature.properties && feature.properties.ip) {
        layer.bindPopup('<img src="/surv_img/' + feature.properties.ip + '.jpeg" alt="" width=800 height=450 />');
      }
    }
  });
  cameraLayer.addTo(map);

  // Tide stations
  var tideLayer = new L.GeoJSON.AJAX("base/tide", {
    pointToLayer: function (feature, latlng) {
      return L.marker(latlng, {
        icon: tideIcon
      });
    },
    onEachFeature: function onEachFeature(feature, layer) {
      if (feature && feature.properties && feature.properties.name) {
        if(feature.properties.url)
          layer.bindPopup('<a href="' + feature.properties.url + '">Station marégraphique ' + feature.properties.name + ' sur MaréesPêches</a>');
        else
          layer.bindPopup('Station marégraphique ' + feature.properties.name);
      }
    }
  });
  tideLayer.addTo(map);
  
  overlays={};
  overlays[name20] = vingt;
  overlays[name100] = cent;
  overlays[nameHydro] = waterLayer;
  overlays[nameWeather] = meteoLayer;
  overlays[nameCam] = cameraLayer;
  overlays[nameTide] = tideLayer;
  L.control.layers({}, overlays, {collapsed: false}).addTo(map);

  // Tide chart
  Highcharts.setOptions({
    chart: {
      style: {
        fontFamily: 'Open Sans'
      }
    }
  });
  Highcharts.chart('tide_graph', {
    chart: {
      type: 'spline',
      backgroundColor: 'transparent'
    },
    title: {
      text: 'Marées à Cap-aux-Corbeaux',
      style: { color: "#ffffff" }
    },
    legend: {
      enabled: false
    },
    xAxis: {
      labels: {
        formatter: function() {
          return this.value.toString().padStart(2, '0')+":00";
        },
        style: { color: "#ffffff" }
      }
    },
    yAxis: {
      title: {
        text:"Niveau prédit (m)",
        style: { color: "#ffffff" }
      },
      labels: {
        style: { color: "#ffffff" }
      }
    },
    tooltip: {
      formatter: function() { return this.y; }
    },
    series: [{
      name: "Marée",
      data: <?php echo parseTides("http://www.tides.gc.ca/fra/station?sid=3060"); ?>
    }]
  });
});
</script>
</body>
</html>