angular.module('telemetryReaderForAndroid.services', [])
  .service('dataService', ['$q', '$http', 'chartDefinitionsService', function ($q, $http, chartDefinitionsService) {
    this.file = null;
    this.selectedFlight = null;
    this.selectedKey = 'altitude';
    this.selectedTitle = 'Altitude';
    this.chart = null;

    this._setCurrentData = function (data) {
      this.file = data;
      _.forEach(this.file.flights, function (flight, index) {


      });
    };

    var testOne = true;

    this.getTestData = function () {
      var deferred = $q.defer();

      if (testOne) {
        console.log('testone');
        $http.get('js/file_data.json').then(function (response) {
          deferred.resolve(response.data);
        });
      } else {
        console.log('test two');
        $http.get('js/file_data.json').then(function (response) {
          deferred.resolve(response.data);
        });
      }
      testOne = !testOne;

      return deferred.promise;
    };

    this.loadData = function (storeAsCurrent) {
      var deferred = $q.defer(),
        that = this;

      if (window.com && window.com.monstarmike 
          && window.com.monstarmike.telemetry 
          && window.com.monstarmike.telemetry.plugins 
          && window.com.monstarmike.telemetry.plugins.tlmDecoder 
          && window.com.monstarmike.telemetry.plugins.tlmDecoder.openFile) {
        window.com.monstarmike.telemetry.plugins.tlmDecoder.openFile(function (data) {
            if (storeAsCurrent && data) {
              that._setCurrentData(that.file);
            }

            deferred.resolve(data);
          },
          function (e) {
            deferred.reject(e);
          });
      } else {
        console.log('getting test data');
        this.getTestData().then(function (data) {
          that._setCurrentData(data);
          deferred.resolve(that.file);
        });
      }

      return deferred.promise;
    };

    this.getCurrentData = function () {
      var deferred = $q.defer(),
        that = this;

      if (this.flights != null) {
        deferred.resolve(this.file);
      } else {
        this.loadData().then(function (data) {
          that._setCurrentData(data);
          deferred.resolve(that.file);
        });
      }

      return deferred.promise;
    };
    
    this.setSelectedFlight = function (flight) {
      var deferred = $q.defer(),
        that = this;

      if (window.com && window.com.monstarmike 
          && window.com.monstarmike.telemetry 
          && window.com.monstarmike.telemetry.plugins 
          && window.com.monstarmike.telemetry.plugins.tlmDecoder 
          && window.com.monstarmike.telemetry.plugins.tlmDecoder.openFile) {
        window.com.monstarmike.telemetry.plugins.tlmDecoder.decodeFlight(this.file, flight, function (decodedFlight) {
          that.selectedFlight = decodedFlight;
          deferred.resolve(that.selectedFlight);
        }, function (error) {
          console.error("error during decoding of flight.", error);
          deferred.reject(error);
        });
      }
      
      return deferred.promise;
    };
      }]);
