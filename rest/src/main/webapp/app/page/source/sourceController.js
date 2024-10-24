// Source data controller
tsApp
  .controller(
    'SourceCtrl',
    function($scope, $location, $http, $q, $interval, NgTableParams, sourceDataService,
      utilService, securityService, gpService, FileUploader, tabService, configureService) {
      console.debug('configure SourceCtrl');

      // Set up tabs and controller
      tabService.setShowing(true);
      utilService.clearError();
      $scope.user = securityService.getUser();
      projectService.getUserHasAnyRole();

      // /////////////////////
      // Local variables
      // /////////////////////

      var sourceDatas = [];
      var sourceDataFiles = [];
      var currentSourceData = null;

      // //////////////////////
      // Scope variables
      // //////////////////////

      $scope.loaders = null;
      $scope.tpSourceDatas = null;
      $scope.tpSourceDataFiles = null;
      $scope.hasZippedFiles = false;
      $scope.showFullPaths = false;

      //
      // Source data retrieval helper functions
      //

      // /////////////////////////////
      // ngTable refresh functions
      // /////////////////////////////

      // Instantiates new table params from sourceDatas array
      function refreshTables() {
        $scope.tpSourceDatas = new NgTableParams({}, {
          dataset : sourceDatas,
          counts : []
        // hides page sizes
        });
        $scope.tpSourceDataFiles = new NgTableParams({}, {
          dataset : $scope.currentSourceData ? $scope.currentSourceData.sourceDataFiles : [],
          counts : []
        // hides page sizes
        });
      }

      // view the source data and retrieve current source data file list
      $scope.viewSourceData = function(sourceData) {

        // set to null initially for currentSourceData watch condition
        $scope.isSourceDataModified = null;
        $scope.currentSourceData = sourceData;

        sourceDataService.findSourceDataFiles("").then(
        // Success
        function(response) {
          sourceDataFiles = response.sourceDataFiles;
          refreshTables();
        });
      };

      // watch for changes to current source data to enable save/cancel buttons
      $scope.$watch('currentSourceData', function() {
        // if null condition found, newly loaded/unmodified source data
        if ($scope.isSourceDataModified == null) {
          $scope.isSourceDataModified = false;
        } else {
          $scope.isSourceDataModified = true;
        }
      }, true);

      // Create new source data JSON object
      $scope.createSourceData = function() {
        var sourceData = {
          name : null,
          description : null,
          lastModifiedBy : securityService.getUser().userName,
          status : 'NEW',
          statusText : 'New source data package',
          sourceDataFiles : [],
          handler : null,
          terminology : null,
          version : null
        };
        sourceDatas.splice(0, 0, sourceData);
        $scope.currentSourceData = sourceData;
        refreshTables();
      };

      // Save source data
      $scope.saveSourceData = function(sourceData) {
        if (!sourceData.name) {
          window.alert('The source data name cannot be empty');
          return;
        }
        sourceDataService.updateSourceData(sourceData).then(function(response) {
          // update the source data and current source data if response returned
          // (add call)
          if (response) {
            // sourceData = response;
            $scope.currentSourceData = response;
          }
          $scope.isSourceDataModified = false;
        });
      };

      // Cancel source data modifications
      $scope.cancelSourceDataModifications = function() {
        if (!$scope.isSourceDataModified || window.confirm('Discard changes?')) {
          getSourceDatas().then(function() {
            angular.forEach(sourceDatas, function(sourceData) {
              if (sourceData.id === $scope.currentSourceData.id) {
                $scope.currentSourceData = sourceData;
                $scope.isSourceDataModified = false;
              }

            });
          });

        }
      };

      // Remove source data
      $scope.removeSourceData = function(sourceData) {

        // if local only, simply remove from the list
        if (!sourceData.id) {
          for (var i = 0; i < sourceDatas.length; i++) {
            if (sourceData === sourceDatas[i]) {
              sourceDatas.splice(i, 1);
              break;
            }
          }
        }

        // otherwise ask user for confirmation and delete
        else if (confirm('This will delete any uploaded files for this configuration. Are you sure?')) {
          sourceDataService.removeSourceData(sourceData).then(
          // Success
          function(response) {
            if ($scope.currentSourceData && $scope.currentSourceData.id === sourceData.id) {
              $scope.currentSourceData = null;
            }
            // retrieve source datas, also refreshes tables
            getSourceDatas();
          });
        }

      };

      // Add source data to source data list
      $scope.addSourceDataFileToSourceData = function(sourceData, file) {
        if (!sourceData.sourceDataFiles || !Array.isArray(sourceDataFiles)) {
          sourceData.sourceDataFiles = [ file ];
        } else {
          sourceData.sourceDataFiles.push(file);
        }
        refreshTables();
      };

      // Remove source data from list
      $scope.removeSourceDataFileFromSourceData = function(file) {
        sourceDataService.removeSourceDataFile(file.id).then(function() {
          getSourceDatas().then(function() {
            angular.forEach(sourceDatas, function(sourceData) {
              if (sourceData.id === $scope.currentSourceData.id) {
                $scope.currentSourceData = sourceData;
              }
            });
          });
        });
      };

      // cancel a running process
      $scope.cancelSourceDataProcess = function(sourceData) {
        sourceDataService.cancelSourceDataProcess(sourceData).then(function() {
          getSourceDatas();
        });
      };

      $scope.getFilePath = function(file) {
        var id = $scope.currentSourceData.id;
        return file.path.substring(file.path.indexOf(id) + id.toString().length + 1);
      };

      // Refreshes source data list from server and instantiates new table
      // params
      function getSourceDatas() {
        var deferred = $q.defer();
        sourceDataService.findSourceData("").then(
        // Success
        function(response) {
          sourceDatas = response.sourceDatas;

          // find and refresh the currently viewed source data
          if ($scope.currentSourceData) {
            angular.forEach(sourceDatas, function(sourceData) {
              if ($scope.currentSourceData.id === sourceData.id) {
                $scope.currentSourceData = sourceData;
                $scope.isSourceDataModified = false;
              }
            });
          }

          // check for polling requirements
          angular.forEach(sourceDatas, function(sourceData) {
            if (sourceData.status === 'LOADING' || sourceData.status === 'REMOVING') {
              $scope.startPolling(sourceData);
            }

          });

          refreshTables();
          deferred.resolve();
        }, function() {
          deferred.reject();
        });
      }

      // Gets $scope.loaders
      function getSourceDataHandlers() {
        sourceDataService.getSourceDataHandlers().then(
        // Success
        function(sourceDataHandlers) {
          $scope.sourceDataHandlers = sourceDataHandlers;

        });
      }

      $scope.destroy = function() {
        configureService.destroy().then(function() {
          $scope.isSnomedLoaded = false;
          $scope.previousSourceData = false;
          $scope.currentSourceData = false;
        });
      };

      // currently active polls, array of objects {id, {poll: ..., log: ...}}
      $scope.polls = {};

      $scope.loadFromSourceData = function(sourceData) {
        // ensure that source data is not modified
        if ($scope.isSourceDataModified) {
          window.alert('Save or cancel changes to source data before loading.');
          return;
        }

        // start load and initiate polling
        sourceDataService.loadFromSourceData(sourceData).then(function() {
          $scope.startPolling(sourceData);
        });

      };

      $scope.removeFromSourceData = function(sourceData) {
        sourceDataService.removeFromSourceData(sourceData).then(function() {
          $scope.startPolling(sourceData);
        });
      };

      $scope.startPolling = function(sourceData) {
        console.log('Starting status polling for ' + sourceData.name);

        // construct the polling object
        $scope.polls[sourceData.id] = {
          status : 'Initializing...',
          poll : null,
          logEntries : null
        };

        $scope.polls[sourceData.id].poll = $interval(function() {

          // get the source data by id (suppress glass pane)
          sourceDataService.getSourceData(sourceData.id, true).then(function(polledSourceData) {
            if (polledSourceData) {
              $scope.updateSourceDataFromPoll(polledSourceData);
            } else {
              $scope.stopPolling(sourceData);
            }
          });

          // get and set the log entries
          sourceDataService.getSourceDataLog(sourceData.terminology, sourceData.version, null, 100)
            .then(function(logEntries) {
              if ($scope.polls[sourceData.id]) {
                $scope.polls[sourceData.id].logEntries = logEntries;
              }
            });

        }, 3142);
      };

      $scope.stopPolling = function(sourceData) {
        console.log('Stop polling for source data ' + sourceData.id + ': ' + sourceData.name);
        $interval.cancel($scope.polls[sourceData.id].poll);
        delete $scope.polls[sourceData.id];
      };

      // cancel all polling on reloads or navigation
      $scope.$on("$routeChangeStart", function(event, next, current) {
        for ( var key in $scope.polls) {
          if ($scope.polls.hasOwnProperty(key)) {
            $interval.cancel($scope.polls[key].poll);
          }
        }
      });

      $scope.updateSourceDataFromPoll = function(polledSourceData) {
        if (!polledSourceData) {
          utilService.setError('Cannot update source data from poll results');
          return;
        }

        // find the source data in the table and replace it
        angular.forEach(sourceDatas, function(sourceData) {
          if (sourceData.id === polledSourceData.id) {
            // TODO: I don't think this actually does anything
            sourceData = polledSourceData;
            refreshTables();
          }

        });
        if ($scope.currentSourceData.id === polledSourceData.id) {
          $scope.currentSourceData = polledSourceData;
        }
        // update poll status from data
        $scope.polls[polledSourceData.id].status = polledSourceData.status;

        // perform actions nased on newly polled status
        switch (polledSourceData.status) {
        case 'LOADING_COMPLETE':
          utilService.handleSuccess('Terminology load completed for '
            + polledSourceData.terminology + ', ' + polledSourceData.version);
          $scope.stopPolling(polledSourceData);
          getSourceDatas();
          break;
        case 'LOADING_FAILED':
          utilService.handleError('Terminology load failed for ' + polledSourceData.terminology
            + ', ' + polledSourceData.version);
          $scope.stopPolling(polledSourceData);
          getSourceDatas();
          break;
        case 'REMOVAL_COMPLETE':
          utilService.handleSuccess('Terminology removal completed for '
            + polledSourceData.terminology + ', ' + polledSourceData.version);
          $scope.stopPolling(polledSourceData);
          getSourceDatas();
          break;
        case 'REMOVAL_FAILED':
          utilService.handleError('Terminology removal failed for ' + polledSourceData.terminology
            + ', ' + polledSourceData.version);
          $scope.stopPolling(polledSourceData);
          getSourceDatas();
          break;
        default:
          // do nothing
        }
      };

      //
      // Angular File Upload controls
      //

      function isZipFile(item) {
        return item.file.name.match(/.*\.zip/g) !== null;
      }

      // Specify the angular-file-uploader
      var uploader = $scope.uploader = new FileUploader({
        url : sourceDataUrl + '/upload'
      });

      // FILTERS

      uploader.filters = [];

      // CALLBACKS
      uploader.onWhenAddingFileFailed = function(item /* {File|FileLikeObject} */, filter, options) {
        // console.info('onWhenAddingFileFailed', item, filter, options);
      };
      uploader.onAfterAddingFile = function(fileItem) {
        // console.info('onAfterAddingFile', fileItem);
        fileItem.isZipped = isZipFile(fileItem);
        if (fileItem.isZipped) {
          $scope.hasZippedFiles = true;
          fileItem.unzip = true;
        }
      };
      uploader.onAfterAddingAll = function(addedFileItems) {
        // console.info('onAfterAddingAll', addedFileItems);
        angular.forEach(addedFileItems, function(fileItem) {
          fileItem.isZipped = isZipFile(fileItem);
          if (fileItem.isZipped) {
            $scope.hasZippedFiles = true;
          }
        });
      };
      uploader.onBeforeUploadItem = function(item) {

        // dynamically set the upload url with the unzip flag
        item.url = sourceDataUrl + '/upload/' + $scope.currentSourceData.id + '?unzip='
          + (item.unzip ? 'true' : 'false');

        // manually set the headers on the item's request (does not inherit from
        // $http, apparently)
        item.headers = {
          'Authorization' : 'admin'
        };
      };
      uploader.onProgressItem = function(fileItem, progress) {
        // console.info('onProgressItem', fileItem, progress);
      };
      uploader.onProgressAll = function(progress) {
        // console.info('onProgressAll', progress);
      };
      uploader.onSuccessItem = function(fileItem, response, status, headers) {
        console.info('onSuccessItem', uploader, fileItem, response, status, headers);
        uploader.queue = uploader.queue.filter(function(item) {
          return !item.isSuccess;
        });
      };
      uploader.onErrorItem = function(fileItem, response, status, headers) {
        // console.info('onErrorItem', fileItem, response, status, headers);
        utilService
          .handleError({
            data : response ? response
              : "Folders cannot be uploaded; only single files or zip files containing no folders may be uploaded.",
            status : status,
            headers : headers
          }); // shoehorn into tsApp expected error format
      };

      uploader.onCancelItem = function(fileItem, response, status, headers) {
        // console.info('onCancelItem', fileItem, response, status, headers);
      };

      uploader.onCompleteItem = function(fileItem, response, status, headers) {
        console.info('onCompleteItem', fileItem, response, status, headers);
      };
      uploader.onCompleteAll = function() {
        console.info('onCompleteAll', uploader);
        getSourceDatas();
      };

      // scope to capitalize first initials only
      $scope.getHumanText = function(str) {
        if (!str) {
          return null;
        }
        return str.replace('_', ' ').replace(/\w\S*/g, function(txt) {
          return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
        });

      };

      //
      // Initialize if USER
      //
      $scope.initialize = function() {
        if (securityService.isUser()) {
          getSourceDatas();
          getSourceDataHandlers();
        }
      };
      //
      // Initialization: Check that application is configured
      //
      configureService.isConfigured().then(function(isConfigured) {
        if (!isConfigured) {
          $location.path('/configure');
        } else {
          $scope.initialize();
        }
      });

    });