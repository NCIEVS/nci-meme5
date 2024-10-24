// Notes directive
tsApp.directive('notes', [ function() {
  return {
    restrict : 'A',
    scope : {
      selected : '=',
      type : '@',
      empty : '='
    },
    templateUrl : 'app/actions/notes/notes.html',
    controller : [
      '$scope',
      '$uibModal',
      '$sce',
      'utilService',
      'workflowService',
      'contentService',
      function($scope, $uibModal, $sce, utilService, workflowService, contentService) {
        console.debug("configure notes directive", $scope.type);

        $scope.field = $scope.type.toLowerCase();
        if ($scope.type == 'Checklist') {
          $scope.field = 'worklist';
        } else if ($scope.type == 'Concept' || $scope.type == 'Atom') {
          $scope.field = 'component';
        }

        // Notes modal
        $scope.openNotesModal = function() {

          var modalInstance = $uibModal.open({
            templateUrl : 'app/actions/notes/notesModal.html',
            controller : NotesModalCtrl,
            backdrop : 'static',
            size : 'lg',
            resolve : {
              selected : function() {
                return $scope.selected;
              },
              type : function() {
                return $scope.type;
              },
            }
          });

          // NO need for result function - no action on close
          // modalInstance.result.then(function(data) {});
        }

        var NotesModalCtrl = function($scope, $uibModalInstance, selected, type) {
          console.debug("configure NotesModalCtrl");
          // Scope vars
          $scope.field = type.toLowerCase();
          if (type == 'Checklist') {
            $scope.field = 'worklist';
          } else if (type == 'Concept' || type == 'Atom') {
            $scope.field = 'component';
          }

          $scope.selected = selected;
          $scope.type = type
          $scope.project = selected.project
          $scope.tinymceOptions = utilService.tinymceOptions;
          $scope.newNote = null;

          // Paging parameters
          $scope.pagedNotes = [];
          $scope.pageSizes = utilService.getPageSizes();
          $scope.paging = utilService.getPaging();
          $scope.paging.sortField = 'lastModified';
          $scope.paging.sortAscending = true;
          $scope.paging.callbacks = {
            getPagedList : getPagedNotes
          };

          $scope.errors = [];

          // Get paged notes (assume all are loaded)
          $scope.getPagedNotes = function() {
            getPagedNotes();
          }
          function getPagedNotes() {

            var pagedArray = utilService.getPagedArray($scope.selected[$scope.field].notes,
              $scope.paging, $scope.pageSize);
            $scope.pagedNotes = pagedArray.data;
            $scope.pagedNotes.totalCount = pagedArray.totalCount;
          }

          // Trusted note value
          $scope.getNoteValue = function(note) {
            return $sce.trustAsHtml(note.note);
          };

          // remove note
          $scope.removeNote = function(object, note) {

            if ($scope.type == 'Worklist') {
              workflowService.removeWorklistNote($scope.project.id, note.id).then(
              // Success - remove worklist note
              function(data) {
                $scope.newNote = null;
                workflowService.getWorklist($scope.project.id, object.id).then(function(data) {
                  object.notes = data.notes;
                  $scope.getPagedNotes();
                },
                // Error - remove worklist
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              },
              // Error - remove worklist note
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            } else if ($scope.type == 'Checklist') {
              workflowService.removeChecklistNote($scope.project.id, note.id).then(
              // Success - remove checklist note
              function(data) {
                $scope.newNote = null;
                workflowService.getChecklist($scope.project.id, object.id).then(function(data) {
                  object.notes = data.notes;
                  $scope.getPagedNotes();
                },
                // Error - remove checklist
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              },
              // Error - remove checklist note
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            } else if ($scope.type == 'Concept') {
              contentService.removeComponentNote(object, note.id).then(
              // Success - remove concept note
              function(data) {
                $scope.newNote = null;
                contentService.getConcept(object.id, $scope.project.id).then(function(data) {
                  object.notes = data.notes;
                  $scope.getPagedNotes();
                })});
            } else if ($scope.type == 'Atom') {
              contentService.removeComponentNote(object, note.id).then(
                // Success - remove atom note
                function(data) {
                  $scope.newNote = null;
                  contentService.getAtom(object.id, $scope.project.id).then(function(data) {
                    object.notes = data.notes;
                    $scope.getPagedNotes();
                  });
                });
              
              }
            
          };

          // add new note
          $scope.submitNote = function(object, text) {

            if ($scope.type == 'Checklist') {
              workflowService.addChecklistNote($scope.project.id, object.id, text).then(
              // Success - add checklist note
              function(data) {
                $scope.newNote = null;
                workflowService.getChecklist($scope.project.id, object.id).then(function(data) {
                  object.notes = data.notes;
                  $scope.getPagedNotes();
                },
                // Error - add checklist note
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              },
              // Error - add checklist note
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            } else if ($scope.type == 'Worklist') {
              workflowService.addWorklistNote($scope.project.id, object.id, text).then(
              // Success - add worklist note
              function(data) {
                $scope.newNote = null;
                workflowService.getWorklist($scope.project.id, object.id).then(function(data) {
                  object.notes = data.notes;
                  $scope.getPagedNotes();
                },
                // Error - add worklist note
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              },
              // Error - add worklist note
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            } else if ($scope.type == 'Concept') {
              contentService.addComponentNote(object, text).then(
                // Success - add concept note
                function(data) {
                  $scope.newNote = null;
                  contentService.getConcept(object.id, $scope.project.id).then(function(data) {
                    object.notes = data.notes;
                    $scope.getPagedNotes();
                  },
                  // Error - add concept note
                  function(data) {
                    utilService.handleDialogError($scope.errors, data);
                  });
                },
                // Error - add concept note
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              
            } else if ($scope.type == 'Atom') {
              contentService.addComponentNote(object, text).then(
                // Success - add atom note
                function(data) {
                  $scope.newNote = null;
                  contentService.getAtom(object.id, $scope.project.id).then(function(data) {
                    object.notes = data.notes;
                    $scope.getPagedNotes();
                  },
                  // Error - add atom note
                  function(data) {
                    utilService.handleDialogError($scope.errors, data);
                  });
                },
                // Error - add atom note
                function(data) {
                  utilService.handleDialogError($scope.errors, data);
                });
              }
          };

          // Convert date to a string
          $scope.toDate = function(lastModified) {
            return utilService.toDate(lastModified);
          };

          // Dismiss modal
          $scope.cancel = function() {
            $uibModalInstance.dismiss('cancel');
          };

          // initialize modal
          $scope.getPagedNotes();
        };

        // end
      } ]

  };
} ]);
