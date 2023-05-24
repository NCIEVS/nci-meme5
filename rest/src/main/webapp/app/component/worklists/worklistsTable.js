// Worklist table
// e.g. <div worklist-table value='Worklist' />
tsApp
  .directive(
    'worklistsTable',
    [
      '$uibModal',
      '$window',
      '$sce',
      '$interval',
      'utilService',
      'securityService',
      'projectService',
      'workflowService',
      function($uibModal, $window, $sce, $interval, utilService, securityService, projectService,
        workflowService) {
        return {
          restrict : 'A',
          scope : {
            // Legal 'value' settings include:
            // Worklist, Checklist
            type : '@',
            selected : '=',
            lists : '=',
            user : '='
          },
          templateUrl : 'app/component/worklists/worklistsTable.html',
          controller : [
            '$scope',
            function($scope) {

              // Reset some scope settings
              $scope.selectedWorklist = null;
              $scope.selected.record = null;
              $scope.lists.records = [];
              $scope.worklistReport = null;
              $scope.reportRefresh = null;

              // This structure reused so don't conflate
              $scope.worklists = [];

              // Paging variables
              $scope.pageSizes = utilService.getPageSizes();
              $scope.paging = {};
              $scope.paging['worklists'] = utilService.getPaging();
              $scope.paging['worklists'].sortField = 'lastModified';
              $scope.paging['worklists'].filterList = new Array();
              $scope.paging['worklists'].sortAscending = false;
              $scope.paging['worklists'].callbacks = {
                getPagedList : getWorklists
              };
              $scope.paging['records'] = utilService.getPaging();
              $scope.paging['records'].sortField = 'clusterId';
              $scope.paging['records'].callbacks = {
                getPagedList : getRecords
              };

              // Project Changed Handler
              $scope.$watch('selected.project', function() {
                if ($scope.selected.project) {
                  // Set project, refresh worklist list
                  $scope.setProject($scope.selected.project);
                }
              });

              // checklists/worklists changed
              $scope.$watch('selected.refreshCt', function() {
                // Skip initial setting
                if ($scope.selected.refreshCt) {
                  $scope.getWorklists($scope.selectedWorklist);
                }
              });

              // Handle workflow changes
              $scope.$on('termServer::checklistChange', function(event, data) {
                if (data.id == $scope.selected.project.id && $scope.type == 'Checklist') {
                  // refresh the list
                  $scope.getWorklists($scope.selectedWorklist);
                }
              });

              $scope.$on('termServer::worklistChange', function(event, data) {
                if (data.id == $scope.selected.project.id && $scope.type == 'Worklist') {
                  // refresh the list
                  $scope.getWorklists($scope.selectedWorklist);
                }
              });

              // scope paging filter list
              $scope.getPagingFilterList = function() {
                var filterList = new Array();
                for ( var userName in $scope.selected.project.userRoleMap) {
                  filterList.push("authors:" + userName);
                  if ($scope.selected.project.userRoleMap[userName] != 'AUTHOR') {
                    filterList.push("reviewers:" + userName);
                  }
                }
                return filterList.sort();
              }

              // See if any project users have a team
              $scope.hasTeam = function() {
                for (var i = 0; i < $scope.selected.users.length; i++) {
                  if ($scope.selected.users[i].team) {
                    return true;
                  }
                }
                return false;
              }
              // Compose a string of all editors for display
              $scope.joinEditors = function(worklist) {
                // check reviewers first because this is who we would unassign
                // from
                if (worklist.reviewers && worklist.reviewers.length > 0) {
                  return worklist.reviewers.join(' ');
                }

                // then authors
                else if (worklist.authors && worklist.authors.length > 0) {
                  return worklist.authors.join(' ');
                }
                return '';
              };

              // Get the "max" workflow state
              $scope.getWorkflowState = function(worklist) {
                var maxD = 0;
                var maxState = 'xxx';
                for ( var k in worklist.workflowStateHistory) {
                  var item = worklist.workflowStateHistory[k];

                  if (!maxD) {
                    maxD = item;
                    maxState = k;
                  }
                  if (item > maxD) {
                    maxD = item;
                    maxState = k;
                  }
                }
                return maxState;
              }

              // Set $scope.selected.project and reload
              $scope.setProject = function(project) {
                $scope.project = project;
                $scope.getWorklists();
                if ($scope.type == 'Worklist') {
                  $scope.paging['worklists'].filterList.length = 0;
                  var list = $scope.getPagingFilterList();
                  for (var i = 0; i < list.length; i++) {
                    $scope.paging['worklists'].filterList.push(list[i]);
                  }
                }
              };

              // Get $scope.worklists (reselect worklist passed in)
              $scope.getWorklists = function(worklist) {
                getWorklists(worklist);
              }
              function getWorklists(worklist) {
                var paging = $scope.paging['worklists'];
                var pfs = {
                  startIndex : (paging.page - 1) * paging.pageSize,
                  maxResults : paging.pageSize,
                  sortField : paging.sortField,
                  ascending : paging.sortAscending,
                  queryRestriction : paging.filter
                };

                if ($scope.type == 'Worklist') {
                  workflowService.findWorklists($scope.selected.project.id, $scope.query, pfs)
                    .then(function(data) {
                      $scope.worklists = data.worklists;
                      $scope.worklists.totalCount = data.totalCount;
                      $scope.lists.worklistCt = data.totalCount;
                      if (worklist) {
                        for (var i = 0; i < data.worklists.length; i++) {
                          if (data.worklists[i].id == worklist.id) {
                            $scope.selectWorklist(data.worklists[i]);
                          }
                        }
                      }
                    });
                }
                if ($scope.type == 'Checklist') {
                  workflowService.findChecklists($scope.selected.project.id, $scope.query, pfs)
                    .then(function(data) {
                      $scope.worklists = data.checklists;
                      $scope.worklists.totalCount = data.totalCount;
                      $scope.lists.checklistCt = data.totalCount;
                      if (worklist) {
                        for (var i = 0; i < data.checklists.length; i++) {
                          if (data.checklists[i].id == worklist.id) {
                            $scope.selectWorklist(data.checklists[i]);
                          }
                        }
                      }
                    });
                }
              }
              ;

              // Get $scope.records
              $scope.getRecords = function() {
                getRecords();
              }
              function getRecords() {

                var paging = $scope.paging['records'];
                // NE-596 hardcode increase in page size for now
                paging.pageSize = 200;
                var pfs = {
                  startIndex : (paging.page - 1) * paging.pageSize,
                  maxResults : paging.pageSize,
                  sortField : paging.sortField,
                  ascending : paging.sortAscending,
                  queryRestriction : paging.filter
                };

                if (paging.typeFilter) {
                  var value = paging.typeFilter;

                  // Handle status
                  if (value == 'N') {
                    pfs.queryRestriction += (pfs.queryRestriction ? ' AND ' : '')
                      + ' workflowStatus:N*';
                  } else if (value == 'R') {
                    pfs.queryRestriction += (pfs.queryRestriction ? ' AND ' : '')
                      + ' workflowStatus:R*';
                  }

                }

                if ($scope.type == 'Worklist') {

                  workflowService.findTrackingRecordsForWorklist($scope.selected.project.id,
                    $scope.selectedWorklist.id, pfs).then(
                  // Success
                  function(data) {
                    $scope.lists.records = data.records;
                    $scope.lists.records.totalCount = data.totalCount;
                  });
                } else if ($scope.type == 'Checklist') {
                  workflowService.findTrackingRecordsForChecklist($scope.selected.project.id,
                    $scope.selectedWorklist.id, pfs).then(
                  // Success
                  function(data) {
                    $scope.lists.records = data.records;
                    $scope.lists.records.totalCount = data.totalCount;
                  });
                }

              }

              // Get $scope.worklistReport
              $scope.findGeneratedConceptReports = function() {
                $scope.worklistReport = null;
                workflowService.findGeneratedConceptReports($scope.selected.project.id,
                  $scope.selectedWorklist.name, {
                    startIndex : 0,
                    maxResults : 1
                  }).then(
                // Success
                function(data) {
                  if (data.strings) {
                    $scope.reportRefresh = null;
                    $scope.worklistReport = data.strings[0];
                  }
                });
              }

              // Export a report
              $scope.getGeneratedConceptReport = function() {
                // Download report
                workflowService.getGeneratedConceptReport($scope.selected.project.id,
                  $scope.selectedWorklist.name + '_rpt.txt');
              }

              // Export a report
              $scope.removeGeneratedConceptReport = function() {
                // Download report
                workflowService.removeGeneratedConceptReport($scope.selected.project.id,
                  $scope.selectedWorklist.name + '_rpt.txt').then(
                // Success
                function(data) {
                  $scope.worklistReport = null;
                });
              }

              // Generate a concept report - this may take a while, show a
              // "refresh" icon
              $scope.generateConceptReport = function() {
                workflowService.generateConceptReport($scope.selected.project.id,
                  $scope.selectedWorklist.id);
                $scope.reportRefresh = true;
                $scope.refresh = $interval(function() {
                  if (!$scope.reportRefresh) {
                    $interval.cancel($scope.refresh)
                  }
                  $scope.findGeneratedConceptReports();
                }, 500);
              }

              // Convert time to a string
              $scope.toTime = function(editingTime) {
                return utilService.toTime(editingTime);
              };

              // Convert date to a string
              $scope.toDate = function(lastModified) {
                return utilService.toDate(lastModified);
              };
              
              $scope.hasPermissions = function(action) {
                return securityService.hasPermissions(action);
              }
              
              // Table sorting mechanism
              $scope.setSortField = function(table, field, object) {
                utilService.setSortField(table, field, $scope.paging);

                // retrieve the correct table
                if (table === 'worklists') {
                  $scope.getWorklists($scope.selectedWorklist);
                }
                if (table === 'records') {
                  $scope.getWorklists($scope.selectedWorklist);
                }
              };

              // Return up or down sort chars if sorted
              $scope.getSortIndicator = function(table, field) {
                return utilService.getSortIndicator(table, field, $scope.paging);
              };

              // Selects a worklist (setting $scope.selectedWorklist).
              // Looks up current release info and records.
              $scope.selectWorklist = function(worklist) {
                workflowService['get' + $scope.type]($scope.selected.project.id, worklist.id).then(
                  function(data) {
                    $scope.selectedWorklist = data;
                    if ($scope.type == 'Worklist') {
                      $scope.parseStateHistory(data);
                    }
                    $scope.getRecords(data);
                    $scope.findGeneratedConceptReports();
                  });
              };

              // parse workflow state history
              $scope.parseStateHistory = function(worklist) {
                $scope.stateHistory = [];
                var states = Object.keys(worklist.workflowStateHistory);
                for (var i = 0; i < states.length; i++) {
                  var state = {
                    name : states[i],
                    timestamp : worklist.workflowStateHistory[states[i]]
                  }
                  $scope.stateHistory.push(state);
                }
                $scope.stateHistory = $scope.stateHistory.sort(utilService.sortBy('timestamp'))
              }

              // Unassign worklist
              $scope.unassignWorklist = function(worklist, userName) {
                workflowService.performWorkflowAction($scope.selected.project.id, worklist.id,
                  userName, $scope.selected.project.userRoleMap[$scope.user.userName], 'UNASSIGN')
                  .then(
                  // Success
                  function(data) {
                    $scope.getWorklists(data);
                  });
              };

              // Remove a worklist
              $scope.removeWorklist = function(worklist) {
                $scope.removeWorklistHelper(worklist);
              };

              // Helper for removing a worklist/checklist
              $scope.removeWorklistHelper = function(worklist) {

                if ($scope.type == 'Worklist') {
                  workflowService.removeWorklist($scope.selected.project.id, worklist.id).then(
                    function() {
                      $scope.selectedWorklist = null;
                      $scope.getWorklists();
                    });
                } else {
                  workflowService.removeChecklist($scope.selected.project.id, worklist.id).then(
                    function() {
                      $scope.selectedWorklist = null;
                      $scope.getWorklists();
                    });
                }
                // });
              };

              // Unassign worklist from user
              $scope.unassign = function(worklist, userName) {
                $scope.performWorkflowAction(worklist, 'UNASSIGN', userName);
              };

              // handle workflow advancement
              $scope.handleWorkflow = function(worklist) {
                if ($scope.type == 'ASSIGNED'
                  && worklist
                  && (worklist.workflowStatus == 'NEW' || worklist.workflowStatus == 'READY_FOR_PUBLICATION')) {
                  $scope.performWorkflowAction(worklist, 'SAVE', $scope.user.userName);
                } else {
                  $scope.getWorklists($scope.selectedWorklist);
                }
              };

              // stamp(approve)/unapprove entire worklist
              $scope.stamp = function(worklist, approve) {
                console.debug("stamp:" + worklist);
                if ($scope.type == 'Worklist') {
                  workflowService
                    .stampWorklist($scope.selected.project.id, worklist, approve, true).then(
                      function() {
                        $scope.selectedWorklist = null;
                        $scope.getWorklists(worklist);
                      });
                } else if ($scope.type == 'Checklist') {
                  workflowService.stampChecklist($scope.selected.project.id, worklist, approve,
                    true).then(function() {
                    $scope.selectedWorklist = null;
                    $scope.getWorklists(worklist);
                  });
                }
              }

              // Performs a workflow action
              $scope.performWorkflowAction = function(worklist, action, userName) {

                workflowService.performWorkflowAction($scope.selected.project.id, worklist.id,
                  userName, $scope.selected.projects.role, action).then(function(data) {
                  $scope.getWorklists($scope.selectedWorklist);
                });
              };

              // Get the most recent note for display
              $scope.getLatestNote = function(worklist) {
                if (worklist && worklist.notes && worklist.notes.length > 0) {
                  return $sce.trustAsHtml(worklist.notes.sort(utilService
                    .sortBy('lastModified', -1))[0].note);
                }
                return $sce.trustAsHtml('');
              };

              // Export a worklist
              $scope.exportList = function(worklist) {
                if ($scope.type == 'Checklist') {
                  workflowService.exportChecklist($scope.selected.project.id, worklist.id,
                    worklist.name);
                } else if ($scope.type == 'Worklist') {
                  workflowService.exportWorklist($scope.selected.project.id, worklist.id,
                    worklist.name);
                }
              }

              //
              // MODALS
              //

              // Assign worklist modal
              $scope.openAssignWorklistModal = function(lworklist, laction) {

                var modalInstance = $uibModal.open({
                  templateUrl : 'app/page/workflow/assignWorklist.html',
                  controller : 'AssignWorklistModalCtrl',
                  backdrop : 'static',
                  resolve : {
                    selected : function() {
                      return $scope.selected;
                    },
                    lists : function() {
                      return $scope.lists;
                    },
                    user : function() {
                      return $scope.user;
                    },
                    worklist : function() {
                      return lworklist;
                    },
                    action : function() {
                      return laction;
                    }
                  }

                });

                modalInstance.result.then(
                // Success
                function(data) {
                  $scope.getWorklists(data);
                });
              };

              // Assign worklist modal
              $scope.openImportModal = function() {

                var modalInstance = $uibModal.open({
                  templateUrl : 'app/page/workflow/import.html',
                  controller : 'ImportModalCtrl',
                  backdrop : 'static',
                  resolve : {
                    selected : function() {
                      return $scope.selected;
                    },
                    lists : function() {
                      return $scope.lists;
                    },
                    user : function() {
                      return $scope.user;
                    }
                  }

                });

                modalInstance.result.then(
                // Success
                function(data) {
                  $scope.getWorklists(data);
                });
              };
              // end

            } ]
        };
      } ]);
