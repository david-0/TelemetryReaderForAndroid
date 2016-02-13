angular.module('telemetryReaderForAndroid.services')

  .service('chartDefinitionsService', [function () {
      this.getChartDefinitions = function (useMetric) {
        var _timeTickFormatter = function (x) {
          var durationObj = moment.duration(x * 10);
          var durationStr = '';

          if (durationObj.hours() > 0) durationStr = durationObj.hours() + ':';

          durationStr += durationObj.minutes() > 0 ? (durationObj.minutes() + ':') : '0:';
          durationStr += durationObj.seconds() > 0 ? (durationObj.seconds() + ':') : '00.';
          durationStr += durationObj.milliseconds() > 0 ? (durationObj.milliseconds()) : '0';
          return durationStr;
        };

        return {
                 "altitude": {
                   "basic": {
                     "zoomEnabled": true,
                     "animationEnabled": true,
                     "title": {
                       "text": "Altitude"
                     },
                     "axisX": {
                       "title": "Time",
                       "labelFormatter": function (e) {
                         return _timeTickFormatter(e.value);
                       }
                     },
                     "axisY": {
                       "title": "Altitude (meters)",
                       "labelFormatter": function (e) {
                         return (e.value / 10) + "m"
                       }
                     },
                     "toolTip": {
                       "contentFormatter": function (e) {
                         var dataPoint = e.entries[0].dataPoint;
                         return (dataPoint.y / 10) + "m (" + _timeTickFormatter(dataPoint.x) + ")";
                       }
                     }
                   },
                   "chartSeriesTypes": [
                     {
                       "data": [
                         {
                           "name": "Altitude",
                           "type": "line",
                         }
                       ]
                     }
                   ]
                 },
                 "current": {
                   "basic": {
                     "zoomEnabled": true,
                     "animationEnabled": true,
                     "title": {
                       "text": "Current"
                     },
                     "axisX": {
                       "title": "Time",
                       "labelFormatter": function (e) {
                         return _timeTickFormatter(e.value);
                       }
                     },
                     "axisY": {
                       "title": "Current (mA)",
                       "labelFormatter": function (e) {
                         return (e.value / 10) + "A"
                       }
                     },
                     "toolTip": {
                       "contentFormatter": function (e) {
                         var dataPoint = e.entries[0].dataPoint;
                         return (dataPoint.y / 10) + "A (" + _timeTickFormatter(dataPoint.x) + ")";
                       }
                     }
                   },
                   "chartSeriesTypes": [
                     {
                       "data": [
                         {
                           "name": "Current",
                           "type": "line",
                         }
                       ]
                     }
                   ]
                 },
                 "gforce": {
                   "chartSeriesTypes": [
                     {
                       "data": [
                         {
                           "name": "X",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Y",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Z",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Max X",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Max Y",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Max Z",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "data": [
                         {
                           "name": "Min Z",
                           "type": "line"
                         }
                       ]
                     }
                   ]
                 },
                 "powerbox": {
                   "basic": {
                     "zoomEnabled": true,
                     "animationEnabled": true,
                     "title": {
                       "text": "PowerBox"
                     },
                     "legend": {
                       "horizontalAlign": "center", // "center" , "right"
                       "verticalAlign": "bottom", // "top" , "bottom"
                       //            "fontSize": 15
                     },
                     "axisX": {
                       "title": "Time",
                       "labelFormatter": function (e) {
                         return _timeTickFormatter(e.value);
                       }
                     }
                   },
                   "chartSeriesTypes": [
                     {
                       "selected": true,
                       "axis": {
                         "title": "Volts"
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return entry.dataPoint.y + " volts (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "Voltage One",
                           "type": "line"
                         },
                         {
                           "showInLegend": true,
                           "name": "Voltage Two",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "selected": true,
                       "axis": {
                         "title": "Capacity (mAh)",
                         "labelFormatter": function (e) {
                           return (e.value) + "mAh"
                         }
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return entry.dataPoint.y + "mAh (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "Capacity One",
                           "type": "line"
                         },
                         {
                           "showInLegend": true,
                           "name": "Capacity Two",
                           "type": "line"
                         }
                       ]
                     }
                   ]
                 },
                 "rx": {
                   "basic": {
                     "animationEnabled": true,
                     "zoomEnabled": true,
                     "title": {
                       "text": "RX"
                     },
                     "legend": {
                       "horizontalAlign": "center", // "center" , "right"
                       "verticalAlign": "bottom", // "top" , "bottom"
                       //            "fontSize": 15
                     },
                     "axisX": {
                       "title": "Time",
                       "labelFormatter": function (e) {
                         return _timeTickFormatter(e.value);
                       }
                     }
                   },
                   chartSeriesTypes: [
                     {
                       "selected": true,
                       "axis": {
                         "title": "Signal"
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           return entry.dataPoint.y
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "A",
                           "type": "line"
                       },
                         {
                           "showInLegend": true,
                           "name": "B",
                           "type": "line"
                       },
                         {
                           "showInLegend": true,
                           "name": "L",
                           "type": "line"
                       },
                         {
                           "showInLegend": true,
                           "name": "R",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "selected": true,
                       "axis": {
                         "title": "Fades and Holds",
                         "minimum": 0
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "Frame Loss",
                           "axisYType": "secondary",
                           "type": "line"
                         },
                         {
                           "showInLegend": true,
                           "name": "Holds",
                           "axisYType": "secondary",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "selected": false,
                       "axis": {
                         "title": "Volts",
                         "labelFormatter": function (e) {
                           return e.value / 100;
                         },
                         "minimum": 0
                       },
                       "toolTip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return (entry.dataPoint.y / 100) + " volts (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "RX Voltage",
                           "type": "line"
                         }
                       ]
                     }
                   ]
                 },
                 "standard": {
                   "basic": {
                     "animationEnabled": true,
                     "zoomEnabled": true,
                     "title": {
                       "text": "Standard"
                     },
                     "legend": {
                       "horizontalAlign": "center", // "center" , "right"
                       "verticalAlign": "bottom", // "top" , "bottom"
                       //            "fontSize": 15
                     },
                     "axisX": {
                       "title": "Time",
                       "labelFormatter": function (e) {
                         return _timeTickFormatter(e.value);
                       }
                     }
                   },
                   "chartSeriesTypes": [
                     {
                       "selected": true,
                       "axis": {
                         "title": "RPM",
                         "labelFormatter": function (e) {
                           return e.value;
                         },
                         "minimum": 0
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return entry.dataPoint.y + " RPM (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "RPM",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "selected": true,
                       "axis": {
                         "title": "Temperature in °F",
                         "labelFormatter": function (e) {
                           return e.value;
                         },
                         "minimum": 23
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return entry.dataPoint.y + " degrees (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "Temperature",
                           "type": "line"
                         }
                       ]
                     },
                     {
                       "selected": false,
                       "axis": {
                         "title": "Volts",
                         "labelFormatter": function (e) {
                           return e.value / 100;
                         },
                         "minimum": 0
                       },
                       "tooltip": {
                         "contentFormatter": function (e) {
                           var entry = e.entries[0];
                           return (entry.dataPoint.y / 100 )+ " volts (" + _timeTickFormatter(entry.dataPoint.x) + ")";
                         }
                       },
                       "data": [
                         {
                           "showInLegend": true,
                           "name": "Voltage",
                           "type": "line"
                         }
                       ]
                     }
                   ]
                 },
                 "vario": {

                 }
               };
    }
  }]);
