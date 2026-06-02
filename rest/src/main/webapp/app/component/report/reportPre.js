// Preformatted report 
tsApp.directive('reportPre', [ function() {
  return {
    restrict : 'A',
    scope : {
      selected : '=',
      callbacks : '='
    },
    templateUrl : 'app/component/report/reportPre.html',
    controller : [
      '$scope',
      '$sce',
      '$element',
      'reportService',
      'securityService',
      function($scope, $sce, $element, reportService, securityService) {

        // Scope vars
        $scope.report = null;

        function persistForReportLink(event) {
          var element = event.target;
          while (element && element !== $element[0]) {
            if (element.tagName && element.tagName.toLowerCase() === 'a') {
              var href = element.getAttribute('href') || '';
              if (href.indexOf('/content/report/') != -1
                || href.indexOf('#/content/report/') != -1) {
                event.preventDefault();
                event.stopPropagation();
                securityService.openSessionWindow(href, '', 'resizable,height=800,width=600');
              }
              return;
            }
            element = element.parentNode;
          }
        }

        $element[0].addEventListener('click', persistForReportLink, true);
        $scope.$on('$destroy', function() {
          $element[0].removeEventListener('click', persistForReportLink, true);
        });

        // watch component, generate the report
        $scope.$watch('selected.component', function() {
          console.debug('selected.component1', $scope.selected.component, $scope.selected.project);
          if ($scope.selected.component) {
            $scope.getReport($scope.selected.component);
            $scope.tId = $scope.selected.component.terminologyId == $scope.selected.component.id ? 
                  '' : $scope.selected.component.terminologyId;      
          }     
        });

        // Trust as HTML
        $scope.getTrustedReport = function() {
          return $sce.trustAsHtml($scope.report);
        };

        // Get the report
        $scope.getReport = function(component) {
          $scope.report = "Loading...";
          reportService.getComponentReport(
            $scope.selected.project ? $scope.selected.project.id : null, component).then(
          // Success
          function(data) {
            $scope.report = data;
          });

        }

      } ]
  };
} ]);
