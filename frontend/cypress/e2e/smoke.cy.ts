describe('Angular 20 shell', () => {
  const config = {
    'base.url': 'http://localhost:8080/umls-server-rest',
    'deploy.enabled.tabs': 'content,source,terminology,metadata,admin',
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
    cy.contains('Admin').should('be.visible');
    cy.get('nav[aria-label="Enabled tabs"]').should('not.contain', 'Sources');
    cy.get('nav[aria-label="Enabled tabs"]').should('not.contain', 'Terminology');
    cy.get('nav[aria-label="Enabled tabs"]').should('not.contain', 'Metadata');
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
    let dssAddedProject = false;

    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'edit,admin',
      'deploy.license.enabled': 'false'
    }).as('projectTabConfig');

    cy.intercept('GET', '/umls-server-rest/security/roles', {
      objects: ['VIEWER', 'USER', 'ADMINISTRATOR'],
      totalCount: 3
    }).as('applicationRoles');

    cy.intercept('GET', '/umls-server-rest/project/roles', {
      objects: ['AUTHOR', 'REVIEWER', 'ADMINISTRATOR'],
      totalCount: 3
    }).as('projectRoles');

    cy.intercept('GET', '/umls-server-rest/project/checks', {
      keyValuePairs: [
        {
          key: 'DEFAULT',
          value: 'Default validation check'
        },
        {
          key: 'REL',
          value: 'Relationship validation check'
        }
      ]
    }).as('dssValidationChecks');

    cy.intercept('GET', '/umls-server-rest/metadata/terminology/current', {
      terminologies: [
        {
          current: true,
          terminology: 'NCIMTH',
          version: 'latest'
        }
      ],
      totalCount: 1
    }).as('dssProjectTerminologies');

    cy.intercept('GET', '/umls-server-rest/metadata/all/NCIMTH/latest', {
      keyValuePairLists: [
        {
          name: 'Languages',
          keyValuePairs: [
            {
              key: 'ENG',
              value: 'English'
            }
          ]
        }
      ]
    }).as('dssProjectLanguages');

    cy.intercept('GET', '/umls-server-rest/workflow/paths', {
      strings: ['DEFAULT', 'DSS', 'ADMIN', 'UPDATED']
    }).as('dssWorkflowPaths');

    cy.intercept('POST', '/umls-server-rest/project/find?query=', (request) => {
      request.reply({
        projects: [
          {
            id: 1,
            name: 'NCI-META Editing',
            description: 'Project for NCI-META Editing',
            terminology: 'NCIMTH',
            version: 'latest',
            userRoleMap: {
              DSS: 'ADMINISTRATOR'
            }
          },
          ...(dssAddedProject
            ? [
                {
                  id: 7,
                  name: 'DSS New Project',
                  description: 'Added by DSS',
                  editingEnabled: true,
                  language: 'ENG',
                  terminology: 'NCIMTH',
                  validationChecks: ['DEFAULT'],
                  version: 'latest',
                  workflowPath: 'DSS',
                  userRoleMap: {
                    dss: 'ADMINISTRATOR'
                  }
                }
              ]
            : [])
        ],
        totalCount: dssAddedProject ? 2 : 1
      });
    }).as('projects');

    cy.intercept('PUT', '/umls-server-rest/project/', (request) => {
      expect(request.body).to.include({
        description: 'Added by DSS',
        editingEnabled: true,
        language: 'ENG',
        name: 'DSS New Project',
        terminology: 'NCIMTH',
        version: 'latest',
        workflowPath: 'DSS'
      });
      expect(request.body.validationChecks).to.deep.equal(['DEFAULT']);
      request.reply({
        id: 7,
        ...request.body,
        userRoleMap: {}
      });
    }).as('dssAddProject');

    cy.intercept(
      'GET',
      '/umls-server-rest/project/assign?projectId=7&userName=dss&role=ADMINISTRATOR',
      (request) => {
        dssAddedProject = true;
        request.reply({
          id: 7,
          name: 'DSS New Project',
          description: 'Added by DSS',
          editingEnabled: true,
          language: 'ENG',
          terminology: 'NCIMTH',
          validationChecks: ['DEFAULT'],
          version: 'latest',
          workflowPath: 'DSS',
          userRoleMap: {
            dss: 'ADMINISTRATOR'
          }
        });
      }
    ).as('dssAssignCreator');

    cy.intercept('POST', '/umls-server-rest/security/user/preferences/update', (request) => {
      expect(request.body).to.include({
        lastProjectId: 7,
        userName: 'dss'
      });
      request.reply(request.body);
    }).as('dssUpdatePreferences');

    cy.intercept('POST', '/umls-server-rest/security/user/find?query=', {
      users: [
        {
          id: 1,
          userName: 'DSS',
          name: 'Deborah Shapiro',
          email: 'DSS@example.com',
          applicationRole: 'USER',
          projectRoleMap: {
            '1': 'ADMINISTRATOR'
          }
        }
      ],
      totalCount: 1
    }).as('users');

    cy.visit('/edit', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
          JSON.stringify({
            applicationRole: 'USER',
            authToken: 'DSS',
            name: 'Deborah Shapiro',
            id: 11,
            projectRoleMap: {
              '1': 'ADMINISTRATOR'
            },
            userName: 'dss',
            userPreferences: {
              id: 22,
              lastTab: '/edit',
              userName: 'dss',
              properties: {}
            }
          })
        );
      }
    });

    cy.wait('@projectTabConfig');
    cy.location('pathname').should('equal', '/admin');
    cy.wait([
      '@applicationRoles',
      '@projectRoles',
      '@dssValidationChecks',
      '@dssProjectTerminologies',
      '@dssWorkflowPaths',
      '@projects',
      '@users'
    ]);
    cy.contains('Admin Foundation').should('be.visible');
    cy.contains('NCI-META Editing').should('be.visible');
    cy.contains('Deborah Shapiro').should('be.visible');
    cy.contains('Admin').should('be.visible');
    cy.get('aside[aria-label="Selected project details"]').within(() => {
      cy.contains('button', 'Edit').should('be.visible');
    });
    cy.get('section[aria-labelledby="projects-title"]').within(() => {
      cy.contains('button', 'Add Project').click();
    });
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Add Project');
    cy.get('#project-form-name').type('DSS New Project');
    cy.get('#project-form-description').type('Added by DSS');
    cy.get('#project-form-terminology').type('NCIMTH');
    cy.get('#project-form-version').should('have.value', 'latest');
    cy.wait('@dssProjectLanguages');
    cy.get('#project-form-workflow-path').select('DSS');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@dssAddProject');
    cy.wait('@dssAssignCreator');
    cy.wait('@dssUpdatePreferences');
    cy.wait('@projects');
    cy.contains('Project added.').should('be.visible');
    cy.contains('DSS New Project').should('be.visible');
    cy.contains('Assign Project').should('not.exist');
  });

  it('supports the admin project, user, and project-role mutation slices', () => {
    let assignedProjectRole: string | null = null;
    let addedUserExists = false;
    let adminPreferences = {
      id: 31,
      feedbackEmail: 'admin@example.com',
      lastProjectId: 5,
      lastProjectRole: 'ADMINISTRATOR',
      lastTab: '/admin',
      properties: {
        adminGroups: '[]'
      },
      userName: 'admin'
    };
    let projectAutomationsEnabled = false;
    let projectDescription = 'Project for NCI-META Editing';
    let projectFeedbackEmail = 'meta@example.com';
    let projectName = 'NCI-META Editing';
    let projectPrecedenceEntries = [
      {
        key: 'NCIMTH',
        value: 'PT'
      },
      {
        key: 'NCIMTH',
        value: 'SY'
      },
      {
        key: 'SNOMEDCT_US',
        value: 'PT'
      }
    ];
    let projectValidationChecks = ['DEFAULT'];
    let projectValidationData = [
      {
        id: 10,
        key: 'OLD',
        type: 'DEFAULT',
        value: 'old-value'
      }
    ];
    let projectWorkflowPath = 'DEFAULT';
    let addProjectCalls = 0;
    let projectDeleted = false;
    let userDeleted = false;
    const confirmationMessages: string[] = [];

    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'admin',
      'deploy.license.enabled': 'false'
    }).as('adminConfig');

    cy.intercept('GET', '/umls-server-rest/security/roles', {
      objects: ['VIEWER', 'USER', 'ADMINISTRATOR'],
      totalCount: 3
    }).as('adminApplicationRoles');

    cy.intercept('GET', '/umls-server-rest/project/roles', {
      objects: ['AUTHOR', 'REVIEWER', 'ADMINISTRATOR'],
      totalCount: 3
    }).as('adminProjectRoles');

    cy.intercept('GET', '/umls-server-rest/project/checks', {
      keyValuePairs: [
        {
          key: 'DEFAULT',
          value: 'Default validation check'
        },
        {
          key: 'REL',
          value: 'Relationship validation check'
        }
      ]
    }).as('adminValidationChecks');

    cy.intercept('GET', '/umls-server-rest/metadata/terminology/current', {
      terminologies: [
        {
          current: true,
          terminology: 'NCIMTH',
          version: 'latest'
        }
      ],
      totalCount: 1
    }).as('adminProjectTerminologies');

    cy.intercept('GET', '/umls-server-rest/metadata/all/NCIMTH/latest', {
      keyValuePairLists: [
        {
          name: 'Languages',
          keyValuePairs: [
            {
              key: 'ENG',
              value: 'English'
            }
          ]
        }
      ]
    }).as('adminProjectLanguagesLatest');

    cy.intercept('GET', '/umls-server-rest/metadata/all/NCIMTH/next', {
      keyValuePairLists: [
        {
          name: 'Languages',
          keyValuePairs: [
            {
              key: 'ENG',
              value: 'English'
            }
          ]
        }
      ]
    }).as('adminProjectLanguagesNext');

    cy.intercept('GET', '/umls-server-rest/workflow/paths', {
      strings: ['DEFAULT', 'DSS', 'ADMIN', 'UPDATED']
    }).as('adminWorkflowPaths');

    cy.intercept('GET', '/umls-server-rest/metadata/precedence/42', (request) => {
      request.reply({
        id: 42,
        name: 'NCI-META precedence',
        precedence: {
          keyValuePairs: projectPrecedenceEntries
        },
        terminology: 'NCIMTH',
        version: 'latest'
      });
    }).as('adminPrecedence');

    cy.intercept('POST', '/umls-server-rest/project/find?query=', (request) => {
      request.reply({
        projects: projectDeleted
          ? []
          : [
              {
                id: 5,
                name: projectName,
                automationsEnabled: projectAutomationsEnabled,
                description: projectDescription,
                editingEnabled: true,
                feedbackEmail: projectFeedbackEmail,
                language: 'ENG',
                precedenceListId: 42,
                terminology: 'NCIMTH',
                validationChecks: projectValidationChecks,
                validationData: projectValidationData,
                version: 'latest',
                workflowPath: projectWorkflowPath,
                userRoleMap: assignedProjectRole ? { admin: assignedProjectRole } : {}
              }
            ],
        totalCount: projectDeleted ? 0 : 1
      });
    }).as('adminProjects');

    cy.intercept('POST', '/umls-server-rest/security/user/find?query=', (request) => {
      request.reply({
        users: [
          {
            id: 1,
            userName: 'admin',
            name: 'Admin',
            email: 'admin@example.com',
            editorLevel: 0,
            applicationRole: 'ADMINISTRATOR',
            projectRoleMap: assignedProjectRole ? { '5': assignedProjectRole } : {}
          },
          ...(addedUserExists && !userDeleted
            ? [
                {
                  id: 2,
                  userName: 'new-user',
                  name: 'New User',
                  email: 'new-user@example.com',
                  editorLevel: 0,
                  applicationRole: 'USER',
                  projectRoleMap: {}
                }
              ]
            : [])
        ],
        totalCount: addedUserExists && !userDeleted ? 2 : 1
      });
    }).as('adminUsers');

    cy.intercept('PUT', '/umls-server-rest/security/user/add', (request) => {
      expect(request.body).to.include({
        applicationRole: 'USER',
        editorLevel: 0,
        email: 'new-user@example.com',
        name: 'New User',
        userName: 'new-user'
      });
      addedUserExists = true;
      userDeleted = false;
      request.reply({
        id: 2,
        ...request.body
      });
    }).as('addUser');

    cy.intercept('DELETE', '/umls-server-rest/security/user/remove/2', (request) => {
      userDeleted = true;
      request.reply({});
    }).as('removeUser');

    cy.intercept('POST', '/umls-server-rest/security/user/preferences/update', (request) => {
      expect(request.body).to.include({
        id: 31,
        userName: 'admin'
      });
      adminPreferences = request.body;
      request.reply(adminPreferences);
    }).as('updateAdminPreferences');

    cy.intercept('POST', '/umls-server-rest/project/reload', {}).as('reloadConfig');

    cy.intercept('POST', '/umls-server-rest/project/exception', {
      statusCode: 500,
      body: 'TEST EXCEPTION'
    }).as('forceException');

    cy.intercept('POST', '/umls-server-rest/project/exception?local=true', {
      statusCode: 500,
      body: 'TEST LOCAL EXCEPTION'
    }).as('forceLocalException');

    cy.intercept('PUT', '/umls-server-rest/project/', (request) => {
      addProjectCalls += 1;

      expect(request.body).to.include({
        automationsEnabled: false,
        description: 'Added by app admin',
        editingEnabled: true,
        language: 'ENG',
        name: 'Admin Added Project',
        terminology: 'NCIMTH',
        version: 'next',
        workflowPath: 'ADMIN'
      });
      expect(request.body.validationChecks).to.deep.equal(['DEFAULT']);
      expect(request.body.validationData).to.deep.equal([]);

      request.reply({
        id: 8,
        ...request.body,
        userRoleMap: {}
      });
    }).as('addProject');

    cy.intercept('POST', '/umls-server-rest/security/user/update', (request) => {
      expect(request.body).to.include({
        applicationRole: 'USER',
        editorLevel: 5,
        userName: 'admin'
      });
      request.reply({});
    }).as('updateUser');

    cy.intercept('POST', '/umls-server-rest/project/', (request) => {
      expect(request.body).to.include({
        automationsEnabled: true,
        description: 'Updated project description',
        editingEnabled: true,
        feedbackEmail: 'updated-meta@example.com',
        id: 5,
        name: 'Updated NCI-META Editing',
        workflowPath: 'UPDATED'
      });
      expect(request.body.validationChecks).to.deep.equal(['REL']);
      expect(request.body.validationData).to.deep.equal([
        {
          key: 'NCI',
          type: 'REL',
          value: 'NCIt'
        }
      ]);

      projectAutomationsEnabled = request.body.automationsEnabled;
      projectDescription = request.body.description;
      projectFeedbackEmail = request.body.feedbackEmail;
      projectName = request.body.name;
      projectValidationChecks = request.body.validationChecks;
      projectValidationData = request.body.validationData;
      projectWorkflowPath = request.body.workflowPath;

      request.reply({});
    }).as('updateProject');

    cy.intercept('POST', '/umls-server-rest/metadata/precedence', (request) => {
      expect(request.body).to.include({
        id: 42,
        name: 'NCI-META precedence',
        terminology: 'NCIMTH',
        version: 'latest'
      });
      expect(request.body.precedence.keyValuePairs).to.deep.equal([
        {
          key: 'NCIMTH',
          value: 'SY'
        },
        {
          key: 'NCIMTH',
          value: 'PT'
        },
        {
          key: 'SNOMEDCT_US',
          value: 'PT'
        }
      ]);
      projectPrecedenceEntries = request.body.precedence.keyValuePairs;
      request.reply({});
    }).as('updatePrecedence');

    cy.intercept('DELETE', '/umls-server-rest/project/5', (request) => {
      projectDeleted = true;
      request.reply({});
    }).as('removeProject');

    cy.intercept(
      'GET',
      '/umls-server-rest/project/assign?projectId=5&userName=admin&role=REVIEWER',
      (request) => {
        assignedProjectRole = 'REVIEWER';
        request.reply({
          id: 5,
          name: 'NCI-META Editing',
          description: 'Project for NCI-META Editing',
          terminology: 'NCIMTH',
          userRoleMap: {
            admin: 'REVIEWER'
          }
        });
      }
    ).as('assignProjectRole');

    cy.intercept(
      'GET',
      '/umls-server-rest/project/unassign?projectId=5&userName=admin',
      (request) => {
        assignedProjectRole = null;
        request.reply({
          id: 5,
          name: 'NCI-META Editing',
          description: 'Project for NCI-META Editing',
          terminology: 'NCIMTH',
          userRoleMap: {}
        });
      }
    ).as('unassignProjectRole');

    cy.visit('/admin', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
	          JSON.stringify({
	            applicationRole: 'ADMINISTRATOR',
	            authToken: 'admin',
	            id: 1,
	            name: 'Admin',
	            userName: 'admin',
	            userPreferences: adminPreferences
	          })
	        );
      }
    });

    cy.wait([
      '@adminConfig',
      '@adminApplicationRoles',
      '@adminProjectRoles',
      '@adminValidationChecks',
      '@adminProjectTerminologies',
      '@adminWorkflowPaths'
    ]);
    cy.wait(['@adminProjects', '@adminUsers']);

    cy.on('window:confirm', (message) => {
      confirmationMessages.push(message);
      return true;
    });

    cy.get('section[aria-labelledby="preferences-title"]').within(() => {
      cy.get('#user-preference-feedback-email').clear();
      cy.get('#user-preference-feedback-email').type('ops@example.com');
      cy.contains('button', 'Save').click();
    });
    cy.wait('@updateAdminPreferences');
    cy.contains('User preferences saved.').should('be.visible');

    cy.get('section[aria-labelledby="preferences-title"]').within(() => {
      cy.contains('button', 'Reset Preferences').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Reset user preferences to defaults?'
      );
    });
    cy.wait('@updateAdminPreferences');
    cy.contains('User preferences reset.').should('be.visible');

    cy.get('section[aria-labelledby="operations-title"]').within(() => {
      cy.contains('button', 'Reload Config').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to reload the configuration?'
      );
    });
    cy.wait('@reloadConfig');
    cy.wait(['@adminProjectTerminologies', '@adminWorkflowPaths', '@adminValidationChecks']);
    cy.wait(['@adminProjects', '@adminUsers']);
    cy.contains('Configuration reloaded.').should('be.visible');

    cy.get('section[aria-labelledby="operations-title"]').within(() => {
      cy.contains('button', 'Force Exception').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to force an exception?'
      );
    });
    cy.wait('@forceException');
    cy.contains('TEST EXCEPTION').should('be.visible');

    cy.get('section[aria-labelledby="operations-title"]').within(() => {
      cy.contains('button', 'Force Test Exception').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to force a test exception?'
      );
    });
    cy.wait('@forceLocalException');
    cy.contains('TEST LOCAL EXCEPTION').should('be.visible');

    cy.get('section[aria-labelledby="projects-title"]').within(() => {
      cy.contains('button', 'Add Project').click();
    });
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Add Project');
    cy.get('#project-form-name').type('Admin Added Project');
    cy.get('#project-form-description').type('Added by app admin');
    cy.get('#project-form-terminology').type('NCIMTH');
    cy.get('#project-form-version').should('have.value', 'latest');
    cy.wait('@adminProjectLanguagesLatest');
    cy.get('#project-form-version').clear();
    cy.get('#project-form-version').type('next');
    cy.get('#project-form-version').blur();
    cy.wait('@adminProjectLanguagesNext');
    cy.get('.user-form button[type="submit"]').click();
    cy.contains(
      'The name, description, terminology, version, and workflow path fields cannot be blank.'
    ).should('be.visible');
    cy.wrap(null).should(() => {
      expect(addProjectCalls).to.equal(0);
    });
    cy.get('#project-form-workflow-path').select('ADMIN');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@addProject');
    cy.wait('@adminProjects');
    cy.contains('Project added.').should('be.visible');

    cy.get('aside[aria-label="Selected project details"]').within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Edit Project');
    cy.wait('@adminPrecedence');
    cy.get('#project-form-name').clear();
    cy.get('#project-form-name').type('Updated NCI-META Editing');
    cy.get('#project-form-description').clear();
    cy.get('#project-form-description').type('Updated project description');
    cy.get('#project-form-feedback-email').clear();
    cy.get('#project-form-feedback-email').type('updated-meta@example.com');
    cy.get('#project-form-workflow-path').select('UPDATED');
    cy.get('#project-form-automations-enabled').check();
    cy.contains('.check-list li', 'Relationship validation check').within(() => {
      cy.contains('button', 'Add').click();
    });
    cy.contains('.check-list li', 'Default validation check').within(() => {
      cy.contains('button', 'Remove').click();
    });
    cy.get('section[aria-label="Validation data"]').within(() => {
      cy.contains('td', 'OLD').should('be.visible');
      cy.contains('button', 'Add').click();
    });
    cy.get('form[aria-label="Validation data form"]').within(() => {
      cy.get('#validation-data-type').select('REL');
      cy.get('#validation-data-key').type('NCI');
      cy.get('#validation-data-value').type('NCIt');
      cy.contains('button', 'Add').click();
    });
    cy.get('section[aria-label="Validation data"]').within(() => {
      cy.contains('td', 'NCI').should('be.visible');
      cy.contains('tr', 'OLD').within(() => {
        cy.contains('button', 'Remove').click();
      });
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to remove this?'
      );
    });
    cy.get('section[aria-label="Precedence list"]').within(() => {
      cy.contains('tr', 'SY').within(() => {
        cy.contains('button', 'Up').click();
      });
      cy.get('tbody tr').first().should('contain.text', 'SY');
      cy.contains('button', 'Save').click();
    });
    cy.wait('@updatePrecedence');
    cy.contains('Precedence list saved.').should('be.visible');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@updateProject');
    cy.wait('@adminProjects');
    cy.contains('Project updated.').should('be.visible');
    cy.contains('Updated NCI-META Editing').should('be.visible');

    cy.get('#project-filter').type('NCI{enter}');
    cy.wait('@adminProjects');
    cy.get('#project-filter').clear();
    cy.wait('@adminProjects');

    cy.get('#user-filter').type('admin{enter}');
    cy.wait('@adminUsers');
    cy.get('#user-filter').clear();
    cy.wait('@adminUsers');

    cy.contains('Assign Project').click();
    cy.get('[role="dialog"]')
      .should('be.visible')
      .and('contain.text', 'Assign Project Role');
    cy.get('#project-assignment-project').select('5');
    cy.get('#project-assignment-role').select('REVIEWER');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@assignProjectRole');
    cy.wait(['@adminProjects', '@adminUsers']);
    cy.contains('Project role assigned.').should('be.visible');
    cy.contains('.role-list li', 'NCI-META Editing (5)').should(
      'contain.text',
      'REVIEWER'
    );

    cy.contains('.role-list li', 'NCI-META Editing (5)').within(() => {
      cy.contains('Remove').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Remove admin from Updated NCI-META Editing (5)?'
      );
    });
    cy.wait('@unassignProjectRole');
    cy.wait(['@adminProjects', '@adminUsers']);
    cy.contains('Project role removed.').should('be.visible');

    cy.contains('Add User').click();
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Add User');
    cy.get('#user-form-username').type('new-user');
    cy.get('#user-form-name').type('New User');
    cy.get('#user-form-email').type('new-user@example.com');
    cy.get('#user-form-role').select('USER');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@addUser');
    cy.wait('@adminUsers');
    cy.contains('User added.').should('be.visible');

    cy.contains('tr', 'new-user').within(() => {
      cy.contains('button', 'Delete').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to remove the user (new-user)?'
      );
    });
    cy.wait('@removeUser');
    cy.wait(['@adminUsers', '@adminProjects']);
    cy.contains('User removed.').should('be.visible');
    cy.contains('td', 'new-user').should('not.exist');

    cy.contains('tbody button', 'Edit').click();
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Edit User');
    cy.get('#user-form-editor-level').clear();
    cy.get('#user-form-editor-level').type('5');
    cy.get('#user-form-role').select('USER');
    cy.get('.user-form button[type="submit"]').click();
    cy.wait('@updateUser');
    cy.contains('User updated.').should('be.visible');

    cy.get('aside[aria-label="Selected project details"]').within(() => {
      cy.contains('button', 'Delete').click();
    });
    cy.wrap(null).should(() => {
      expect(confirmationMessages[confirmationMessages.length - 1]).to.equal(
        'Are you sure you want to remove the project (Updated NCI-META Editing)?'
      );
    });
    cy.wait('@removeProject');
    cy.wait(['@adminProjects', '@adminUsers']);
    cy.contains('Project removed.').should('be.visible');
    cy.contains('No projects available.').should('be.visible');
  });

  it('ignores stored guest sessions when login is enabled', () => {
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'admin',
      'deploy.license.enabled': 'false',
      'deploy.login.enabled': 'true'
    }).as('loginRequiredConfig');

    cy.visit('/admin', {
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
      'deploy.enabled.tabs': 'admin',
      'deploy.license.enabled': 'false',
      'deploy.login.enabled': 'true'
    }).as('staleSessionConfig');

    cy.intercept('GET', '/umls-server-rest/security/roles', {
      statusCode: 500,
      body: 'AuthToken does not have a valid username.'
    }).as('adminAuthFailure');

    cy.visit('/admin', {
      onBeforeLoad(window) {
        window.localStorage.setItem(
          'user',
          JSON.stringify({
            applicationRole: 'ADMINISTRATOR',
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

    cy.wait(['@staleSessionConfig', '@adminAuthFailure']);
    cy.location('pathname').should('equal', '/login');
    cy.window().its('localStorage.user').should('be.undefined');
  });

  it('supports the phase 6 process and workflow slices', () => {
    const storedProjectUser = {
      applicationRole: 'USER',
      authToken: 'DSS',
      name: 'Deborah Shapiro',
      projectRoleMap: {
        '5': 'ADMINISTRATOR'
      },
      userName: 'dss',
      userPreferences: {
        lastProjectId: 5,
        lastProjectRole: 'ADMINISTRATOR',
        lastTab: '/process',
        properties: {}
      }
    };
    let preparedExecutionExists = false;
    let preparedExecutionStartDate: string | null = null;
    let preparedExecutionStopDate: string | null = null;
    let preparedExecutionFailDate: string | null = null;
    let preparedExecutionFinishDate: string | null = null;
    let preparedExecutionSteps: unknown[] = [];
    let addedProcessConfigExists = false;
    let addedProcessConfigDeleted = false;
    let addedProcessConfigName = 'Custom Inversion';
    let addedProcessConfigDescription = 'Custom inversion process';
    let importedProcessConfigExists = false;
    const addedProcessConfig = () => ({
      description: addedProcessConfigDescription,
      feedbackEmail: 'process@example.com',
      id: 350,
      inputPath: '/data/custom/input',
      lastModified: '2026-06-10T09:05:00Z',
      logPath: '/data/custom/logs',
      name: addedProcessConfigName,
      steps: [],
      terminology: 'NCI',
      type: 'Inversion',
      version: '2026_05'
    });
    const importedProcessConfig = () => ({
      description: 'Imported process config',
      feedbackEmail: 'import@example.com',
      id: 360,
      inputPath: '/data/import/input',
      lastModified: '2026-06-10T09:07:00Z',
      logPath: '/data/import/logs',
      name: 'Imported Inversion',
      steps: [],
      terminology: 'NCI',
      type: 'Inversion',
      version: '2026_05'
    });
    let configuredSteps = [
      {
        algorithmKey: 'collect-source-files',
        description: 'Collect source files for inversion.',
        enabled: true,
        id: 700,
        lastModified: '2026-06-10T08:05:00Z',
        name: 'Collect Source Files',
        parameters: [
          {
            fieldName: 'inputPath',
            name: 'Input Path',
            type: 'DIRECTORY',
            value: '/data/source'
          }
        ]
      },
      {
        algorithmKey: 'validate-output',
        description: 'Validate generated inversion output.',
        enabled: true,
        id: 701,
        lastModified: '2026-06-10T08:06:00Z',
        name: 'Validate Output',
        parameters: []
      }
    ];
    const completedStep = {
      ...configuredSteps[0],
      algorithmConfigId: 700,
      finishDate: '2026-06-10T08:42:00Z',
      id: 800,
      startDate: '2026-06-10T08:31:00Z',
      warning: false
    };
    const steppedForwardStep = {
      ...configuredSteps[1],
      algorithmConfigId: 701,
      finishDate: '2026-06-10T09:17:00Z',
      id: 801,
      startDate: '2026-06-10T09:16:30Z',
      warning: false
    };
    const preparedExecution = () => ({
      failDate: preparedExecutionFailDate,
      finishDate: preparedExecutionFinishDate,
      id: 302,
      name: 'Nightly Inversion',
      processConfigId: 300,
      startDate: preparedExecutionStartDate,
      stopDate: preparedExecutionStopDate,
      steps: [],
      type: 'inversion',
      warning: false,
      workId: 'INV-002'
    });
    let importedWorkflowConfigExists = false;
    let addedWorkflowConfigExists = false;
    let addedWorkflowConfigDeleted = false;
    let createdWorkflowChecklistExists = false;
    let createdWorkflowWorklistExists = false;
    let workflowChecklistDeleted = false;
    let workflowWorklistDeleted = false;
    let workflowWorklistAuthorAvailable = true;
    let workflowWorklistAuthors: string[] = [];
    let workflowWorklistReviewerAvailable = false;
    let workflowWorklistReviewers: string[] = [];
    let workflowWorklistStateHistory: Record<string, string> = {
      Created: '2026-06-10T08:30:00Z'
    };
    let workflowWorklistStatus = 'NEW';
    let workflowChecklistNotes = [
      {
        id: 900,
        lastModified: '2026-06-10T08:26:00Z',
        lastModifiedBy: 'admin',
        note: '<p>Initial checklist note</p>'
      }
    ];
    let workflowWorklistNotes = [
      {
        id: 901,
        lastModified: '2026-06-10T08:31:00Z',
        lastModifiedBy: 'admin',
        note: '<p>Initial worklist note</p>'
      }
    ];
    let nextWorkflowNoteId = 902;
    let workflowReportExists = false;
    const workflowReportFileName = 'wrk26a_demotions_default_001_rpt.txt';
    const workflowProjectUsers = [
      {
        id: 1,
        name: 'Deborah Shapiro',
        projectRoleMap: {
          '5': 'REVIEWER'
        },
        team: 'QA',
        userName: 'dss'
      },
      {
        id: 2,
        name: 'Author Two',
        projectRoleMap: {
          '5': 'AUTHOR'
        },
        team: 'QA',
        userName: 'author2'
      },
      {
        id: 3,
        name: 'Other Team Author',
        projectRoleMap: {
          '5': 'AUTHOR'
        },
        team: 'Content',
        userName: 'author3'
      },
      {
        id: 4,
        name: 'Reviewer Two',
        projectRoleMap: {
          '5': 'REVIEWER'
        },
        team: 'QA',
        userName: 'reviewer2'
      }
    ];
    let addedWorkflowConfigType = 'QUALITY_ASSURANCE';
    let addedWorkflowConfigQueryStyle = 'REPORT';
    let addedWorkflowConfigAdminConfig = false;
    let addedWorkflowConfigMutuallyExclusive = true;
    let addedWorkflowBinDefinitionExists = false;
    let addedWorkflowBinDeleted = false;
    let addedWorkflowBinName = 'qa-bin';
    let addedWorkflowBinDescription = 'Quality assurance bin';
    let addedWorkflowBinQuery = 'select * from concepts';
    let addedWorkflowBinQueryType = 'SQL';
    let addedWorkflowBinRequired = true;
    let addedWorkflowBinEditable = true;
    let addedWorkflowBinEnabled = true;
    const primaryWorkflowConfig = () => ({
      id: 400,
      adminConfig: false,
      lastModified: '2026-06-10T08:00:00Z',
      mutuallyExclusive: true,
      queryStyle: 'CLUSTER',
      type: 'MUTUALLY_EXCLUSIVE',
      workflowBinDefinitions: [
        {
          id: 401,
          name: 'demotions',
          rank: 1
        },
        ...(addedWorkflowBinDefinitionExists && !addedWorkflowBinDeleted
          ? [
              {
                id: 470,
                name: addedWorkflowBinName,
                rank: 2
              }
            ]
          : [])
      ]
    });
    const importedWorkflowConfig = () => ({
      id: 450,
      adminConfig: false,
      lastModified: '2026-06-10T09:20:00Z',
      mutuallyExclusive: false,
      queryStyle: 'CLUSTER',
      type: 'IMPORTED_CONFIG',
      workflowBinDefinitions: [
        {
          id: 451,
          name: 'imported-bin',
          rank: 1
        }
      ]
    });
    const addedWorkflowConfig = () => ({
      id: 460,
      adminConfig: addedWorkflowConfigAdminConfig,
      lastModified: '2026-06-10T09:25:00Z',
      mutuallyExclusive: addedWorkflowConfigMutuallyExclusive,
      queryStyle: addedWorkflowConfigQueryStyle,
      type: addedWorkflowConfigType,
      workflowBinDefinitions: []
    });
    const primaryWorkflowBinDefinition = () => ({
      autofix: '',
      description: 'Demotion review bin',
      editable: true,
      enabled: true,
      id: 401,
      name: 'demotions',
      query: 'select * from concepts where status = "DEMOTION"',
      queryType: 'SQL',
      required: true,
      workflowConfig: {
        id: 400
      },
      workflowConfigId: 400
    });
    const addedWorkflowBinDefinition = () => ({
      autofix: '',
      description: addedWorkflowBinDescription,
      editable: addedWorkflowBinEditable,
      enabled: addedWorkflowBinEnabled,
      id: 470,
      name: addedWorkflowBinName,
      query: addedWorkflowBinQuery,
      queryType: addedWorkflowBinQueryType,
      required: addedWorkflowBinRequired,
      workflowConfig: {
        id: 400
      },
      workflowConfigId: 400
    });
    const workflowWorklist = () => ({
      authorAvailable: workflowWorklistAuthorAvailable,
      authors: workflowWorklistAuthors,
      description: 'Demotion default worklist',
      epoch: '26a',
      id: 430,
      lastModified: '2026-06-10T08:30:00Z',
      name: 'wrk26a_demotions_default_001',
      notes: workflowWorklistNotes,
      reviewerAvailable: workflowWorklistReviewerAvailable,
      reviewers: workflowWorklistReviewers,
      stats: {
        actionsCt: 2,
        clusterCt: 2,
        conceptCt: 4
      },
      team: 'QA',
      trackingRecords: [{ id: 4 }, { id: 5 }],
      workflowBinName: 'demotions',
      workflowStateHistory: workflowWorklistStateHistory,
      workflowStatus: workflowWorklistStatus
    });
    const createdWorkflowChecklist = () => ({
      description: 'Created from default demotion clusters',
      id: 421,
      lastModified: '2026-06-10T10:15:00Z',
      name: 'Created QA Checklist',
      stats: {
        clusterCt: 2
      }
    });
    const createdWorkflowWorklist = () => ({
      authorAvailable: true,
      authors: [],
      description: 'Created worklist from default demotion clusters',
      epoch: '26a',
      id: 431,
      lastModified: '2026-06-10T10:20:00Z',
      name: 'wrk26a_demotions_default_002',
      reviewerAvailable: false,
      reviewers: [],
      stats: {
        clusterCt: 2,
        conceptCt: 3
      },
      team: 'QA',
      workflowBinName: 'demotions',
      workflowStatus: 'NEW'
    });

    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      ...config,
      'deploy.enabled.tabs': 'process,workflow,admin',
      'deploy.license.enabled': 'false'
    }).as('phase6Config');

    cy.intercept('GET', '/umls-server-rest/metadata/terminology/current', {
      terminologies: [
        {
          current: true,
          terminology: 'NCI',
          version: '2026_05'
        },
        {
          current: true,
          terminology: 'NCIMTH',
          version: 'latest'
        }
      ],
      totalCount: 2
    }).as('phase6Terminologies');

    cy.intercept('POST', '/umls-server-rest/process/config/find?projectId=5', (request) => {
      const pageStart = request.body.startIndex ?? 0;
      const firstPageProcesses = [
        {
          id: 300,
          name: 'Nightly Inversion',
          description: 'Build inversion artifacts',
          lastModified: '2026-06-10T08:15:00Z',
          steps: [],
          terminology: 'NCI',
          type: 'inversion',
          version: '2026_05'
        },
        ...(importedProcessConfigExists ? [importedProcessConfig()] : []),
        ...(addedProcessConfigExists && !addedProcessConfigDeleted
          ? [addedProcessConfig()]
          : []),
        ...Array.from({ length: 9 }, (_, index) => ({
          id: 310 + index,
          name: `Inversion Config ${index + 2}`,
          description: 'Additional inversion config',
          lastModified: '2026-06-10T08:10:00Z',
          steps: [],
          terminology: 'NCI',
          type: 'inversion',
          version: '2026_05'
        }))
      ];
      const secondPageProcesses = [
        {
          id: 320,
          name: 'Inversion Config 11',
          description: 'Second-page inversion config',
          lastModified: '2026-06-10T08:05:00Z',
          steps: [],
          terminology: 'NCI',
          type: 'inversion',
          version: '2026_05'
        },
        {
          id: 321,
          name: 'Inversion Config 12',
          description: 'Second-page inversion config',
          lastModified: '2026-06-10T08:04:00Z',
          steps: [],
          terminology: 'NCI',
          type: 'inversion',
          version: '2026_05'
        }
      ];

      request.reply({
        processes:
          pageStart === 0 ? firstPageProcesses.slice(0, 10) : secondPageProcesses,
        totalCount:
          12 +
          (importedProcessConfigExists ? 1 : 0) +
          (addedProcessConfigExists && !addedProcessConfigDeleted ? 1 : 0)
      });
    }).as('processConfigs');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/process\/config\/\d+\?projectId=5$/,
      (request) => {
        const id = Number(new URL(request.url).pathname.split('/').pop());
        const isPrimary = id === 300;

        if (id === 350) {
          request.reply(addedProcessConfig());
          return;
        }

        if (id === 360) {
          request.reply(importedProcessConfig());
          return;
        }

        request.reply({
          id,
          name: isPrimary ? 'Nightly Inversion' : `Inversion Config ${id - 309}`,
          description: isPrimary
            ? 'Build inversion artifacts'
            : 'Additional inversion config',
          inputPath: '/data/input',
          lastModified: '2026-06-10T08:15:00Z',
          logPath: '/data/logs',
          steps: isPrimary ? configuredSteps : [],
          terminology: 'NCI',
          type: 'inversion',
          version: '2026_05'
        });
      }
    ).as('processConfigDetail');

    cy.intercept('GET', '/umls-server-rest/process/algo/inversion?projectId=5', {
      keyValuePairs: [
        {
          key: 'collect-source-files',
          value: 'Collect Source Files'
        },
        {
          key: 'normalize-metadata',
          value: 'Normalize Metadata'
        },
        {
          key: 'validate-output',
          value: 'Validate Output'
        }
      ],
      totalCount: 3
    }).as('algorithmTypes');

    cy.intercept('GET', '/umls-server-rest/process/algo/autofix?projectId=5', {
      keyValuePairs: [
        {
          key: 'DemotionAutofixAlgorithm',
          value: 'Demotion Autofix Algorithm'
        }
      ],
      totalCount: 1
    }).as('autofixAlgorithms');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/process\/config\/algo\/[^/]+\/new\?projectId=5&processId=300$/,
      (request) => {
        const algorithmKey = decodeURIComponent(
          new URL(request.url).pathname.split('/').at(-2) ?? ''
        );
        const label =
          algorithmKey === 'normalize-metadata'
            ? 'Normalize Metadata'
            : algorithmKey === 'validate-output'
              ? 'Validate Output'
              : 'Collect Source Files';

        request.reply({
          algorithmKey,
          description: `${label} step`,
          enabled: true,
          name: label,
          parameters: [
            {
              fieldName: 'mode',
              name: 'Mode',
              possibleValues: ['full', 'delta'],
              type: 'ENUM',
              value: 'full'
            },
            {
              fieldName: 'reportOnly',
              name: 'Report Only',
              type: 'BOOLEAN',
              value: 'false'
            }
          ],
          processId: 300,
          properties: {}
        });
      }
    ).as('newAlgorithmConfig');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/process\/config\/algo\/\d+\?projectId=5$/,
      (request) => {
        const id = Number(new URL(request.url).pathname.split('/').pop());
        const step = configuredSteps.find((entry) => entry.id === id);

        request.reply({
          ...(step ?? configuredSteps[0]),
          process: {
            id: 300
          },
          processId: 300
        });
      }
    ).as('algorithmConfigDetail');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/process/config/algo/validate',
        query: {
          processId: '300',
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          algorithmKey: 'normalize-metadata',
          name: 'Normalize Metadata Step'
        });
        request.reply({});
      }
    ).as('validateAlgorithmConfig');

    cy.intercept(
      {
        method: 'PUT',
        pathname: '/umls-server-rest/process/config/algo',
        query: {
          processId: '300',
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          algorithmKey: 'normalize-metadata',
          name: 'Normalize Metadata Step'
        });
        configuredSteps = [
          ...configuredSteps,
          {
            ...request.body,
            enabled: true,
            id: 702,
            lastModified: '2026-06-10T09:12:00Z'
          }
        ];
        request.reply(configuredSteps[configuredSteps.length - 1]);
      }
    ).as('addAlgorithmConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/process/config/algo',
        query: {
          processId: '300',
          projectId: '5'
        }
      },
      (request) => {
        configuredSteps = configuredSteps.map((step) =>
          step.id === request.body.id
            ? {
                ...step,
                ...request.body,
                lastModified: '2026-06-10T09:14:00Z'
              }
            : step
        );
        request.reply({});
      }
    ).as('updateAlgorithmConfig');

    cy.intercept(
      {
        method: 'DELETE',
        pathname: '/umls-server-rest/process/config/algo/702',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        configuredSteps = configuredSteps.filter((step) => step.id !== 702);
        request.reply({});
      }
    ).as('removeAlgorithmConfig');

    cy.intercept(
      {
        method: 'PUT',
        pathname: '/umls-server-rest/process/config',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          feedbackEmail: 'process@example.com',
          inputPath: '/data/custom/input',
          logPath: '/data/custom/logs',
          name: 'Custom Inversion',
          terminology: 'NCI',
          type: 'Inversion',
          version: '2026_05'
        });
        addedProcessConfigExists = true;
        addedProcessConfigDeleted = false;
        addedProcessConfigName = request.body.name;
        addedProcessConfigDescription = request.body.description;
        request.reply(addedProcessConfig());
      }
    ).as('addProcessConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/process/config',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        if (request.body.id === 300) {
          configuredSteps = request.body.steps;
          request.reply({});
          return;
        }

        expect(request.body).to.include({
          id: 350,
          name: 'Updated Custom Inversion'
        });
        addedProcessConfigName = request.body.name;
        addedProcessConfigDescription = request.body.description;
        request.reply({});
      }
    ).as('updateProcessConfig');

    cy.intercept(
      {
        method: 'DELETE',
        pathname: '/umls-server-rest/process/config/350',
        query: {
          cascade: 'true',
          projectId: '5'
        }
      },
      (request) => {
        addedProcessConfigDeleted = true;
        request.reply({});
      }
    ).as('removeProcessConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/process/config/import',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.headers['content-type']).to.contain('multipart/form-data');
        importedProcessConfigExists = true;
        request.reply(importedProcessConfig());
      }
    ).as('importProcessConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/process/config/export',
        query: {
          processId: '300',
          projectId: '5'
        }
      },
      (request) => {
        request.reply({
          body: JSON.stringify({
            id: 300,
            name: 'Nightly Inversion'
          }),
          headers: {
            'content-type': 'application/octet-stream'
          },
          statusCode: 200
        });
      }
    ).as('exportProcessConfig');

    cy.intercept('POST', '/umls-server-rest/process/execution/find?projectId=5', (request) => {
      const pageStart = request.body.startIndex ?? 0;
      const processes = [
        ...(preparedExecutionExists ? [preparedExecution()] : []),
        {
          failDate: null,
          finishDate: '2026-06-10T08:45:00Z',
          id: 301,
          name: 'Nightly Inversion',
          processConfigId: 300,
          startDate: '2026-06-10T08:30:00Z',
          type: 'inversion',
          warning: false,
          workId: 'INV-001'
        },
        ...Array.from({ length: 9 }, (_, index) => ({
          failDate: null,
          finishDate: '2026-06-10T08:40:00Z',
          id: 330 + index,
          name: `Nightly Inversion ${index + 2}`,
          processConfigId: 300,
          startDate: '2026-06-10T08:25:00Z',
          type: 'inversion',
          warning: false,
          workId: `INV-00${index + 2}`
        }))
      ];
      const secondPageProcesses = [
        {
          failDate: null,
          finishDate: '2026-06-10T08:20:00Z',
          id: 340,
          name: 'Nightly Inversion 11',
          processConfigId: 300,
          startDate: '2026-06-10T08:00:00Z',
          type: 'inversion',
          warning: false,
          workId: 'INV-011'
        },
        {
          failDate: null,
          finishDate: '2026-06-10T08:10:00Z',
          id: 341,
          name: 'Nightly Inversion 12',
          processConfigId: 300,
          startDate: '2026-06-10T07:50:00Z',
          type: 'inversion',
          warning: false,
          workId: 'INV-012'
        }
      ];

      request.reply({
        processes: pageStart === 0 ? processes.slice(0, 10) : secondPageProcesses,
        totalCount: preparedExecutionExists ? 13 : 12
      });
    }).as('processExecutions');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/process\/execution\/\d+\?projectId=5$/,
      (request) => {
        const id = Number(new URL(request.url).pathname.split('/').pop());

        if (id === 302) {
          request.reply({
            ...preparedExecution(),
            description: 'Prepared inversion execution',
            steps: preparedExecutionSteps
          });
          return;
        }

        request.reply({
          failDate: null,
          finishDate: '2026-06-10T08:45:00Z',
          id,
          name:
            id === 341
              ? 'Nightly Inversion 12'
              : id === 340
                ? 'Nightly Inversion 11'
                : 'Nightly Inversion',
          processConfigId: 300,
          startDate: '2026-06-10T08:30:00Z',
          steps: [completedStep],
          type: 'inversion',
          warning: false,
          workId: id === 301 ? 'INV-001' : `INV-${id}`
        });
      }
    ).as('processExecutionDetail');

    cy.intercept('GET', '/umls-server-rest/process/executing?projectId=5', (request) => {
      const processes =
        preparedExecutionExists &&
        preparedExecutionStartDate &&
        !preparedExecutionStopDate &&
        !preparedExecutionFailDate &&
        !preparedExecutionFinishDate
          ? [preparedExecution()]
          : [];

      request.reply({
        processes,
        totalCount: processes.length
      });
    }).as('runningProcesses');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/config/300/prepare',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        preparedExecutionExists = true;
        preparedExecutionStartDate = null;
        preparedExecutionStopDate = null;
        preparedExecutionFailDate = null;
        preparedExecutionFinishDate = null;
        preparedExecutionSteps = [];
        request.reply('302');
      }
    ).as('prepareProcess');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/execution/302/execute',
        query: {
          background: 'true',
          projectId: '5'
        }
      },
      (request) => {
        preparedExecutionStartDate = '2026-06-10T09:15:00Z';
        request.reply('302');
      }
    ).as('executeProcess');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/execution/302/cancel',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        preparedExecutionStopDate = '2026-06-10T09:16:00Z';
        preparedExecutionSteps = [completedStep];
        request.reply('302');
      }
    ).as('cancelProcess');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/execution/302/step',
        query: {
          background: 'true',
          projectId: '5',
          step: '1'
        }
      },
      (request) => {
        preparedExecutionSteps = [completedStep, steppedForwardStep];
        request.reply('302');
      }
    ).as('stepProcess');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/execution/302/step',
        query: {
          background: 'true',
          projectId: '5',
          step: '-1'
        }
      },
      (request) => {
        preparedExecutionSteps = [];
        request.reply('302');
      }
    ).as('unstepProcess');

    cy.intercept(
      {
        method: 'GET',
        pathname: '/umls-server-rest/process/execution/302/restart',
        query: {
          background: 'true',
          projectId: '5'
        }
      },
      (request) => {
        preparedExecutionStopDate = null;
        preparedExecutionStartDate = '2026-06-10T09:17:00Z';
        request.reply('302');
      }
    ).as('restartProcess');

    cy.intercept('GET', '/umls-server-rest/project/5', {
      id: 5,
      name: 'NCI-META Editing',
      teamBased: true,
      userRoleMap: {
        author2: 'AUTHOR',
        author3: 'AUTHOR',
        dss: 'REVIEWER',
        reviewer2: 'REVIEWER'
      }
    }).as('workflowProject');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/project/5/users'
      },
      (request) => {
        expect(request.body).to.deep.include({
          maxResults: 500,
          sortField: 'userName',
          startIndex: 0
        });
        request.reply({
          totalCount: workflowProjectUsers.length,
          users: workflowProjectUsers
        });
      }
    ).as('workflowProjectUsers');

    cy.intercept('GET', '/umls-server-rest/workflow/config/all?projectId=5', (request) => {
      const configs = [
        primaryWorkflowConfig(),
        ...(importedWorkflowConfigExists ? [importedWorkflowConfig()] : []),
        ...(addedWorkflowConfigExists && !addedWorkflowConfigDeleted
          ? [addedWorkflowConfig()]
          : [])
      ];

      request.reply({
        configs,
        totalCount: configs.length
      });
    }).as('workflowConfigs');

    cy.intercept('GET', '/umls-server-rest/workflow/epoch/all?projectId=5', {
      epochs: [
        {
          active: true,
          id: 410,
          name: '26a'
        }
      ],
      totalCount: 1
    }).as('workflowEpochs');

    cy.intercept('POST', '/umls-server-rest/workflow/checklist/find?projectId=5', (request) => {
      const checklists = [
        ...(workflowChecklistDeleted
          ? []
          : [
              {
                description: 'Release checklist',
                id: 420,
                lastModified: '2026-06-10T08:25:00Z',
                name: 'Release QA',
                stats: {
                  clusterCt: 3
                }
              }
            ]),
        ...(createdWorkflowChecklistExists ? [createdWorkflowChecklist()] : [])
      ];

      request.reply({
        checklists,
        totalCount: checklists.length
      });
    }).as('workflowChecklists');

    cy.intercept('POST', '/umls-server-rest/workflow/worklist/find?projectId=5', (request) => {
      const worklists = [
        ...(workflowWorklistDeleted ? [] : [workflowWorklist()]),
        ...(createdWorkflowWorklistExists ? [createdWorkflowWorklist()] : [])
      ];

      request.reply({
        totalCount: worklists.length,
        worklists
      });
    }).as('workflowWorklists');

    cy.intercept(
      'GET',
      '/umls-server-rest/workflow/checklist/420?projectId=5',
      (request) => {
        request.reply({
          description: 'Release checklist',
          id: 420,
          lastModified: '2026-06-10T08:25:00Z',
          name: 'Release QA',
          notes: workflowChecklistNotes,
          stats: {
            approvedCt: 1,
            clusterCt: 3,
            conceptCt: 5
          },
          trackingRecords: [{ id: 1 }, { id: 2 }, { id: 3 }]
        });
      }
    ).as('workflowChecklistDetail');

    cy.intercept(
      'GET',
      '/umls-server-rest/workflow/worklist/430?projectId=5',
      (request) => {
        request.reply(workflowWorklist());
      }
    ).as('workflowWorklistDetail');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/worklist\/action\?.*/,
      (request) => {
        const url = new URL(request.url);
        const userName = url.searchParams.get('userName') ?? '';

        expect(url.searchParams.get('projectId')).to.equal('5');
        expect(url.searchParams.get('worklistId')).to.equal('430');

        switch (url.searchParams.get('action')) {
          case 'ASSIGN':
            expect(url.searchParams.get('userRole')).to.equal('AUTHOR');
            workflowWorklistAuthorAvailable = false;
            workflowWorklistAuthors = [userName];
            workflowWorklistStateHistory = {
              Assigned: '2026-06-10T10:35:00Z',
              Created: '2026-06-10T08:30:00Z'
            };
            workflowWorklistStatus = 'EDITING';
            break;
          case 'UNASSIGN':
            if (userName === 'author2') {
              expect(url.searchParams.get('userRole')).to.equal('ADMINISTRATOR');
            } else {
              expect(userName).to.equal('dss');
              expect(url.searchParams.get('userRole')).to.equal('AUTHOR');
            }
            workflowWorklistAuthorAvailable = true;
            workflowWorklistAuthors = [];
            workflowWorklistStateHistory = {
              Created: '2026-06-10T08:30:00Z'
            };
            workflowWorklistStatus = 'NEW';
            break;
          case 'FINISH':
            expect(userName).to.equal('dss');
            expect(url.searchParams.get('userRole')).to.equal('REVIEWER');
            workflowWorklistStateHistory = {
              Created: '2026-06-10T08:30:00Z',
              'Review Assigned': '2026-06-10T10:40:00Z',
              'Review Done': '2026-06-10T10:45:00Z'
            };
            workflowWorklistStatus = 'REVIEW_DONE';
            break;
          default:
            throw new Error(`Unexpected workflow action ${url.searchParams.get('action')}`);
        }

        request.reply(workflowWorklist());
      }
    ).as('performWorkflowAction');

    cy.intercept(
      'POST',
      /\/umls-server-rest\/workflow\/worklist\/430\/stamp\?.*/,
      (request) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('projectId')).to.equal('5');
        expect(url.searchParams.get('activityId')).to.equal(
          'wrk26a_demotions_default_001'
        );
        expect(url.searchParams.get('approve')).to.equal('true');
        request.reply({});
      }
    ).as('stampWorklist');

    cy.intercept(
      'POST',
      /\/umls-server-rest\/workflow\/checklist\?.*/,
      (request) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('projectId')).to.equal('5');
        expect(url.searchParams.get('workflowBinId')).to.equal('440');
        expect(url.searchParams.get('clusterType')).to.equal(null);
        expect(url.searchParams.get('name')).to.equal('Created QA Checklist');
        expect(url.searchParams.get('description')).to.equal(
          'Created from default demotion clusters'
        );
        expect(url.searchParams.get('randomize')).to.equal('false');
        expect(url.searchParams.get('excludeOnWorklist')).to.equal('true');
        expect(request.body).to.deep.include({
          maxResults: 50,
          sortField: 'indexedData',
          startIndex: 1
        });

        createdWorkflowChecklistExists = true;
        request.reply(createdWorkflowChecklist());
      }
    ).as('createChecklist');

    cy.intercept(
      'PUT',
      /\/umls-server-rest\/workflow\/worklist\?.*/,
      (request) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('projectId')).to.equal('5');
        expect(url.searchParams.get('workflowBinId')).to.equal('440');
        expect(url.searchParams.get('clusterType')).to.equal(null);
        expect(request.body).to.deep.include({
          maxResults: 2,
          sortField: 'clusterId',
          startIndex: 0
        });

        createdWorkflowWorklistExists = true;
        request.reply(createdWorkflowWorklist());
      }
    ).as('createWorklist');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/log\?projectId=5.*checklistId=420.*/,
      '2026-06-10 ADD checklist note - Release QA'
    ).as('workflowChecklistLog');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/log\?projectId=5.*worklistId=430.*/,
      '2026-06-10 ASSIGN worklist - wrk26a_demotions_default_001'
    ).as('workflowWorklistLog');

    cy.intercept(
      'PUT',
      '/umls-server-rest/workflow/worklist/430/note?projectId=5',
      (request) => {
        expect([
          'Assigning to author2',
          'Follow-up worklist note'
        ]).to.include(request.body);

        const note = {
          id: nextWorkflowNoteId++,
          lastModified: '2026-06-10T11:05:00Z',
          lastModifiedBy: 'dss',
          note: request.body
        };
        workflowWorklistNotes = [...workflowWorklistNotes, note];
        request.reply(note);
      }
    ).as('addWorklistNote');

    cy.intercept(
      'DELETE',
      /\/umls-server-rest\/workflow\/worklist\/note\/\d+\?projectId=5/,
      (request) => {
        const noteId = Number(request.url.match(/\/note\/(\d+)/)?.[1] ?? 0);

        workflowWorklistNotes = workflowWorklistNotes.filter(
          (note) => note.id !== noteId
        );
        request.reply({});
      }
    ).as('removeWorklistNote');

    cy.intercept(
      'PUT',
      '/umls-server-rest/workflow/checklist/420/note?projectId=5',
      (request) => {
        expect(request.body).to.equal('Follow-up checklist note');

        const note = {
          id: nextWorkflowNoteId++,
          lastModified: '2026-06-10T11:07:00Z',
          lastModifiedBy: 'dss',
          note: request.body
        };
        workflowChecklistNotes = [...workflowChecklistNotes, note];
        request.reply(note);
      }
    ).as('addChecklistNote');

    cy.intercept(
      'DELETE',
      /\/umls-server-rest\/workflow\/checklist\/note\/\d+\?projectId=5/,
      (request) => {
        const noteId = Number(request.url.match(/\/note\/(\d+)/)?.[1] ?? 0);

        workflowChecklistNotes = workflowChecklistNotes.filter(
          (note) => note.id !== noteId
        );
        request.reply({});
      }
    ).as('removeChecklistNote');

    cy.intercept(
      'POST',
      '/umls-server-rest/workflow/report?projectId=5&query=wrk26a_demotions_default_001',
      (request) => {
        expect(request.body).to.deep.include({
          maxResults: 1,
          startIndex: 0
        });
        request.reply({
          objects: workflowReportExists ? [workflowReportFileName] : [],
          strings: workflowReportExists ? [workflowReportFileName] : [],
          totalCount: workflowReportExists ? 1 : 0
        });
      }
    ).as('workflowReports');

    cy.intercept(
      'GET',
      '/umls-server-rest/workflow/worklist/430/report/generate?projectId=5&sendEmail=true',
      (request) => {
        workflowReportExists = true;
        request.reply({
          body: workflowReportFileName,
          headers: {
            'content-type': 'text/plain'
          }
        });
      }
    ).as('generateWorkflowReport');

    cy.intercept(
      'GET',
      `/umls-server-rest/workflow/report/${workflowReportFileName}?projectId=5`,
      {
        body: 'Concept report for wrk26a_demotions_default_001\n',
        headers: {
          'content-type': 'text/plain'
        },
        statusCode: 200
      }
    ).as('downloadWorkflowReport');

    cy.intercept(
      'DELETE',
      `/umls-server-rest/workflow/report/${workflowReportFileName}?projectId=5`,
      (request) => {
        workflowReportExists = false;
        request.reply({});
      }
    ).as('removeWorkflowReport');

    cy.intercept('GET', '/umls-server-rest/workflow/checklist/420/export?projectId=5', {
      body: 'clusterId\tconceptId\n1\t1\n',
      headers: {
        'content-type': 'application/octet-stream'
      },
      statusCode: 200
    }).as('exportChecklist');

    cy.intercept('GET', '/umls-server-rest/workflow/worklist/430/export?projectId=5', {
      body: 'clusterId\tconceptId\n4\t4\n',
      headers: {
        'content-type': 'application/octet-stream'
      },
      statusCode: 200
    }).as('exportWorklist');

    cy.intercept('DELETE', '/umls-server-rest/workflow/checklist/420?projectId=5', (request) => {
      workflowChecklistDeleted = true;
      request.reply({});
    }).as('removeChecklist');

    cy.intercept('DELETE', '/umls-server-rest/workflow/worklist/430?projectId=5', (request) => {
      workflowWorklistDeleted = true;
      request.reply({});
    }).as('removeWorklist');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/bin\/all\?projectId=5&type=.*/,
      (request) => {
        const type = new URL(request.url).searchParams.get('type');
        const bins =
          type === 'IMPORTED_CONFIG'
            ? [
                {
                  clusterCt: 2,
                  creationTime: 6000,
                  description: 'Imported workflow bin',
                  editable: true,
                  id: 452,
                  lastModified: '2026-06-10T09:22:00Z',
                  name: 'imported-bin',
                  rank: 1,
                  required: false,
                  stats: [
                    {
                      clusterType: 'all',
                      stats: {
                        all: 2,
                        assigned: 1,
                        unassigned: 1
                      }
                    }
                  ],
                  timestamp: '2026-06-10T09:21:00Z',
                  type: 'IMPORTED_CONFIG'
                }
              ]
            : type === addedWorkflowConfigType
              ? []
            : [
                {
                  clusterCt: 6,
                  creationTime: 12000,
                  description: 'Demotion review bin',
                  editable: true,
                  enabled: true,
                  id: 440,
                  lastModified: '2026-06-10T08:14:00Z',
                  name: 'demotions',
                  rank: 1,
                  required: true,
                  stats: [
                    {
                      clusterType: 'all',
                      stats: {
                        all: 6,
                        assigned: 2,
                        unassigned: 4
                      }
                    },
                    {
                      clusterType: 'default',
                      stats: {
                        all: 4,
                        assigned: 1,
                        unassigned: 3
                      }
                    },
                    {
                      clusterType: 'chem',
                      stats: {
                        all: 2,
                        assigned: 1,
                        unassigned: 1
                      }
                    }
                  ],
                  timestamp: '2026-06-10T08:12:00Z',
                  type: 'MUTUALLY_EXCLUSIVE'
                },
                ...(addedWorkflowBinDefinitionExists && !addedWorkflowBinDeleted
                  ? [
                      {
                        clusterCt: 0,
                        creationTime: 0,
                        description: addedWorkflowBinDescription,
                        editable: addedWorkflowBinEditable,
                        enabled: addedWorkflowBinEnabled,
                        id: 471,
                        lastModified: '2026-06-10T09:40:00Z',
                        name: addedWorkflowBinName,
                        rank: 2,
                        required: addedWorkflowBinRequired,
                        stats: [
                          {
                            clusterType: 'all',
                            stats: {
                              all: 0,
                              assigned: 0,
                              unassigned: 0
                            }
                          }
                        ],
                        timestamp: '2026-06-10T09:39:00Z',
                        type: 'MUTUALLY_EXCLUSIVE'
                      }
                    ]
                  : [])
              ];

        request.reply({
          bins,
          totalCount: bins.length
        });
      }
    ).as('workflowBins');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/definition\?projectId=5&name=.*&type=.*/,
      (request) => {
        const url = new URL(request.url);
        const name = url.searchParams.get('name');

        if (name === 'demotions') {
          request.reply(primaryWorkflowBinDefinition());
          return;
        }

        request.reply(addedWorkflowBinDefinition());
      }
    ).as('workflowBinDefinition');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/bin/clear/all',
        query: {
          projectId: '5',
          type: 'MUTUALLY_EXCLUSIVE'
        }
      },
      {}
    ).as('clearWorkflowBins');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/bin/regenerate/all',
        query: {
          projectId: '5',
          type: 'MUTUALLY_EXCLUSIVE'
        }
      },
      {}
    ).as('regenerateWorkflowBins');

    cy.intercept(
      'GET',
      /\/umls-server-rest\/workflow\/query\/test\?projectId=5.*/,
      (request) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('query')).to.equal('select * from concepts');
        expect(url.searchParams.get('queryType')).to.equal('SQL');
        expect(url.searchParams.get('queryStyle')).to.equal('CLUSTER');
        request.reply({
          results: [
            {
              value: '100001, 100001'
            },
            {
              value: '100002, 100002'
            }
          ],
          totalCount: 2
        });
      }
    ).as('workflowQueryTest');

    cy.intercept(
      'PUT',
      /\/umls-server-rest\/workflow\/definition\?projectId=5.*/,
      (request) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('positionAfterId')).to.equal('401');
        expect(request.body).to.include({
          description: 'Quality assurance bin',
          editable: true,
          enabled: true,
          name: 'qa-bin',
          query: 'select * from concepts',
          queryType: 'SQL',
          required: true,
          workflowConfigId: 400
        });
        expect(request.body.workflowConfig).to.deep.equal({ id: 400 });
        addedWorkflowBinDefinitionExists = true;
        addedWorkflowBinDeleted = false;
        addedWorkflowBinName = request.body.name;
        addedWorkflowBinDescription = request.body.description;
        addedWorkflowBinQuery = request.body.query;
        addedWorkflowBinQueryType = request.body.queryType;
        addedWorkflowBinRequired = request.body.required;
        addedWorkflowBinEditable = request.body.editable;
        addedWorkflowBinEnabled = request.body.enabled;
        request.reply(addedWorkflowBinDefinition());
      }
    ).as('addWorkflowBinDefinition');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/definition',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          description: 'Updated QA bin',
          editable: false,
          enabled: true,
          id: 470,
          name: 'updated-qa-bin',
          query: 'select id from concepts',
          queryType: 'JPQL',
          required: false,
          workflowConfigId: 400
        });
        addedWorkflowBinName = request.body.name;
        addedWorkflowBinDescription = request.body.description;
        addedWorkflowBinQuery = request.body.query;
        addedWorkflowBinQueryType = request.body.queryType;
        addedWorkflowBinRequired = request.body.required;
        addedWorkflowBinEditable = request.body.editable;
        addedWorkflowBinEnabled = request.body.enabled;
        request.reply({});
      }
    ).as('updateWorkflowBinDefinition');

    cy.intercept(
      {
        method: 'DELETE',
        pathname: '/umls-server-rest/workflow/definition/470',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        addedWorkflowBinDeleted = true;
        request.reply({});
      }
    ).as('removeWorkflowBinDefinition');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/config/import',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.headers['content-type']).to.contain('multipart/form-data');
        importedWorkflowConfigExists = true;
        request.reply(importedWorkflowConfig());
      }
    ).as('importWorkflowConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/config/export',
        query: {
          projectId: '5',
          workflowId: '400'
        }
      },
      (request) => {
        request.reply({
          body: JSON.stringify(primaryWorkflowConfig()),
          headers: {
            'content-type': 'application/octet-stream'
          },
          statusCode: 200
        });
      }
    ).as('exportWorkflowConfig');

    cy.intercept(
      {
        method: 'PUT',
        pathname: '/umls-server-rest/workflow/config',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          adminConfig: false,
          mutuallyExclusive: true,
          queryStyle: 'REPORT',
          type: 'QUALITY_ASSURANCE'
        });
        addedWorkflowConfigExists = true;
        addedWorkflowConfigDeleted = false;
        addedWorkflowConfigType = request.body.type;
        addedWorkflowConfigQueryStyle = request.body.queryStyle;
        addedWorkflowConfigAdminConfig = request.body.adminConfig;
        addedWorkflowConfigMutuallyExclusive = request.body.mutuallyExclusive;
        request.reply(addedWorkflowConfig());
      }
    ).as('addWorkflowConfig');

    cy.intercept(
      {
        method: 'POST',
        pathname: '/umls-server-rest/workflow/config',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        expect(request.body).to.include({
          adminConfig: true,
          id: 460,
          mutuallyExclusive: false,
          queryStyle: 'OTHER',
          type: 'UPDATED_QA'
        });
        addedWorkflowConfigType = request.body.type;
        addedWorkflowConfigQueryStyle = request.body.queryStyle;
        addedWorkflowConfigAdminConfig = request.body.adminConfig;
        addedWorkflowConfigMutuallyExclusive = request.body.mutuallyExclusive;
        request.reply({});
      }
    ).as('updateWorkflowConfig');

    cy.intercept(
      {
        method: 'DELETE',
        pathname: '/umls-server-rest/workflow/config/460',
        query: {
          projectId: '5'
        }
      },
      (request) => {
        addedWorkflowConfigDeleted = true;
        request.reply({});
      }
    ).as('removeWorkflowConfig');

    cy.on('window:confirm', () => {
      return true;
    });

    cy.visit('/process', {
      onBeforeLoad(window) {
        window.localStorage.setItem('user', JSON.stringify(storedProjectUser));
      }
    });
    cy.wait([
      '@phase6Config',
      '@phase6Terminologies',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process Foundation').should('be.visible');
    cy.contains('Nightly Inversion').should('be.visible');
    cy.contains('INV-001').should('be.visible');
    cy.contains('Collect Source Files').should('be.visible');
    cy.get('#process-type').select('Inversion');
    cy.wait([
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Nightly Inversion').should('be.visible');
    cy.contains('Page 1 of 2').should('be.visible');
    cy.get('section[aria-labelledby="process-configs-title"]').within(() => {
      cy.contains('button', 'Import Process').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Import Process Config');
    cy.get('#process-import-file').selectFile(
      {
        contents: Cypress.Buffer.from('{"name":"Imported Inversion"}'),
        fileName: 'process.300.txt',
        mimeType: 'text/plain'
      },
      { force: true }
    );
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@importProcessConfig',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process config imported.').should('be.visible');
    cy.contains('Imported Inversion').should('be.visible');
    cy.get('section[aria-labelledby="process-configs-title"]').within(() => {
      cy.contains('tr', 'Nightly Inversion').click();
    });
    cy.wait(['@processConfigDetail', '@algorithmTypes']);
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('button', 'Export').click();
    });
    cy.wait('@exportProcessConfig');
    cy.contains('Process config exported.').should('be.visible');
    cy.get('section[aria-labelledby="process-configs-title"]').within(() => {
      cy.contains('button', 'Add Process').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Add Process Config');
    cy.get('#process-form-name').type('Custom Inversion');
    cy.get('#process-form-type').select('Inversion');
    cy.get('#process-form-terminology').should('have.value', 'NCI');
    cy.get('#process-form-version').should('have.value', '2026_05');
    cy.get('#process-form-input-path').type('/data/custom/input');
    cy.get('#process-form-log-path').type('/data/custom/logs');
    cy.get('#process-form-feedback-email').type('process@example.com');
    cy.get('#process-form-description').type('Custom inversion process');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@addProcessConfig',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process config added.').should('be.visible');
    cy.contains('Custom Inversion').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Edit Process Config');
    cy.get('#process-form-name').clear().type('Updated Custom Inversion');
    cy.get('#process-form-description').clear().type('Updated custom process');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@updateProcessConfig',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process config saved.').should('be.visible');
    cy.contains('Updated Custom Inversion').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('button', 'Delete').click();
    });
    cy.wait([
      '@removeProcessConfig',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process config removed.').should('be.visible');
    cy.contains('Updated Custom Inversion').should('not.exist');
    cy.wait('@algorithmTypes');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('button', 'Add Step').click();
    });
    cy.wait('@newAlgorithmConfig');
    cy.get('[role="dialog"]').should('contain.text', 'Add Algorithm Step');
    cy.get('#algorithm-form-type').select('normalize-metadata');
    cy.wait('@newAlgorithmConfig');
    cy.get('#algorithm-form-name').clear().type('Normalize Metadata Step');
    cy.get('#algorithm-form-description').clear().type('Normalize metadata before validation');
    cy.get('#algorithm-param-0').select('delta');
    cy.get('#algorithm-param-1').check();
    cy.contains('button', 'Validate').click();
    cy.wait('@validateAlgorithmConfig');
    cy.contains('Algorithm configuration successfully validated.').should('be.visible');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait(['@addAlgorithmConfig', '@processConfigDetail', '@algorithmTypes']);
    cy.contains('Algorithm step added.').should('be.visible');
    cy.contains('Normalize Metadata Step').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('tr', 'Normalize Metadata Step').within(() => {
        cy.contains('button', 'Edit').click();
      });
    });
    cy.wait('@algorithmConfigDetail');
    cy.get('[role="dialog"]').should('contain.text', 'Edit Algorithm Step');
    cy.get('#algorithm-form-name').clear().type('Updated Normalize Metadata');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait(['@updateAlgorithmConfig', '@processConfigDetail', '@algorithmTypes']);
    cy.contains('Algorithm step saved.').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('tr', 'Updated Normalize Metadata').within(() => {
        cy.contains('button', 'Disable').click();
      });
    });
    cy.wait([
      '@algorithmConfigDetail',
      '@updateAlgorithmConfig',
      '@processConfigDetail',
      '@algorithmTypes'
    ]);
    cy.contains('Algorithm step disabled.').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('tr', 'Updated Normalize Metadata').within(() => {
        cy.contains('button', 'Up').click();
      });
    });
    cy.wait(['@updateProcessConfig', '@processConfigDetail', '@algorithmTypes']);
    cy.contains('Algorithm step moved.').should('be.visible');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('tr', 'Updated Normalize Metadata').within(() => {
        cy.contains('button', 'Delete').click();
      });
    });
    cy.wait(['@removeAlgorithmConfig', '@processConfigDetail', '@algorithmTypes']);
    cy.contains('Algorithm step removed.').should('be.visible');
    cy.contains('Updated Normalize Metadata').should('not.exist');
    cy.get('aside[aria-label="Selected process config details"]').within(() => {
      cy.contains('button', 'Prepare').click();
    });
    cy.wait([
      '@prepareProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process prepared for execution.').should('be.visible');
    cy.contains('INV-002').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('button', 'Execute').click();
    });
    cy.wait([
      '@executeProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process execution started.').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('RUNNING').should('be.visible');
      cy.contains('button', 'Cancel').click();
    });
    cy.wait([
      '@cancelProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process cancellation requested.').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('STOPPED').should('be.visible');
      cy.contains('Collect Source Files').should('be.visible');
      cy.contains('button', 'Step').click();
    });
    cy.wait([
      '@stepProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process step started.').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('Validate Output').should('be.visible');
      cy.contains('button', 'Unstep').click();
    });
    cy.wait([
      '@unstepProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process unstep started.').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('No steps have been executed yet.').should('be.visible');
      cy.contains('button', 'Restart').click();
    });
    cy.wait([
      '@restartProcess',
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Process restart started.').should('be.visible');
    cy.get('aside[aria-label="Selected process execution details"]').within(() => {
      cy.contains('RUNNING').should('be.visible');
    });
    cy.get('section[aria-labelledby="process-configs-title"]').within(() => {
      cy.contains('button', 'Next').click();
    });
    cy.wait([
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Inversion Config 12').should('be.visible');
    cy.get('section[aria-labelledby="process-executions-title"]').within(() => {
      cy.contains('button', 'Next').click();
    });
    cy.wait([
      '@processConfigs',
      '@processExecutions',
      '@runningProcesses',
      '@processConfigDetail',
      '@processExecutionDetail'
    ]);
    cy.contains('Nightly Inversion 12').should('be.visible');

    cy.visit('/workflow');
    cy.wait([
      '@phase6Config',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.wait(['@workflowProject', '@workflowProjectUsers']);
    cy.contains('Workflow Foundation').should('be.visible');
    cy.contains('MUTUALLY_EXCLUSIVE').should('be.visible');
    cy.contains('demotions').should('be.visible');
    cy.contains('wrk26a_demotions_default_001').should('be.visible');
    cy.contains('Release QA').should('be.visible');
    cy.get('section[aria-labelledby="worklists-title"]').within(() => {
      cy.contains('tr', 'wrk26a_demotions_default_001').within(() => {
        cy.contains('button', 'View').click();
      });
    });
    cy.wait(['@workflowWorklistDetail', '@workflowWorklistLog', '@workflowReports']);
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Worklist: wrk26a_demotions_default_001')
      .and('contain.text', 'ASSIGN worklist');
    cy.get('section[aria-label="Workflow item notes"]').within(() => {
      cy.contains('Initial worklist note').should('be.visible');
      cy.get('#workflow-note-text').type('Follow-up worklist note');
      cy.contains('button', 'Add Note').click();
    });
    cy.wait('@addWorklistNote');
    cy.contains('Worklist note added.').should('be.visible');
    cy.get('section[aria-label="Workflow item notes"]').within(() => {
      cy.contains('Follow-up worklist note').should('be.visible');
      cy.contains('.note-item', 'Follow-up worklist note').within(() => {
        cy.contains('button', 'Remove').click();
      });
    });
    cy.wait('@removeWorklistNote');
    cy.contains('Worklist note removed.').should('be.visible');
    cy.get('section[aria-label="Workflow item notes"]').should(
      'not.contain.text',
      'Follow-up worklist note'
    );
    cy.get('section[aria-label="Worklist assignments"]').within(() => {
      cy.get('#workflow-assignment-user').should('not.contain', 'author3');
      cy.get('#workflow-assignment-user').select('author2');
      cy.get('#workflow-assignment-note').type('Assigning to author2');
      cy.contains('button', 'Assign Selected User').click();
    });
    cy.wait([
      '@performWorkflowAction',
      '@addWorklistNote',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist assigned to author2.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Authors')
      .and('contain.text', 'author2');
    cy.get('section[aria-label="Worklist assignments"]').within(() => {
      cy.contains('.assignee-chip', 'author2').within(() => {
        cy.contains('button', 'Remove').click();
      });
    });
    cy.wait([
      '@performWorkflowAction',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist unassigned from author2.').should('be.visible');
    cy.get('section[aria-label="Worklist assignments"] .assignee-chip').should(
      'not.exist'
    );
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Concept Reports')
      .and('contain.text', 'No generated concept report found.')
      .contains('button', 'Generate Report')
      .click();
    cy.wait('@generateWorkflowReport');
    cy.contains('Concept report generated.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', workflowReportFileName)
      .contains('button', 'Download Report')
      .click();
    cy.wait('@downloadWorkflowReport');
    cy.contains('Concept report downloaded.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Remove Report')
      .click();
    cy.wait('@removeWorkflowReport');
    cy.contains('Concept report removed.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]').should(
      'contain.text',
      'No generated concept report found.'
    );
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Close')
      .click();
    cy.get('section[aria-labelledby="workflow-list-detail-title"]').should(
      'not.exist'
    );
    cy.get('section[aria-labelledby="worklists-title"]').within(() => {
      cy.contains('tr', 'wrk26a_demotions_default_001').within(() => {
        cy.contains('button', 'Export').click();
      });
    });
    cy.wait('@exportWorklist');
    cy.contains('Worklist exported.').should('be.visible');
    cy.get('section[aria-labelledby="worklists-title"]').within(() => {
      cy.contains('tr', 'wrk26a_demotions_default_001').within(() => {
        cy.contains('button', 'Delete').click();
      });
    });
    cy.wait([
      '@removeWorklist',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist removed.').should('be.visible');
    cy.contains('wrk26a_demotions_default_001').should('not.exist');
    cy.get('section[aria-labelledby="checklists-title"]').within(() => {
      cy.contains('tr', 'Release QA').within(() => {
        cy.contains('button', 'View').click();
      });
    });
    cy.wait(['@workflowChecklistDetail', '@workflowChecklistLog']);
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Checklist: Release QA')
      .and('contain.text', 'ADD checklist note');
    cy.get('section[aria-label="Workflow item notes"]').within(() => {
      cy.contains('Initial checklist note').should('be.visible');
      cy.get('#workflow-note-text').type('Follow-up checklist note');
      cy.contains('button', 'Add Note').click();
    });
    cy.wait('@addChecklistNote');
    cy.contains('Checklist note added.').should('be.visible');
    cy.get('section[aria-label="Workflow item notes"]').within(() => {
      cy.contains('Follow-up checklist note').should('be.visible');
      cy.contains('.note-item', 'Follow-up checklist note').within(() => {
        cy.contains('button', 'Remove').click();
      });
    });
    cy.wait('@removeChecklistNote');
    cy.contains('Checklist note removed.').should('be.visible');
    cy.get('section[aria-label="Workflow item notes"]').should(
      'not.contain.text',
      'Follow-up checklist note'
    );
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Close')
      .click();
    cy.get('section[aria-labelledby="workflow-list-detail-title"]').should(
      'not.exist'
    );
    cy.get('section[aria-labelledby="checklists-title"]').within(() => {
      cy.contains('tr', 'Release QA').within(() => {
        cy.contains('button', 'Export').click();
      });
    });
    cy.wait('@exportChecklist');
    cy.contains('Checklist exported.').should('be.visible');
    cy.get('section[aria-labelledby="checklists-title"]').within(() => {
      cy.contains('tr', 'Release QA').within(() => {
        cy.contains('button', 'Delete').click();
      });
    });
    cy.wait([
      '@removeChecklist',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Checklist removed.').should('be.visible');
    cy.contains('Release QA').should('not.exist');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('th', 'Dates').should('exist');
      cy.contains('th', 'Cluster').should('exist');
      cy.contains('th', 'Assigned').should('exist');
      cy.contains('Created:').should('be.visible');
      cy.contains('Run Time: 12 sec').should('be.visible');
      cy.contains('td', 'default').should('exist');
      cy.contains('td', 'chem').should('exist');
      cy.contains('td', '4').should('exist');
      cy.contains('tr', 'default').within(() => {
        cy.contains('button', 'Add Checklist').click();
      });
    });
    cy.get('[role="dialog"]').should('contain.text', 'Add Checklist');
    cy.get('#workflow-checklist-form-name').type('Created QA Checklist');
    cy.get('#workflow-checklist-form-description').type(
      'Created from default demotion clusters'
    );
    cy.get('#workflow-checklist-form-cluster-count')
      .invoke('val', '50')
      .trigger('input');
    cy.get('#workflow-checklist-form-skip-count').clear().type('1');
    cy.get('#workflow-checklist-form-sort-order').select('indexedData');
    cy.get('#workflow-checklist-form-exclude-worklists').check();
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@createChecklist',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Checklist created.').should('be.visible');
    cy.contains('Created QA Checklist').should('be.visible');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('tr', 'default').within(() => {
        cy.contains('button', 'Add Worklist').click();
      });
    });
    cy.get('[role="dialog"]').should('contain.text', 'Add Worklist');
    cy.get('#workflow-worklist-form-cluster-count')
      .invoke('val', '2')
      .trigger('input');
    cy.get('#workflow-worklist-form-number').clear().type('1');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@createWorklist',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('wrk26a_demotions_default_002').should('be.visible');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('button', 'Regenerate Bins').click();
    });
    cy.wait(['@clearWorkflowBins', '@regenerateWorkflowBins', '@workflowBins']);
    cy.contains('Workflow bins regenerated.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('button', 'Add Bin').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Add Workflow Bin');
    cy.wait('@autofixAlgorithms');
    cy.get('#workflow-bin-form-name').type('qa-bin');
    cy.get('#workflow-bin-form-description').type('Quality assurance bin');
    cy.get('#workflow-bin-form-position-after').select('401');
    cy.get('#workflow-bin-form-query').type('select * from concepts');
    cy.get('#workflow-bin-form-required').check();
    cy.get('.dialog-form').within(() => {
      cy.contains('button', 'Test').click();
    });
    cy.wait('@workflowQueryTest');
    cy.contains('Query met validation requirements.').should('be.visible');
    cy.contains('Full Test Result Count: 2').should('be.visible');
    cy.contains('100001, 100001').should('be.visible');
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait(['@addWorkflowBinDefinition', '@workflowBins']);
    cy.contains('Workflow bin added.').should('be.visible');
    cy.contains('qa-bin').should('exist');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('tr', 'qa-bin').within(() => {
        cy.contains('button', 'Edit').click();
      });
    });
    cy.wait('@workflowBinDefinition');
    cy.get('[role="dialog"]').should('contain.text', 'Edit Workflow Bin');
    cy.get('#workflow-bin-form-name').clear().type('updated-qa-bin');
    cy.get('#workflow-bin-form-description').clear().type('Updated QA bin');
    cy.get('#workflow-bin-form-query-type').select('JPQL');
    cy.get('#workflow-bin-form-query').clear().type('select id from concepts');
    cy.get('#workflow-bin-form-required').uncheck();
    cy.get('#workflow-bin-form-editable').uncheck();
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait(['@updateWorkflowBinDefinition', '@workflowBins']);
    cy.contains('Workflow bin saved.').should('be.visible');
    cy.contains('updated-qa-bin').should('exist');
    cy.get('section[aria-labelledby="workflow-bins-title"]').within(() => {
      cy.contains('tr', 'updated-qa-bin').within(() => {
        cy.contains('button', 'Delete').click();
      });
    });
    cy.wait([
      '@workflowBinDefinition',
      '@removeWorkflowBinDefinition',
      '@workflowBins'
    ]);
    cy.contains('Workflow bin removed.').should('be.visible');
    cy.contains('updated-qa-bin').should('not.exist');
    cy.get('section[aria-labelledby="workflow-configs-title"]').within(() => {
      cy.contains('button', 'Export').click();
    });
    cy.wait('@exportWorkflowConfig');
    cy.contains('Workflow config exported.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-configs-title"]').within(() => {
      cy.contains('button', 'Import Workflow').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Import Workflow Config');
    cy.get('#workflow-import-file').selectFile(
      {
        contents: Cypress.Buffer.from('{"type":"IMPORTED_CONFIG"}'),
        fileName: 'workflow.400.txt',
        mimeType: 'text/plain'
      },
      { force: true }
    );
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@importWorkflowConfig',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Workflow config imported.').should('be.visible');
    cy.contains('IMPORTED_CONFIG').should('be.visible');
    cy.contains('imported-bin').should('be.visible');
    cy.get('section[aria-labelledby="workflow-configs-title"]').within(() => {
      cy.contains('button', 'Add Config').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Add Workflow Config');
    cy.get('#workflow-config-form-type').type('QUALITY_ASSURANCE');
    cy.get('#workflow-config-form-query-style').select('REPORT');
    cy.get('#workflow-config-form-mutually-exclusive').check();
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@addWorkflowConfig',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Workflow config added.').should('be.visible');
    cy.contains('QUALITY_ASSURANCE').should('be.visible');
    cy.get('section[aria-labelledby="workflow-configs-title"]').within(() => {
      cy.contains('button', 'Edit').click();
    });
    cy.get('[role="dialog"]').should('contain.text', 'Edit Workflow Config');
    cy.get('#workflow-config-form-type').clear().type('UPDATED_QA');
    cy.get('#workflow-config-form-query-style').select('OTHER');
    cy.get('#workflow-config-form-mutually-exclusive').uncheck();
    cy.get('#workflow-config-form-admin').check();
    cy.get('.dialog-form button[type="submit"]').click();
    cy.wait([
      '@updateWorkflowConfig',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Workflow config saved.').should('be.visible');
    cy.contains('UPDATED_QA').should('be.visible');
    cy.get('section[aria-labelledby="workflow-configs-title"]').within(() => {
      cy.contains('button', 'Delete').click();
    });
    cy.wait([
      '@removeWorkflowConfig',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Workflow config removed.').should('be.visible');
    cy.contains('UPDATED_QA').should('not.exist');

    cy.then(() => {
      workflowWorklistDeleted = false;
      workflowWorklistAuthorAvailable = true;
      workflowWorklistAuthors = [];
      workflowWorklistReviewerAvailable = false;
      workflowWorklistReviewers = [];
      workflowWorklistStateHistory = {
        Created: '2026-06-10T08:30:00Z'
      };
      workflowWorklistStatus = 'NEW';
    });

    cy.window().then((window) => {
      window.localStorage.setItem(
        'user',
        JSON.stringify({
          ...storedProjectUser,
          projectRoleMap: {
            '5': 'AUTHOR'
          },
          userPreferences: {
            ...storedProjectUser.userPreferences,
            lastProjectRole: 'AUTHOR',
            lastTab: '/workflow'
          }
        })
      );
    });
    cy.visit('/workflow');
    cy.wait([
      '@phase6Config',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.get('section[aria-labelledby="worklists-title"]').within(() => {
      cy.contains('tr', 'wrk26a_demotions_default_001').within(() => {
        cy.contains('button', 'View').click();
      });
    });
    cy.wait(['@workflowWorklistDetail', '@workflowWorklistLog']);
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Available for author assignment')
      .and('contain.text', 'Authors')
      .and('contain.text', 'n/a');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Assign to me')
      .click();
    cy.wait([
      '@performWorkflowAction',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist assigned.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Authors')
      .and('contain.text', 'dss')
      .contains('button', 'Unassign me')
      .click();
    cy.wait([
      '@performWorkflowAction',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist unassigned.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Available for author assignment')
      .and('contain.text', 'Authors')
      .and('contain.text', 'n/a');

    cy.then(() => {
      workflowWorklistAuthorAvailable = false;
      workflowWorklistAuthors = ['author1'];
      workflowWorklistReviewerAvailable = false;
      workflowWorklistReviewers = ['dss'];
      workflowWorklistStateHistory = {
        Created: '2026-06-10T08:30:00Z',
        'Review Assigned': '2026-06-10T10:40:00Z'
      };
      workflowWorklistStatus = 'READY_FOR_PUBLICATION';
    });
    cy.window().then((window) => {
      window.localStorage.setItem(
        'user',
        JSON.stringify({
          ...storedProjectUser,
          projectRoleMap: {
            '5': 'REVIEWER'
          },
          userPreferences: {
            ...storedProjectUser.userPreferences,
            lastProjectRole: 'REVIEWER',
            lastTab: '/workflow'
          }
        })
      );
    });
    cy.visit('/workflow');
    cy.wait([
      '@phase6Config',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.get('section[aria-labelledby="worklists-title"]').within(() => {
      cy.contains('tr', 'wrk26a_demotions_default_001').within(() => {
        cy.contains('button', 'View').click();
      });
    });
    cy.wait(['@workflowWorklistDetail', '@workflowWorklistLog']);
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Workflow State')
      .and('contain.text', 'Review Assigned')
      .and('contain.text', 'Reviewers')
      .and('contain.text', 'dss');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Stamp')
      .click();
    cy.wait([
      '@stampWorklist',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist stamped.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .contains('button', 'Finish')
      .click();
    cy.wait([
      '@performWorkflowAction',
      '@workflowConfigs',
      '@workflowEpochs',
      '@workflowChecklists',
      '@workflowWorklists',
      '@workflowBins'
    ]);
    cy.contains('Worklist finished.').should('be.visible');
    cy.get('section[aria-labelledby="workflow-list-detail-title"]')
      .should('contain.text', 'Review Done')
      .and('not.contain.text', 'Stamp')
      .and('not.contain.text', 'Finish');
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
