// Atoms directive
tsApp.directive('atoms', [ function() {
  return {
    restrict : 'A',
    scope : {
      selected : '=',
      lists : '=',
      showHidden : '=',
      callbacks : '='
    },
    templateUrl : 'app/component/atoms/atoms.html',
    controller : [
      '$scope',
      '$window',
      '$uibModal',
      'utilService',
      'contentService',
      'editService',
      'appConfig',
      function($scope, $window, $uibModal, utilService, contentService, editService, appConfig) {

        $scope.appConfig = appConfig;
        $scope.expanded = {};
        $scope.showing = true;
        $scope.getPagedList = function() {
          getPagedList();
        }

        // Paging function
        function getPagedList() {
          $scope.pagedData = utilService.getPagedArray($scope.selected.component.atoms.filter(
          // handle hidden flag
          function(item) {
            return $scope.paging.showHidden || (!item.obsolete && !item.suppressible);
          }), $scope.paging);

        }

        // instantiate paging and paging callbacks function
        $scope.pagedData = [];
        $scope.pageSizes = utilService.getPageSizes();
        $scope.paging = utilService.getPaging();
        $scope.paging.pageSize = 100000;
        $scope.pageCallbacks = {
          getPagedList : getPagedList
        };

        // watch the component
        $scope.$watch('selected.component', function() {
          if ($scope.selected.component) {
            // reset paging
            // commented out - interferes with Show All/Show Paged
            // $scope.paging = utilService.getPaging();
            $scope.pageCallbacks = {
              getPagedList : getPagedList
            };
            // get data
            getPagedList();
          }
        }, true);

        // watch show hidden flag
        $scope.$watch('showHidden', function(newValue, oldValue) {
          $scope.paging.showHidden = $scope.showHidden;

          // if value changed, get paged list
          if (newValue != oldValue) {
            getPagedList();
          }
        });

        // toggle an items collapsed state
        $scope.toggleItemCollapse = function(item) {
          $scope.expanded[item.id] = !$scope.expanded[item.id];
        };

        // get the collapsed state icon
        $scope.getCollapseIcon = function(item) {

          // if no expandable content detected, return blank glyphicon
          // (see
          // tsApp.css)
          if (!contentService.atomHasContent(item))
            return 'glyphicon glyphicon-plus glyphicon-plus nocontent';

          // return plus/minus based on current expanded status
          if ($scope.expanded[item.id])
            return 'noul glyphicon glyphicon-minus';
          else
            return 'noul glyphicon glyphicon-plus';
        };

        // Determine if the atom has a prject"new atom termgroup"
        $scope.isNewAtomTermgroup = function(atom) {
          if (!$scope.selected.project) {
            return false;
          }
          for (var i = 0; i < $scope.selected.project.newAtomTermgroups.length; i++) {
            var tg = $scope.selected.project.newAtomTermgroups[i];
            if (atom.terminology + '/' + atom.termType == tg) {
              return true;
            }
          }
          return false;
        }

        // Remove an atom
        $scope.removeAtom = function(atom) {
          editService.removeAtom($scope.selected.project.id, $scope.selected.component.id, atom.id)
            .then(
            // success
            function(data) {
              $scope.callbacks.getComponent($scope.selected.component);
            });
        }

        //
        // MODALS
        //
        // Add atom modal
        $scope.openAddAtomModal = function(latom) {

          var modalInstance = $uibModal.open({
            templateUrl : 'app/page/content/editAtom.html',
            backdrop : 'static',
            controller : 'SimpleAtomModalCtrl',
            resolve : {
              atom : function() {
                return null;
              },
              action : function() {
                return 'Add';
              },
              selected : function() {
                return $scope.selected;
              },
              lists : function() {
                return $scope.lists;
              }
            }
          });

          modalInstance.result.then(
          // Success
          function(user) {
            $scope.callbacks.getComponent($scope.selected.component);
          });
        };
        // Edit atom modal
        $scope.openEditAtomModal = function(latom) {

          var modalInstance = $uibModal.open({
            templateUrl : 'app/page/content/editAtom.html',
            backdrop : 'static',
            controller : 'SimpleAtomModalCtrl',
            resolve : {
              atom : function() {
                return latom;
              },
              action : function() {
                return 'Edit';
              },
              selected : function() {
                return $scope.selected;
              },
              lists : function() {
                return $scope.lists;
              }
            }
          });

          modalInstance.result.then(
          // Success
          function(user) {
            $scope.callbacks.getComponent($scope.selected.component);
          });
        };
        
        
        $scope.openCodeConceptsWindow = function(atom, width, height) {

          var newUrl = utilService.composeUrl('/edit/codeConcepts');
          $scope.selected.linkedAtom = atom;
          window.$windowScope = $scope;

          $scope.windows['codeConcepts'] = $window.open(newUrl, 'codeConceptsWindow', 'width=600, height=600, scrollbars=yes');
          $scope.windows['codeConcepts'].document.title = 'Code Concepts Reference';
          $scope.windows['codeConcepts'].focus();
         
        };

        // end controller
      } ]
  };
} ]);
