describe('Angular 20 shell', () => {
  const config = {
    'base.url': 'http://localhost:8080/umls-server-rest',
    'deploy.enabled.tabs': 'content,metadata,admin',
    'deploy.feedback.email': 'info@example.com',
    'deploy.footer.copyright': 'Copyright @2026',
    'deploy.landing.enabled': 'true',
    'deploy.license.enabled': 'true',
    'deploy.link': 'http://www.westcoastinformatics.com',
    'deploy.login.enabled': 'true',
    'deploy.password.reset': 'http://passwordreset.example.com',
    'deploy.presented.by': 'Presented by Test',
    'deploy.title': 'NCI-META Test'
  };

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
    cy.intercept('GET', '/umls-server-rest/configure/properties', config).as(
      'config'
    );
  });

  it('loads config and renders enabled tabs', () => {
    cy.visit('/');

    cy.wait('@config');
    cy.contains('NCI-META Test').should('be.visible');
    cy.contains('Content').should('be.visible');
    cy.contains('Metadata').should('be.visible');
    cy.contains('Admin').should('be.visible');
  });

  it('logs in, persists the compatible user session, and logs out', () => {
    cy.intercept('POST', '/umls-server-rest/security/authenticate/dss', {
      applicationRole: 'ADMINISTRATOR',
      authToken: 'DSS',
      editorLevel: 5,
      name: 'Deborah Shapiro',
      userName: 'dss',
      userPreferences: {
        lastTab: '/admin',
        properties: {}
      }
    }).as('login');

    cy.intercept('GET', '/umls-server-rest/security/logout/DSS', {}).as('logout');

    cy.visit('/login');
    cy.wait('@config');
    cy.get('#userField').type('dss');
    cy.get('#passwordField').type('secret');
    cy.get('#userLoginButton').click();
    cy.wait('@login');

    cy.window()
      .its('localStorage.user')
      .should('contain', '"authToken":"DSS"');
    cy.getCookie('user').should('exist');

    cy.contains('Logout').click();
    cy.wait('@logout');
    cy.window().its('localStorage.user').should('be.undefined');
  });

  it('falls back from inaccessible project-role tabs to the first accessible tab', () => {
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'edit,admin',
      'deploy.license.enabled': 'false'
    }).as('projectTabConfig');

    cy.visit('/edit', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
          JSON.stringify({
            applicationRole: 'ADMINISTRATOR',
            authToken: 'DSS',
            name: 'Deborah Shapiro',
            userName: 'dss',
            userPreferences: {
              lastTab: '/edit',
              properties: {}
            }
          })
        );
      }
    });

    cy.wait('@projectTabConfig');
    cy.location('pathname').should('equal', '/admin');
    cy.contains('Enabled Tab').should('be.visible');
    cy.contains('Admin').should('be.visible');
  });

  it('ignores stored guest sessions when login is enabled', () => {
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'terminology,admin',
      'deploy.license.enabled': 'false',
      'deploy.login.enabled': 'true'
    }).as('loginRequiredConfig');

    cy.visit('/terminology', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
          JSON.stringify({
            applicationRole: 'VIEWER',
            authToken: 'guest',
            name: 'Guest',
            userName: 'guest',
            userPreferences: {
              properties: {}
            }
          })
        );
      }
    });

    cy.wait('@loginRequiredConfig');
    cy.location('pathname').should('equal', '/login');
    cy.window().its('localStorage.user').should('be.undefined');
  });

  it('clears stale stored tokens when the backend rejects the session', () => {
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'terminology,admin',
      'deploy.license.enabled': 'false',
      'deploy.login.enabled': 'true'
    }).as('staleSessionConfig');

    cy.intercept('GET', '/umls-server-rest/metadata/terminology/current', {
      statusCode: 500,
      body: 'AuthToken does not have a valid username.'
    }).as('terminologyAuthFailure');

    cy.visit('/terminology', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
          JSON.stringify({
            applicationRole: 'VIEWER',
            authToken: 'stale-token',
            name: 'Stale User',
            userName: 'stale',
            userPreferences: {
              properties: {}
            }
          })
        );
      }
    });

    cy.wait(['@staleSessionConfig', '@terminologyAuthFailure']);
    cy.location('pathname').should('equal', '/login');
    cy.window().its('localStorage.user').should('be.undefined');
  });

  it('renders the terminology read-only feature slice', () => {
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'terminology,admin',
      'deploy.license.enabled': 'false',
      'deploy.login.enabled': 'false'
    }).as('terminologyConfig');

    cy.intercept('GET', '/umls-server-rest/metadata/terminology/current', {
      terminologies: [
        {
          id: 1,
          terminology: 'NCI',
          version: '2026_05',
          preferredName: 'NCI Thesaurus',
          organizingClassType: 'CONCEPT',
          citation: {
            title: 'NCI Thesaurus',
            publisher: 'National Cancer Institute'
          }
        },
        {
          id: 2,
          terminology: 'SNOMEDCT_US',
          version: '2026_03',
          preferredName: 'SNOMED CT US Edition',
          organizingClassType: 'CONCEPT'
        }
      ]
    }).as('terminologies');

    cy.intercept('GET', '/umls-server-rest/metadata/rootTerminology/NCI', {
      terminology: 'NCI',
      restrictionLevel: 0,
      language: 'ENG',
      acquisitionContact: {
        organization: 'NCI'
      },
      contentContact: {
        name: 'Content Team',
        email: 'content@example.com'
      },
      licenseContact: {
        name: 'License Team'
      }
    }).as('rootTerminology');

    cy.visit('/terminology');
    cy.wait(['@terminologyConfig', '@terminologies', '@rootTerminology']);

    cy.contains('Read-Only Feature Slice').should('be.visible');
    cy.contains('NCI Thesaurus').should('be.visible');
    cy.contains('SNOMED CT US Edition').should('be.visible');
    cy.contains('Restriction Level').should('be.visible');
    cy.contains('ENG').should('be.visible');
    cy.get('#terminology-filter').type('snomed');
    cy.get('tbody').should('contain', 'SNOMED CT US Edition');
    cy.get('tbody').should('not.contain', 'NCI Thesaurus');
  });

  it('accepts the license with the AngularJS-compatible cookie name', () => {
    cy.visit('/license');
    cy.wait('@config');
    cy.contains('Accept License').click();

    cy.document()
      .its('cookie')
      .should('contain', 'WCI%20NCI-META%20Test=license_accepted');
  });
});
