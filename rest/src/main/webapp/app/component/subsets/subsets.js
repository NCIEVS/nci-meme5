// Subsets
tsApp.directive('subsets', [ function() {
  console.debug('configure subsets directive');
  return {
    restrict : 'A',
    scope : {
      component : '=',
      metadata : '=',
      showHidden : '=',
      callbacks : '='
    },
    templateUrl : 'app/component/subsets/subsets.html',
    controller : [ '$scope', 'utilService', function($scope, utilService) {
      $scope.showing = true;

      // instantiate paging and paging callbacks function
      $scope.pagedData = [];
      $scope.pageSizes = utilService.getPageSizes();
      $scope.paging = utilService.getPaging();
      $scope.pageCallbacks = {
        getPagedList : getPagedList
      };

      // watch the component
      $scope.$watch('component', function() {
        if ($scope.component) {
          // Clear paging
          $scope.paging = utilService.getPaging();
          $scope.pageCallbacks = {
            getPagedList : getPagedList
          };
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

      // get paged list
      function getPagedList() {

        $scope.pagedData = utilService.getPagedArray($scope.component.members.filter(
        // handle hidden flag
        function(item) {
          return $scope.paging.showHidden || (!item.obsolete && !item.suppressible);
        }), $scope.paging);
        console.debug('subsets', $scope.pagedData);
      }

      // end controller
    } ]
  };
} ]);
