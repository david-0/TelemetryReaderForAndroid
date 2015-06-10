angular.module('telemetryReaderForAndroid.controllers', ['telemetryReaderForAndroid.services'])

  .controller('AppCtrl', ['$scope', '$ionicModal', '$ionicLoading', '$timeout', '$window', '$state', 'dataService',
                          function ($scope, $ionicModal, $ionicLoading, $timeout, $window, $state, dataService) {
      $scope.doGetDataFile = function () {
        $ionicLoading.show();
        
        console.log('get data file');

        dataService.loadData(true).then(function (data) {
          dataService.file = data;
          if (dataService.file && dataService.file.flights && dataService.file.flights.length > 0) {
            dataService.setSelectedFlight(dataService.file.flights[0]).then(function () {
              console.log('here');
              window.location = "#/app/fileInfo";
            });
          } else {
            dataService.selectedFlight = null;
            window.location = "#/app/fileInfo";
          }
        });
      };

      $scope.setTelemetryType = function (key, title) {
        dataService.selectedKey = key;
        dataService.selectedTitle = title;
        $window.title = title;
      }
}])
  .controller('TelemetryViewerController', ['$scope', '$window', '$ionicLoading', '$ionicScrollDelegate', 'filterFilter', 'dataService',
                                     function ($scope, $window, $ionicLoading, $ionicScrollDelegate, filterFilter, dataService) {
      $scope.chart = null;
      $scope.chartData = null;
      $scope.service = dataService;
      $scope.chartDataOptions = null;

      $scope.$on('$ionicView.enter', function () {
        if (!$scope.service.selectedKey)
          $scope.service.selectedKey = 'current';

        if (!$scope.service.flights) {
          $ionicLoading.show();
          dataService.getCurrentData().then(function (file) {
            if (file && file.flights && file.flights.length > 0) {
              $scope.service.setSelectedFlight(file.flights[0]).then(function() {
                $scope.selectedFlightChanged();
              });
            } else {
              $ionicLoading.hide();
            }
          }, function (error) {
            $ionicLoading.hide();
            window.location = '#/app/welcome';
          });
        } else {
          $scope.chart = null;

          $scope.selectedFlightChanged();
        }
      });

      var doSetupChart = function (canvasJSChartOptions, chartSeriesTypes) {
        if (chartSeriesTypes[0].axis) {
          canvasJSChartOptions['axisY'] = chartSeriesTypes[0].axis;
        }
        if (chartSeriesTypes[0].tooltip) {
          canvasJSChartOptions['toolTip'] = chartSeriesTypes[0].tooltip;
        }
        canvasJSChartOptions['data'] = chartSeriesTypes[0].data;
        _.forEach(canvasJSChartOptions['data'], function (dataSet) {
          dataSet['axisYType'] = 'primary';
        })

        console.log('canvasJSChartOptions - 0', canvasJSChartOptions);

        if (chartSeriesTypes.length == 2) {
          canvasJSChartOptions['axisY2'] = chartSeriesTypes[1].axis;
          if (canvasJSChartOptions['toolTip'] &&
            chartSeriesTypes[1].tooltip &&
            chartSeriesTypes[1].tooltip.contentFormatter) {
            canvasJSChartOptions['toolTip'] = {
              "contentFormatter": function (e) {
                if (e.entries[0].dataSeries.axisYType === 'secondary') {
                  return chartSeriesTypes[1].tooltip.contentFormatter(e);
                } else {
                  return chartSeriesTypes[0].tooltip.contentFormatter(e);
                }
              }
            }
          } else if (!canvasJSChartOptions['toolTip'] &&
            chartSeriesTypes[1].tooltip &&
            chartSeriesTypes[1].tooltip.contentFormatter) {
            canvasJSChartOptions['toolTip'] = chartSeriesTypes[1].tooltip;
          }
          _.forEach(chartSeriesTypes[1].data, function (dataSet) {
            console.log('data set series 2', dataSet);
            dataSet['axisYType'] = 'secondary';
            canvasJSChartOptions['data'].push(dataSet);
          });
        }
      }

      $scope.selectedFlightChanged = function () {
        $ionicLoading.show();

        if (!$scope.service.selectedFlight) {
          console.warn('no selected flight');
          $ionicLoading.hide();
          return;
        }

        console.log('selectedKey', $scope.service.selectedKey);
        $scope.chartDataOptions = $scope.service.selectedFlight.flightData[$scope.service.selectedKey];
        console.log('chartOptions', $scope.chartDataOptions);
        if (!$scope.chartDataOptions) {
          return;
        }

        var canvasJSChartOptions = _.cloneDeep($scope.chartDataOptions.basic);

        if ($scope.chartDataOptions.chartSeriesTypes.length < 3) {
          doSetupChart(canvasJSChartOptions, $scope.chartDataOptions.chartSeriesTypes);
        } else {
          var chartSeriesTypes = [];
          _.forEach($scope.chartDataOptions.chartSeriesTypes, function (series) {
            if (!series.selected) return;

            chartSeriesTypes.push(series);
          });
          doSetupChart(canvasJSChartOptions, chartSeriesTypes);
        }

        console.log("CanvasJSChartOptions - ready to go!", canvasJSChartOptions);

        $scope.chart = null;
        try {
          $scope.chart = new CanvasJS.Chart("myChart", canvasJSChartOptions);
          $scope.chart.render();
        } catch (e) {
          console.log("Error when trying to render the chart!", e);
        }

        $ionicLoading.hide();
      };

      $scope.selectChanged = function (flight) {
        $ionicLoading.show();

        if ($scope.service.selectedFlight['_cached'] === undefined
        || !$scope.service.selectedFlight['_cached']) {
          window.setTimeout(function () {
            $scope.service.setSelectedFlight($scope.service.selectedFlight).then(function(decodedFlight) {
              $scope.selectedFlightChanged();
             });
          }, 100);
        } else {
          $scope.selectedFlightChanged();
        }
      };

      $scope.seriesClicked = function (series) {

        var selected = _.where($scope.chartDataOptions.chartSeriesTypes, {
          "selected": true
        });

        if (selected.length > 2) series.selected = false;
        else if (selected.length == 0) series.selected = true;

        $scope.selectedFlightChanged();
      }

      $scope.$watch('service.selectedKey', function () {
        $scope.selectedFlightChanged();
      });
}])
  .controller('FileInfoController', ['$scope', '$ionicLoading', 'dataService', function($scope, $ionicLoading, dataService) {
    $scope.service = dataService;
    
    $scope.$on('$ionicView.enter', function () {
      $ionicLoading.hide();
    });
}]);
