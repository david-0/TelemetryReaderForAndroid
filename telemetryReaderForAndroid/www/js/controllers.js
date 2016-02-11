angular.module('telemetryReaderForAndroid.controllers', ['telemetryReaderForAndroid.services'])
  .controller('AppCtrl', ['$scope', '$ionicModal', '$ionicLoading', '$timeout', '$window', '$log', 'dataService',
                          function ($scope, $ionicModal, $ionicLoading, $timeout, $window, $log, dataService) {
      /**
       * A scope level reference to the data service
       */
      $scope.service = dataService;
      /**
       * A scope level reference to the selected flight.
       */
      $scope.selectedFlight = dataService.selectedFlight;

      /**
       * Success handler for setting the selected flight in the service. Called when the
       * flight is successfully fully populated with JSON telmetry data.
       * @param {Object} selectedFlight Fully populated flight JSON object.
       */
      var selectedFlightSuccessHandler = function (selectedFlight) {
		  $ionicLoading.hide();
		  
          if (selectedFlight) $log.debug('A flight object was handed back by the promise.');
          $scope.selectedFlight = selectedFlight;
        },
        /**
         * Error handler for setting the selected flight. Called when the promise is rejected.
         * @param {Object} error Exception from the promise being rejected.
         */
        selectedFlightErrorHandler = function (error) {
		  $ionicLoading.hide();
          $log.error('Error during the selected flight handler.', error);
        };

      /**
       * Used by the view to handle the user interaction for loading a new data file.
       */
      $scope.doGetDataFile = function () {
        $log.debug('Going to get a data file!');
		
		$ionicLoading.show();
        
        $scope.service.loadData(true).then(function (data) {
          // ignoring data since we loaded data and set it to the current. data === $scope.service.file
          if ($scope.service.file.flights && $scope.service.file.flights.length > 0) {
            $scope.service.setSelectedFlight($scope.service.file.flights[0])
              .then(selectedFlightSuccessHandler, selectedFlightErrorHandler);
          } else {
            $scope.service.setSelectedFlight(null)
              .then(selectedFlightSuccessHandler, selectedFlightErrorHandler);
          }
        });
      };

      /**
       * Used by the view to handle the user's selection of a type of telemetry values to view.
       * @param {String} key   The key value in the flight's view model data hash for the telemetry data
       * @param {String} title The title to display for the page.
       */
      $scope.setTelemetryType = function (key, title) {
        $scope.service.selectedKey = key;
        $scope.service.selectedTitle = title;
        $window.title = title;
      }
}])
  .controller('TelemetryViewerController', ['$scope', '$window', '$log', '$ionicLoading', '$ionicScrollDelegate', 'filterFilter', 'dataService',
                                     function ($scope, $window, $log, $ionicLoading, $ionicScrollDelegate, filterFilter, dataService) {
      $scope.chart = null;
      $scope.chartData = null;
      $scope.service = dataService;
      $scope.chartDataOptions = null;

      /**
       * This handler is for the ionic's view enter event. The handler checks for the selected flight. If
       * no flight is loaded the user is prompted to load a flight otherwise the view is updated through
       * the selectedFlightChanged scope method.
       */
      $scope.$on('$ionicView.enter', function () {
        if (!$scope.service.selectedKey)
          $scope.service.selectedKey = 'current';

        if (!$scope.service.file) {
          $ionicLoading.show();
          dataService.getCurrentData().then(function (file) {
            if (file && file.flights && file.flights.length > 0) {
              $scope.service.setSelectedFlight(file.flights[0]).then(function () {
                $scope.selectedFlightChanged();
              });
            } else {
              $ionicLoading.hide();
            }
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
        canvasJSChartOptions['data'] = [];
        _.forEach(chartSeriesTypes[0].data, function (dataSet) {
          console.log('data set series 1', dataSet);
          dataSet['axisYType'] = 'primary';
          canvasJSChartOptions['data'].push(dataSet);
        });

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

      /**
       * Handles the selected flight being changed. This scope level method is invokded from the view
       * whenever the dropdown changes. If no flight was loaded when initially loading the page this
       * function will be called after a data file is loaded and the first flight in the file is set
       * to the selected flight.
       */
      $scope.selectedFlightChanged = function () {
        $ionicLoading.show();

        if (!$scope.service.selectedFlight) {
          $log.warn('Cannot handle the selected flight changing when there is no selected flight!');
          $ionicLoading.hide();
          return;
        }

        // using the selectedKey from the service (set in the app controller) get the chart data options for the flight.
        $log.debug('The selected view key (selectedKey):', $scope.service.selectedKey);
        $scope.chartDataOptions = $scope.service.selectedFlight.flightData[$scope.service.selectedKey];
        $log.debug('The chart\'s data options for the selected key.', $scope.chartDataOptions);

        if (!$scope.chartDataOptions) {
          $log.error('This is embarassing! The selected key doesn\'t have any chart data options defined. Without the chartDataOptions processing cannot continue.');
          return;
        }

        // clone the basic chart options - TODO: explain why I do this!
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

        $log.debug("Finished creating the complete CanvasJS chart data structure (canvasJSChartOptions):", canvasJSChartOptions);

        $scope.chart = null;

        try {
          $scope.chart = new CanvasJS.Chart("myChart", canvasJSChartOptions);
          $scope.chart.render();
        } catch (e) {
          $log.error("Error when trying to render the chart!", e);
        }

        // the processing is complete for when a selected flight is changed. Hide the loading/busy indicator.
        $ionicLoading.hide();
      };

      /**
       * Handler for the view's select input. Looks to see if the newly selected flight is cached,
       * previously fully filled out JSON, or if it is just a stub placeholder.
       * @param {Object} selectedFlight The selected flight. Will be the same object as $scope.service.selectedFlight.
       */
      $scope.selectChanged = function (selectedFlight) {
        $ionicLoading.show();

        if ($scope.service.selectedFlight['_cached'] === undefined || !$scope.service.selectedFlight['_cached']) {
          window.setTimeout(function () {
            $scope.service.setSelectedFlight($scope.service.selectedFlight).then(function (decodedFlight) {
              $scope.selectedFlightChanged();
            });
          }, 100);
        } else {
          $scope.selectedFlightChanged();
        }
      };

      /**
       * Click handler for the series checkboxes. These checkboxes are only shown when there are 3 or more
       * data series which have different scales for the Y axis.
       * @param {Object} series The chart series
       */
      $scope.seriesClicked = function (series) {

        var selected = _.where($scope.chartDataOptions.chartSeriesTypes, {
          "selected": true
        });

        if (selected.length > 2) series.selected = false;
        else if (selected.length == 0) series.selected = true;

        $scope.selectedFlightChanged();
      }

      $scope.shareChart = function () {
        var filename = 'telemetry_' + $scope.service.selectedKey + '_'
            $scope.service.selectedFlight.name.replace(' ', '').replace('.', '') + '.png';        
        $ionicLoading.show();
        
        if (window.com && window.com.monstarmike && window.com.monstarmike.telemetry && window.com.monstarmike.telemetry.plugins && 
			window.com.monstarmike.telemetry.plugins.sharing && window.com.monstarmike.telemetry.plugins.sharing.share) {
			$log.debug('Exporting the image data to the social sharing plugin.');
			var dataUrl = $('#myChart canvas')[0].toDataURL('image/png');
			var successCallback = function () {
					$ionicLoading.hide();
				},
				errorCallback = function (error) {
					$ionicLoading.hide();
					$log.error('There was an error during the attempt to share via the sharing plugin!', error);
				};
			window.com.monstarmike.telemetry.plugins.sharing.share(dataUrl, successCallback, errorCallback);
		  } else {
			  $('#myChart canvas')[0].toBlob(function (blob) {
				  $log.debug('Canvas has been exported to a blob.');
				  $log.debug('Saving the file with browser functionality.');
				  saveAs(blob, filename);
				  $ionicLoading.hide();
			  });
		  }
      };

      $scope.$watch('service.selectedKey', function () {
        $scope.selectedFlightChanged();
      });


}])
  .controller('FileInfoController', ['$scope', '$window', '$ionicLoading', '$ionicScrollDelegate', 'filterFilter', 'dataService',
                                     function ($scope, $window, $ionicLoading, $ionicScrollDelegate, filterFilter, dataService) {

      $scope.service = dataService;

      $scope.$on('$ionicView.enter', function () {
        if (!$scope.service.file) {
          $ionicLoading.show();
          dataService.getCurrentData().then(function (file) {
            if (file && file.flights && file.flights.length > 0) {
              $scope.service.setSelectedFlight(file.flights[0]).then(function () {
                $ionicLoading.hide();
              });
            } else {
              $ionicLoading.hide();
            }
          });
        }
      });

}]);
