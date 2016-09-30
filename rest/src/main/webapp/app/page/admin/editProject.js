// Edit project modal controller
tsApp.controller('EditProjectModalCtrl', [
  '$scope',
  '$uibModalInstance',
  'utilService',
  'metadataService',
  'projectService',
  'selected',
  'lists',
  'user',
  'validationChecks',
  'action',
  'project',
  function($scope, $uibModalInstance, utilService, metadataService, projectService, selected,
    lists, user, validationChecks, action, project) {

    // Scope variables
    $scope.action = action;

    // use project if passed in
    $scope.project = (project ? project : {
      feedbackEmail : user.userPreferences.feedbackEmail
    });
    $scope.selected = selected;
    $scope.lists = lists;
    $scope.user = user;
    $scope.validationChecks = validationChecks;
    $scope.availableChecks = [];
    $scope.selectedChecks = [];
    $scope.languages = [];
    $scope.errors = [];

    // move a check from unselected to selected
    $scope.selectValidationCheck = function(check) {
      $scope.selectedChecks.push(check);
      var index = $scope.availableChecks.indexOf(check);
      $scope.availableChecks.splice(index, 1);
    };

    // move a check from selected to unselected
    $scope.removeValidationCheck = function(check) {
      $scope.availableChecks.push(check);
      var index = $scope.selectedChecks.indexOf(check);
      $scope.selectedChecks.splice(index, 1);
    };

    // get the terminology object for an abbreviation
    $scope.getTerminology = function(terminology) {
      for (var i = 0; i < $scope.lists.terminologies.length; i++) {
        if ($scope.lists.terminologies[i].terminology == terminology) {
          return $scope.lists.terminologies[i];
        }
      }
      return {
        version : null
      };
    }
    // Handler for the terminology changing
    $scope.setTerminology = function(terminology) {
      $scope.project.version = $scope.getTerminology(terminology).version;
      // Get metadata for languages for this terminology/version
      if ($scope.project.version) {
        $scope.errors = [];
        metadataService.getAllMetadata($scope.project.terminology, $scope.project.version).then(
        // Success
        function(data) {
          $scope.languages = data.languages;
          if (!$scope.project.language) {
            $scope.project.language = 'ENG';
          }
        },
        // Error
        function(data) {
          utilService.handleDialogError($scope.errors, data);
        });
      }
    }

    // Handler for version changing
    $scope.setVersion = function(version) {
      // Get metadata for languages for this terminology/version
      metadataService.getAllMetadata($scope.project.terminology, $scope.project.version).then(
      // Success
      function(data) {
        $scope.languages = data.languages;
        if (!$scope.project.language) {
          $scope.project.language = 'ENG';
        }
      },
      // Error
      function(data) {
        utilService.handleDialogError($scope.errors, data);
      });

    }

    // Add the project
    $scope.submitProject = function(project) {
      if (!project || !project.name || !project.description || !project.terminology) {
        window.alert('The name, description, and terminology fields cannot be blank. ');
        return;
      }
      // Connect validation checks
      project.validationChecks = [];
      for (var i = 0; i < $scope.validationChecks.length; i++) {
        if ($scope.selectedChecks.indexOf($scope.validationChecks[i].value) != -1) {
          project.validationChecks.push($scope.validationChecks[i].key);
        }
      }

      // Add project - this will validate the expression
      projectService.addProject(project).then(
        // Success
        function(data) {
          // if not an admin, add user as a project admin
          if ($scope.user.applicationRole != 'ADMINISTRATOR') {
            var projectId = data.id;
            projectService.assignUserToProject(data.id, $scope.user.userName, 'ADMINISTRATOR')
              .then(function(data) {
                // Update 'anyrole'
                projectService.getUserHasAnyRole();

                // Set the "last project" setting to this project
                $scope.user.userPreferences.lastProjectId = projectId;
                securityService.updateUserPreferences($scope.user.userPreferences);
                $uibModalInstance.close(data);
              },
              // Error
              function(data) {
                $scope.errors[0] = data;
                utilService.clearError();
              });
          } else {
            // Close modal and send back the project
            $uibModalInstance.close(data);
          }
        },
        // Error
        function(data) {
          $scope.errors[0] = data;
          utilService.clearError();
        });
    };

    // Dismiss the modal
    $scope.cancel = function() {
      $uibModalInstance.dismiss('cancel');
    };

    //
    // INITIALIZE
    //

    // Configure validation checks
    if (project) {
      // Attach validation checks
      for (var i = 0; i < $scope.validationChecks.length; i++) {
        if (project.validationChecks.indexOf($scope.validationChecks[i].key) > -1) {
          $scope.selectedChecks.push($scope.validationChecks[i].value);
        } else {
          $scope.availableChecks.push($scope.validationChecks[i].value);
        }
      }
      $scope.setTerminology(project.terminology);
    } else {
      // Wire default validation check 'on' by default
      for (var i = 0; i < $scope.validationChecks.length; i++) {
        if ($scope.validationChecks[i].value.startsWith('Default')) {
          $scope.selectedChecks.push($scope.validationChecks[i].value);
        } else {
          $scope.availableChecks.push($scope.validationChecks[i].value);
        }
      }
    }

    // end
  } ]);