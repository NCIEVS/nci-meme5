// Content controller
tsApp.directive('tsFooter', [
  '$rootScope',
  '$location',
  '$routeParams',
  '$sce',
  'gpService',
  'securityService',
  'utilService',
  'appConfig',
  function($rootScope, $location, $routeParams, $sce, gpService, securityService, utilService,
    appConfig) {
    console.debug('configure footer directive');
    return {
      restrict : 'A',
      scope : {},
      templateUrl : 'app/page/footer/footer.html',
      link : function(scope, element, attrs) {

        scope.isShowing = function() {
          if (!utilService.isHeaderFooterShowing()) {
            return false;
          } else {
            return true;
          }
        };

        // pass values to scope
        scope.appConfig = appConfig;

        // Convert to trusted HTML
        scope.deployPresentedBy = function() {
          return $sce.trustAsHtml(scope.appConfig['deploy.presented.by']);
        }

        // Site tracking code
        scope.siteTrackingCode = function() {
          return $sce.trustAsHtml(scope.appConfig['deploy.tracking.code']);
        }

        // Site tracking code
        scope.siteCookieCode = function() {
          return $sce.trustAsHtml(scope.appConfig['deploy.cookie.code']);
        }

        // Declare user
        scope.user = securityService.getUser();

        // Logout method
        scope.logout = function() {
          if (scope.appConfig['deploy.login.enabled'] === 'true') {
            securityService.logout();
          } else {
            securityService.clearUser();
          }
          $location.path('/');
        };

        // Check gp status
        scope.isGlassPaneNegative = function() {
          return gpService.isGlassPaneNegative();
        };

        // Get gp counter
        scope.getGlassPaneCounter = function() {
          return gpService.getGlassPane().counter;
        };

        // get truncated version (no dashed content)
        scope.getTruncatedVersion = function(version) {
          if (version && version.indexOf('-') != -1) {
            return version.substring(0, version.indexOf('-'));
          } else
            return version;
        };
      }
    };
  } ]);
