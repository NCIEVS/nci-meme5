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

  it('accepts the license with the AngularJS-compatible cookie name', () => {
    cy.visit('/license');
    cy.wait('@config');
    cy.contains('Accept License').click();

    cy.document()
      .its('cookie')
      .should('contain', 'WCI%20NCI-META%20Test=license_accepted');
  });
});
