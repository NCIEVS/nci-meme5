// Security service
var securityUrl = 'security';
tsApp.service('securityService', [
  '$http',
  '$location',
  '$q',
  '$cookies',
  '$window',
  'utilService',
  'gpService',
  'appConfig',
  function($http, $location, $q, $cookies, $window, utilService, gpService, appConfig) {

    // Declare the user
    var user = {
      userName : null,
      password : null,
      name : null,
      email : null,
      authToken : null,
      applicationRole : null,
      userPreferences : null,
      editorLevel : null
    };

    // Search results
    var searchParams = {
      page : 1,
      query : null
    };

    var authDebugVersion = 'NM-311-popup-reauthorization-v2';

    function setAuthDebugSource(source) {
      $http.defaults.headers.common['X-UMLS-Client-Auth-Version'] = authDebugVersion;
      $http.defaults.headers.common['X-UMLS-Client-Auth-Source'] = source;
      console.debug('auth debug', authDebugVersion, source);
    }

    function ensureUserPreferences(data) {
      if (!data.userPreferences) {
        data.userPreferences = {};
      }
      if (!data.userPreferences.properties) {
        data.userPreferences.properties = {};
      }
    }

    function getCompactStoredUser() {
      var prefs = user.userPreferences || {};
      var properties = prefs.properties || {};
      return {
        userName : user.userName,
        name : user.name,
        email : user.email,
        authToken : user.authToken,
        applicationRole : user.applicationRole,
        editorLevel : user.editorLevel,
        userPreferences : {
          lastProjectId : prefs.lastProjectId,
          lastProjectRole : prefs.lastProjectRole,
          lastTerminology : prefs.lastTerminology,
          lastTab : prefs.lastTab,
          properties : {
            reportModeTab : properties.reportModeTab
          }
        }
      };
    }

    function parseStoredUser(storedUser, source) {
      if (!storedUser) {
        return null;
      }
      try {
        var parsedUser = JSON.parse(storedUser);
        if (!parsedUser || !parsedUser.authToken) {
          return null;
        }
        return {
          source : source,
          user : parsedUser
        };
      } catch (e) {
        return null;
      }
    }

    function getStoredUser() {
      var parsedUser = null;
      try {
        if ($window.localStorage) {
          parsedUser = parseStoredUser($window.localStorage.getItem('user'), 'localStorage');
        }
      } catch (e) {
        parsedUser = null;
      }
      if (parsedUser) {
        return parsedUser;
      }
      return parseStoredUser($cookies.get('user'), 'cookie');
    }

    function getWindowNameUser() {
      try {
        if (!$window.name) {
          return null;
        }
        var session = JSON.parse($window.name);
        if (!session || session.authDebugVersion !== authDebugVersion || !session.user
          || !session.user.authToken) {
          return null;
        }
        $window.name = '';
        return {
          source : 'window-name',
          user : session.user
        };
      } catch (e) {
        return null;
      }
    }

    function getOpenerServiceUser() {
      try {
        if (!$window.opener || $window.opener.closed || !$window.opener.angular
          || !$window.opener.document) {
          return null;
        }
        var openerInjector = $window.opener.angular.element($window.opener.document.documentElement)
          .injector();
        if (!openerInjector && $window.opener.document.body) {
          openerInjector = $window.opener.angular.element($window.opener.document.body).injector();
        }
        if (!openerInjector) {
          return null;
        }
        var openerSecurityService = openerInjector.get('securityService');
        if (openerSecurityService && openerSecurityService.getSessionUser) {
          return openerSecurityService.getSessionUser();
        }
        if (openerSecurityService && openerSecurityService.getUser) {
          return openerSecurityService.getUser();
        }
      } catch (e) {
        return null;
      }
      return null;
    }

    function getOpenerScopeUser() {
      try {
        if ($window.opener && !$window.opener.closed && $window.opener.$windowScope
          && $window.opener.$windowScope.user) {
          return $window.opener.$windowScope.user;
        }
      } catch (e) {
        return null;
      }
      return null;
    }

    function getOpenerUser() {
      var openerUser = getOpenerServiceUser();
      var source = 'opener-service';
      if (!openerUser) {
        openerUser = getOpenerScopeUser();
        source = 'opener-scope';
      }
      if (!openerUser || !openerUser.authToken) {
        return null;
      }
      return {
        source : source,
        user : angular.copy(openerUser)
      };
    }

    function saveStoredUser() {
      if (!user.authToken) {
        return;
      }
      try {
        if ($window.localStorage) {
          $window.localStorage.setItem('user', angular.toJson(user));
        }
      } catch (e) {
        // Fall back to the compact cookie below.
      }
      $cookies.put('user', angular.toJson(getCompactStoredUser()), { path : '/' });
    }

    setAuthDebugSource('service-init');

    // Configure tabs
    this.saveTab = function(prefs, tab) {
      if (prefs && prefs.lastTab != tab) {
        prefs.lastTab = tab;
        this.updateUserPreferences(prefs);
      }
    };

    // Configure role
    this.saveRole = function(prefs, role) {
      if (prefs && prefs.lastProjectRole != role) {
        prefs.lastProjectRole = role;
        this.updateUserPreferences(prefs);
      }
    };

    // Configure projectId
    this.saveProjectId = function(prefs, projectId) {
      if (prefs && prefs.lastProjectId != projectId) {
        prefs.lastProjectId = projectId;
        this.updateUserPreferences(prefs);
      }
    };

    // Configure role
    this.saveProjectIdAndRole = function(prefs, projectId, role) {
      if (prefs && (prefs.lastProjectId != projectId || prefs.lastProjectRole != role)) {
        prefs.lastProjectId = projectId;
        prefs.lastProjectRole = role;
        this.updateUserPreferences(prefs);
      }
    };

    // save properties
    this.saveProperty = function(prefs, key, value) {
      if (prefs && prefs.properties[key] != value) {
        prefs.properties[key] = value;
        this.updateUserPreferences(prefs);
      }
    }

    // get property
    this.getProperty = function(prefs, key, defaultValue) {
      if (prefs && prefs.properties[key]) {
        return prefs.properties[key];
      } else {
        return defaultValue;
      }
    }

    // reset user preferences
    this.resetUserPreferences = function(user) {
      user.userPreferences.properties = {};
      user.userPreferences.lastProjectId = null;
      user.userPreferences.lastProjectRole = null;
      user.userPreferences.lastTerminology = null;
      user.userPreferences.lastTab = null;
      this.updateUserPreferences(user.userPreferences);
    }

    // accepts the license
    this.acceptLicense = function() {
      var deferred = $q.defer();
      var expireDate = new Date();
      expireDate.setDate(expireDate.getDate() + 30);
      $cookies.put('WCI ' + appConfig['deploy.title'], 'license_accepted', {
        expires : expireDate
      });
      var cookie = $cookies.get('WCI ' + appConfig['deploy.title']);
      deferred.resolve();
      return deferred.promise;
    };

    // checks the license
    this.checkLicense = function() {
      var deferred = $q.defer();

      if (appConfig['deploy.license.enabled'] !== 'true') {
        deferred.resolve();
      } else {

        var cookie = $cookies.get('WCI ' + appConfig['deploy.title']);
        if (!cookie) {
          deferred.reject();
        } else {
          // refresh the cookie whenever license is checked
          this.acceptLicense();
          deferred.resolve();
        }
      }
      return deferred.promise;
    };

    // Gets the user
    this.getUser = function() {

      // if login is not enabled, set and return the Guest user
      if (appConfig['deploy.login.enabled'] === 'true'
        && appConfig['deploy.login.enabled'] !== 'true') {
        this.setGuestUser();
      }
      // otherwise, determine if user is already logged in
      else if (!$http.defaults.headers.common.Authorization) {
        var storedUser = getStoredUser();
        if (!storedUser) {
          storedUser = getWindowNameUser();
        }
        if (!storedUser) {
          storedUser = getOpenerUser();
        }
        // If there is a stored user session, load it
        if (storedUser) {
          this.setUser(storedUser.user, storedUser.source);
          $http.defaults.headers.common.Authorization = user.authToken;
        } else {
          setAuthDebugSource('none');
        }
      }
      // return user (blank if not found)
      return user;
    };

    this.getSessionUser = function() {
      if (!user.authToken) {
        return null;
      }
      return getCompactStoredUser();
    };

    // Sets the user
    this.setUser = function(data, authSource) {
      ensureUserPreferences(data);
      user.userName = data.userName;
      user.name = data.name;
      user.email = data.email;
      user.authToken = data.authToken;
      user.password = "";
      user.applicationRole = data.applicationRole;
      user.userPreferences = data.userPreferences;
      user.editorLevel = data.editorLevel;
      $http.defaults.headers.common.Authorization = data.authToken;
      setAuthDebugSource(authSource ? authSource : 'login');

      // Whenever set user is called, persist browser session state for popouts.
      saveStoredUser();
    };

    this.persistUser = function(authSource) {
      if (user.authToken) {
        setAuthDebugSource(authSource ? authSource : 'persist');
        saveStoredUser();
      }
    };

    this.openSessionWindow = function(url, title, features, authSource) {
      this.persistUser(authSource ? authSource : 'popout-open');
      var newWindow = $window.open('', title ? title : '', features ? features : '');
      if (!newWindow) {
        return null;
      }
      try {
        newWindow.name = angular.toJson({
          authDebugVersion : authDebugVersion,
          user : getCompactStoredUser()
        });
      } catch (e) {
        // Stored fallbacks above may still be available to the new window.
      }
      newWindow.location.href = url;
      return newWindow;
    };

    // Set user to the guest user
    this.setGuestUser = function() {
      user.userName = 'guest';
      user.name = 'Guest';
      user.authToken = 'guest';
      user.password = 'guest';
      user.applicationRole = 'VIEWER';
      user.userPreferences = {
        properties : {}
      };

      // Whenever set user is called, persist browser session state for popouts.
      saveStoredUser();
      $http.defaults.headers.common.Authorization = 'guest';
      setAuthDebugSource('guest');

    };

    // Determine if guest user
    this.isGuestUser = function() {
      return $http.defaults.headers.common.Authorization == 'guest';
    };

    // Clears the user
    this.clearUser = function() {
      user.userName = null;
      user.name = null;
      user.authToken = null;
      user.password = null;
      user.applicationRole = null;
      user.userPreferences = null;

      try {
        if ($window.localStorage) {
          $window.localStorage.removeItem('user');
        }
      } catch (e) {
        // Continue clearing cookies.
      }
      $cookies.remove('user', { path : '/' });
      // $cookies.remove('user');
      angular.forEach($cookies.getAll(), function (v, k) {
         $cookies.remove(k);
      });
      setAuthDebugSource('cleared');

    };

    var httpClearUser = this.clearUser;

    // isLoggedIn function
    this.isLoggedIn = function() {
      return user.authToken;
    };

    //
    // Role functions
    // Note that administrator is considered all roles
    //
    this.hasPrivilegesOf = function(role) {
      switch (role) {
      case 'ADMINISTRATOR':
        return this.isAdmin();
      case 'USER':
        return this.isUser() || this.isAdmin();
      case 'VIEWER':
        return this.isViewer() || this.isUser() || this.isAdmin();
      default:
        return true;
      }
      console.trace();
      return false;
    };

    // isAdmin function
    this.isAdmin = function() {
      return user.applicationRole === 'ADMINISTRATOR';
    };

    // isUser function
    this.isUser = function() {
      return user.applicationRole === 'USER';
    };

    // isViewer function
    this.isViewer = function() {
      return user.applicationRole === 'VIEWER';
    };
    // See permissions.js for permissions

    this.hasPermissions = function(action) {
      var userProjectRole = user.userPreferences.lastProjectRole;
      if (userProjectRole == 'AUTHOR' && user.editorLevel == 5) {
        userProjectRole = 'EDITOR5';
      }

      // console.debug('permissions', action, userProjectRole);
      return this.permissions[action][userProjectRole]
        || this.permissions[action]['APP_' + user.applicationRole];
    }

    // add a new action and roleMap to the permissions map
    this.permissions = {};
    this.addPermission = function(action, roleMap) {
      this.permissions[action] = roleMap;
    }

    // Authenticate user
    this.authenticate = function(userName, password) {

      var deferred = $q.defer();

      gpService.increment();

      // login
      $http({
        url : securityUrl + '/authenticate/' + userName,
        method : 'POST',
        data : password,
        headers : {
          'Content-Type' : 'text/plain'
        }
      }).then(function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        gpService.decrement();
        utilService.handleError(response);
        deferred.reject(response.data);
      });

      return deferred.promise;

    };

    // Logs user out
    this.logout = function() {

      var deferred = $q.defer();
      if (user.authToken == null) {
        window.alert("You are not currently logged in");
        deferred.reject('Not currently logged in');
      } else {
        gpService.increment();

        // logout
        $http.get(securityUrl + '/logout/' + user.authToken).then(
        // success
        function(response) {

          // clear scope variables
          httpClearUser();

          // clear http authorization header
          $http.defaults.headers.common.Authorization = null;
          gpService.decrement();
          deferred.resolve('Successfully logged out');

        },
        // error
        function(response) {
          utilService.handleError(response);
          gpService.decrement();
          deferred.reject('Failed to logout');
        });
        return deferred.promise;
      }
    };

    // Get user by name
    this.getUserByName = function(userName) {

      var deferred = $q.defer();

      gpService.increment();

      // logout
      $http.get(securityUrl + '/user/name/' + userName).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    }

    // get all users
    this.getUsers = function() {
      var deferred = $q.defer();

      // Get users
      gpService.increment();
      $http.get(securityUrl + '/user/users').then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get user for auth token
    this.getUserForAuthToken = function() {
      var deferred = $q.defer();

      // Get users
      gpService.increment();
      $http.get(securityUrl + '/user').then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // adds user
    this.addUser = function(user) {
      var deferred = $q.defer();

      // Add user
      gpService.increment();
      $http.put(securityUrl + '/user/add', user).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // updates user
    this.updateUser = function(user) {
      var deferred = $q.defer();

      // Add user
      gpService.increment();
      $http.post(securityUrl + '/user/update', user).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // removes user
    this.removeUser = function(id) {
      var deferred = $q.defer();

      // Add user
      gpService.increment();
      $http['delete'](securityUrl + '/user/remove/' + id).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // gets application roles
    this.getApplicationRoles = function() {
      var deferred = $q.defer();

      // Get application roles
      gpService.increment();
      $http.get(securityUrl + '/roles').then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Finds users as a list
    this.findUsersAsList = function(query, pfs) {
      // Setup deferred
      var deferred = $q.defer();

      // Make POST call
      gpService.increment();
      $http.post(securityUrl + '/user/find?query=' + utilService.prepQuery(query),
        utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });

      return deferred.promise;
    };

    //
    // User Favorites
    //

    // Create the base user favorite string, without timestamp
    function getUserFavoriteStr(component) {
      var type = component.type;
      var terminology = component.terminology;
      var version = component.version;
      var terminologyId = component.terminologyId;
      var name = component.name ? component.name : component.value;
      return type + '~~' + terminology + '~~' + version + '~~' + terminologyId + '~~' + name;
    }

    // Gets the user favorite string object without reference to name or
    // timestamp
    function getUserFavorite(component) {

      if (!user || !user.userPreferences || !user.userPreferences.favorites) {
        return null;
      }
      var delimitedStr = getUserFavoriteStr(component);

      var matchFound = false;
      for (var i = 0; i < user.userPreferences.favorites.length; i++) {
        if (user.userPreferences.favorites[i].indexOf(delimitedStr) != -1) {
          return user.userPreferences.favorites[i];
        }
      }
      return null;
    }

    // Determines whether object is in favorites (without reference to name or
    // timestamp)
    this.isUserFavorite = function(component) {
      var favorite = getUserFavorite(component);
      if (favorite) {
        return true;
      } else {
        return false;
      }

    };

    // Adds a user favorite
    this.addUserFavorite = function(component) {
      var type = component.type;
      var terminology = component.terminology;
      var version = component.version;
      var terminologyId = component.terminologyId;
      var name = component.name;

      var deferred = $q.defer();
      if (this.isGuestUser()) {
        $q.reject('Cannot add favorites for guest user');
        utilService.handleError('Guest users cannot add favorites');
      } else {
        if (!user.userPreferences || !type || !terminology || !version || !terminologyId || !name) {
          deferred.reject('Insufficient arguments');
        }
        var delimitedStr = getUserFavoriteStr(component);
        if (!user.userPreferences.favorites) {
          user.userPreferences.favorites = [];
        }

        if (!this.isUserFavorite(component)) {

          // add the timestamp after verifying this component info is not
          // matched
          user.userPreferences.favorites.push(delimitedStr + '~~' + new Date().getTime());

          this.updateUserPreferences(user.userPreferences).then(function(response) {
            deferred.resolve(response);
          }, function(response) {
            deferred.reject(response);
          });
        } else {
          deferred.reject('Favorite already exists');
        }
      }

      return deferred.promise;

    };

    // Removes a user favorite
    this.removeUserFavorite = function(component) {
      var type = component.type;
      var terminology = component.terminology;
      var version = component.version;
      var terminologyId = component.terminologyId;
      var name = component.name ? component.name : component.value;
      
      console.debug('remove user favorite', component, type, terminology, version, terminologyId, name);

      var deferred = $q.defer();
      if (!user.userPreferences || !type || !terminology || !version || !terminologyId || !name) {
        utilService.handleError({
          data : 'Unexpected error removing favorite: insufficient arguments'
        });
        deferred.reject('Unexpected error removing favorite');
      }
      var delimitedStr = getUserFavoriteStr(component);

      var matchFound = false;
      for (var i = 0; i < user.userPreferences.favorites.length; i++) {
        console.debug(user.userPreferences.favorites[i], delimitedStr)
        if (user.userPreferences.favorites[i].indexOf(delimitedStr) != -1) {
          console.debug('match found: ', user.userPreferences.favorites[i]);
          matchFound = true;
          user.userPreferences.favorites.splice(i, 1);
          break;
        }
      }
      if (matchFound) {
        this.updateUserPreferences(user.userPreferences).then(function(response) {
          deferred.resolve(response);
        }, function(response) {
          deferred.reject(response);
        });
      } else {
        utilService.handleError({
          data : 'Unexpected error removing favorite: favorite not found'
        });
        deferred.reject('Unexpected error removing favorite: favorite not found');
      }

      return deferred.promise;

    };

    // update user preferences
    this.updateUserPreferences = function(userPreferences) {
      console.debug('updateUserPreferences', userPreferences);

      var deferred = $q.defer();

      // skip if user preferences is not set
      if (!userPreferences) {
        console.log('User preferences not set');
        deferred.reject('user preferences not set');
      }

      // Skip for guest user
      else if (this.isGuestUser()) {
        console.log('Skipped updating preferences for guest user');
        deferred.reject('guest user');
      } else {
        // Whenever we update user preferences, persist session state for popouts.
        user.userPreferences = userPreferences;
        ensureUserPreferences(user);
        saveStoredUser();

        gpService.increment();
        $http.post(securityUrl + '/user/preferences/update', userPreferences).then(
        // success
        function(response) {
          gpService.decrement();
          deferred.resolve(response.data);
        },
        // error
        function(response) {
          utilService.handleError(response);
          gpService.decrement();
          deferred.reject(response.data);
        });
      }
      return deferred.promise;
    };

    // Get favorite
    this.getFavorite = function(component) {
      var type = component.type;
      var terminology = component.terminology;
      var version = component.version;
      var terminologyId = component.terminologyId;
      var name = component.name;
      return this.getUser().userPreferences.favorites.filter(function(item) {
        return item.terminology === terminology && item.terminologyId === terminologyId
          && item.version === version && item.type === type;
      }).length > 0;
    };

  } ]);
