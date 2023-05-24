// Relationships
tsApp.directive('relationships', [ function() {
  console.debug('configure relationships directive');
  return {
    restrict : 'A',
    scope : {
      component : '=',
      metadata : '=',
      showHidden : '=',
      callbacks : '='
    },
    templateUrl : 'app/component/relationships/relationships.html',
    controller : [
      '$scope',
      '$window',
      'utilService',      
      function($scope, $window, utilService) {
      
        // check callbacks supplied
        if (!$scope.callbacks || !$scope.callbacks.findRelationships) {
          utilService
            .setError('Relationships directive requires callbacks.findRelationships function');
        }

        $scope.showing = true;

        // instantiate paging and paging callbacks function
        $scope.pagedData = [];
        $scope.pageSizes = utilService.getPageSizes();
        $scope.paging = utilService.getPaging();
        $scope.pageCallbacks = {
          getPagedList : getPagedList
        };
        $scope.paging.sortField = 'group';
        $scope.paging.sortAscending = true;
        $scope.paging.showInferred = false;

        // Default is Group/Type, where in getpagedData
        // relationshipType is automatically appended as a multi-
        // sort search
        $scope.paging.sortOptions = [ {
          key : 'Group, Type',
          value : 'group'
        }, {
          key : 'Type',
          value : 'relationshipType'
        }, {
          key : 'Additional Type',
          value : 'additionalRelationshipType'
        }, {
          key : 'Name',
          value : 'toName'
        } ];

        $scope.getPagedList = function() {
          getPagedList();
        }
        function getPagedList() {

          // compute the sort order
          // if group sort specified, sort additionally by relationship type
          // otherwise, sort by specified field and additionally by group
          // compute the sort order
          // always sort intelligently around relationship type and additional
          // relationship type
          var sortFields = new Array();
          if ($scope.paging.sortField === 'group') {
            sortFields = [ 'group', 'relationshipType', 'additionalRelationshipType', 'toName' ];
          } else if ($scope.paging.sortField === 'relationshipType') {
            sortFields = [ 'relationshipType', 'additionalRelationshipType', 'group', 'toName' ];
          } else if ($scope.paging.sortFied === 'additionalRelationshipType') {
            sortFields = [ 'additionalRelationshipType', 'relationshipType', 'group', 'toName' ];
          } else {
            sortFields = [ $scope.paging.sortField, 'group', 'relationshipType',
              'additionalRelationshipType', 'toName' ];
          }

          var paging = {
            page : $scope.paging.page,
            pageSize : $scope.paging.pageSize,
            showSuppressible : $scope.showHidden,
            showObsolete : $scope.showHidden,
            showInferred : $scope.paging.showInferred,
            text : $scope.paging.filter,
            sortFields : sortFields,
            sortAscending : $scope.paging.sortAscending
          };

          // Request from service
          $scope.callbacks.findRelationships($scope.component, paging).then(
          // Success
          function(data) {

            $scope.pagedData.data = data.relationships;
            $scope.pagedData.totalCount = data.totalCount;

          });
        }
        // watch the component
        $scope.$watch('component', function() {
          if ($scope.component) {
            // Reset paging
            $scope.paging.page = 1;
            $scope.paging.filter = null;

            // Get data
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

        // Function to toggle inferred flag and apply paging
        $scope.toggleInferred = function() {
          if ($scope.paging.showInferred == null || $scope.paging.showInferred == undefined) {
            $scope.paging.showInferred = false;
          } else {
            $scope.paging.showInferred = !$scope.paging.showInferred;
          }
          getPagedList();
        };
        
        // calls callback with constructed component from type and terminology triple
        $scope.getComponent = function(organizingClassType, terminologyId, terminology, version) {
          $scope.callbacks.getComponent( {
            type : organizingClassType,
            terminologyId : terminologyId,
            terminology: terminology,
            version : version
          });       
        }
        
        $scope.displayConcept = function(item) {
          var currentUrl = window.location.href;
          var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
          var newUrl = baseUrl + '/content/report/' + $scope.component.type + '/' + $scope.component.terminology
            + '/' + item.toId;
          var title = 'Report-' + $scope.component.terminology + '/' + $scope.component.version + ', '
            + $scope.component.terminologyId;
          var newWindow = $window.open(newUrl, title, 'width=500, height=600');
          newWindow.document.title = title;
          newWindow.focus();
        }

        // end controller
      } ]
  };
} ]);
