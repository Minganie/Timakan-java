<!DOCTYPE html>
<html lang="fr">
  <head>
    <meta charset="utf-8">
    <title>Hydrométéorologie de Charlevoix</title>
    <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
    <script src="https://code.highcharts.com/highcharts.js"></script>
    <script src="https://code.highcharts.com/modules/data.js"></script>
    <link rel="stylesheet" href="../css/water_station.css">
  </head>
<?php
require_once("private.php");

if(isset($_GET['station']) && !empty($_GET['station'])) {
    $serial = $_GET['station'];
    $info = array();
    try {
        $dbh = new PDO('pgsql:host=localhost;dbname=timakan', $USER, $PW);
        if($dbh) {
            $info_sql = "SELECT * FROM water_stations WHERE gid=$serial";
            $stmt = $dbh->prepare($info_sql);
            $i = 0;
            if ($stmt->execute()) {
                while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                    $info = $row;
                    ++$i;
                }
            }
            if($i == 0 || $i > 1)
                throw new Exception('Wrong number of lines returned by database.');
        }
    } catch(Exception $e) {
        $info = array();
        error_log($e->getMessage());
    }
}
?>
  <body>
  <h1><?php echo $info['name']; ?></h1>
  <div id="graph_week" class="timakan_chart"></div>
  <div id="graph_year" class="timakan_chart"></div>
  <div id="graph_all" class="timakan_chart"></div>
  <script>
  <?php 
    $mon = ((isset($info['monitoring']) && ! empty($info['monitoring'])) ? $info['monitoring'] : null);
    $ovf = ((isset($info['overflow']) && ! empty($info['overflow'])) ? $info['overflow'] : null);
    $his = ((isset($info['historical']) && ! empty($info['historical'])) ? $info['historical'] : null);
    $his_ref = ((isset($info['hist_ref']) && ! empty($info['hist_ref'])) ? '"'.$info['hist_ref'].'"' : null);
    $plotlines = '[';
    $max = 0;
    if($mon != null) {
        $plotlines .= "{
            value: $mon,
            color: 'green',
            dashStyle: 'shortdash',
            width: 2,
            label: {
                text: 'Surveillance'
            }
        },";
        $max = max($max, $mon);
    }
    if($ovf != null) {
        $plotlines .= "{
            value: $ovf,
            color: 'orange',
            dashStyle: 'shortdash',
            width: 2,
            label: {
                text: 'Débordement'
            }
        },";
        $max = max($max, $ovf);
    }
    if($his != null) {
        $plotlines .= "{
            value: $his,
            color: 'red',
            dashStyle: 'shortdash',
            width: 2,
            label: {
                text: 'Historique'
            }
        },";
        $max = max($max, $his);
    }
    $plotlines .= ']';
    echo "var horizs = $plotlines;\n";
    // echo "var maxy = ". ($max > 0 ? $max : 'null') .";\n";
    echo "var maxy=null;\n";
  ?>
  function parseDateTime(dateTimeString) {
      return Date.parse(dateTimeString);
  }
    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });
  Highcharts.chart('graph_all', {
    data: {
        csvURL: window.location.origin + '/csv/<?php echo $info['gid'] ?>_all.csv',
        parseDate: parseDateTime
    },
    title: {
        text: 'Données historiques'
    },
    legend: {
        enabled: false
    },
    xAxis: {
        type: 'datetime',
        minTickInterval: 1000*3600
    },
    yAxis: {
        title: {text:"Niveau d'eau"},
        max: maxy,
        plotLines: horizs
    },
    series: [{
        name: 'Toutes',
        lineWidth: 1,
        marker: {
            radius: 4
        }
    }]
  });
  Highcharts.chart('graph_year', {
    data: {
        csvURL: window.location.origin + '/csv/<?php echo $info['gid'] ?>_year.csv',
        parseDate: parseDateTime,
        firstRowAsNames: false,
        startRow:1
    },
    title: {
        text: 'Dernière année'
    },
    xAxis: {
        type: 'datetime'
    },
    yAxis: {
        title: {text:"Niveau d'eau"},
        max: maxy,
        plotLines: horizs
    },
    legend: {
        layout: 'vertical',
        align: 'right',
        verticalAlign: 'middle',
        reversed: true
    },
    series: [{
        name: 'Minimum',
        marker: {
            enabled: false
        },
        color: '#dddddd'
    },{
        name: 'q10',
        marker: {
            enabled: false
        },
        color: '#00ff00'
    },{
        name: 'Médiane',
        marker: {
            enabled: false
        },
        color: '#0000ff'
    },{
        name: 'q90',
        marker: {
            enabled: false
        },
        color: '#ff0000'
    },{
        name: 'Maximum',
        marker: {
            enabled: false
        },
        color: '#dddddd'
    },{
        name: 'Niveau courant',
        lineWidth: 3,
        color: '#000000'
    }]
  });
  Highcharts.chart('graph_week', {
    data: {
        csvURL: window.location.origin + '/csv/<?php echo $info['gid'] ?>_week.csv',
        parseDate: parseDateTime,
        firstRowAsNames: false,
        startRow:1
    },
    title: {
        text: 'Deux dernières semaines'
    },
    xAxis: {
        type: 'datetime'
    },
    yAxis: {
        title: {text:"Niveau d'eau"},
        max: maxy,
        plotLines: horizs
    },
    legend: {
        layout: 'vertical',
        align: 'right',
        verticalAlign: 'middle',
        reversed: true
    },
    series: [{
        name: 'Minimum',
        marker: {
            enabled: false
        },
        color: '#dddddd'
    },{
        name: 'q10',
        marker: {
            enabled: false
        },
        color: '#00ff00'
    },{
        name: 'Médiane',
        marker: {
            enabled: false
        },
        color: '#0000ff'
    },{
        name: 'q90',
        marker: {
            enabled: false
        },
        color: '#ff0000'
    },{
        name: 'Maximum',
        marker: {
            enabled: false
        },
        color: '#dddddd'
    },{
        name: 'Niveau courant',
        lineWidth: 3,
        color: '#000000'
    }]
  });
  </script>
  </body>
</html>
