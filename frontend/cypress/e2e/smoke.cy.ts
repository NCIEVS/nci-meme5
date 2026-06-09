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
