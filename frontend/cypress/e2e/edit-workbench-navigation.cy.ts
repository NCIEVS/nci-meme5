describe('Edit workbench navigation', () => {
  const activityId = 'wrk26b_snomedct_us_default_022';
  const records = [123, 456, 789].map((id, index) => ({
    id: index + 1,
    clusterId: index + 1,
    concepts: [{ id }]
  }));

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
    // Keep this regression suite independent of any running backend or database.
    cy.intercept('/umls-server-rest/**', (request) => {
      throw new Error(`Unexpected API request: ${request.method} ${request.url}`);
    });
    cy.intercept('GET', '/umls-server-rest/configure/properties', {
      'deploy.enabled.tabs': 'edit',
      'deploy.login.enabled': 'true',
      'deploy.license.enabled': 'false'
    });
    cy.intercept('GET', '/umls-server-rest/project/3', {
      id: 3,
      editingEnabled: true,
      terminology: 'NCIMTH',
      version: 'latest',
      userRoleMap: { editor: 'AUTHOR' }
    });
    cy.intercept('GET', '/umls-server-rest/metadata/all/NCIMTH/latest', {
      keyValuePairLists: []
    });
    cy.intercept('GET', '/umls-server-rest/metadata/sty/NCIMTH/latest', {
      types: [{ expandedForm: 'Finding', typeId: 'T033' }]
    });
    cy.intercept('GET', /\/umls-server-rest\/content\/concept\/.+\?projectId=3$/, (request) => {
      const id = Number(new URL(request.url).pathname.split('/').pop()!.replace(/^C/, ''));
      request.reply({
        id,
        name: `Foreign body in test site ${id}`,
        terminology: 'NCIMTH',
        terminologyId: `C${id}`,
        version: 'latest',
        type: 'CONCEPT',
        lastModified: 1770000000000,
        workflowStatus: 'NEEDS_REVIEW',
        semanticTypes: [{ id: id + 1000, semanticType: 'Finding' }]
      });
    }).as('concept');
    cy.intercept('POST', '/umls-server-rest/workflow/worklist/22/records?projectId=3', {
      records,
      totalCount: records.length
    }).as('records');
    cy.intercept('POST', '/umls-server-rest/meta/concept/approve*', (request) => {
      const params = new URL(request.url).searchParams;
      expect(params.get('projectId')).to.equal('3');
      expect(params.get('activityId')).to.equal(activityId);
      expect(params.get('lastModified')).to.equal('1770000000000');
      expect(params.get('overrideWarnings')).to.equal('false');
      request.reply({ valid: true, errors: [], warnings: [] });
    }).as('approve');
  });

  function visitWorkbench(extraParams = ''): void {
    cy.visit(
      `/#/edit/semantic-types?type=CONCEPT&terminology=NCIMTH&version=latest&terminologyId=C123&componentId=123&projectId=3&recordId=1&worklistId=22&activityId=${activityId}${extraParams}`,
      {
        onBeforeLoad(window) {
          window.localStorage.setItem('user', JSON.stringify({
            applicationRole: 'USER',
            authToken: 'TEST',
            userName: 'editor',
            projectRoleMap: { '3': 'AUTHOR' },
            userPreferences: { lastProjectId: 3, lastProjectRole: 'AUTHOR', properties: {} }
          }));
        }
      }
    );
    cy.contains('.styw-concept', 'Foreign body in test site 123').should('be.visible');
    cy.contains('button', 'Approve/Next').should('not.be.disabled');
  }

  function expectConcept(id: number, recordId: number): void {
    cy.contains('.styw-concept', `Foreign body in test site ${id}`).should('be.visible');
    cy.location().should((location) => {
      const params = new URLSearchParams(location.hash.split('?')[1]);
      expect(location.search).to.equal('');
      expect(params.get('componentId')).to.equal(String(id));
      expect(params.get('recordId')).to.equal(String(recordId));
      expect(params.get('worklistId')).to.equal('22');
      expect(params.get('activityId')).to.equal(activityId);
      expect(params.has('terminologyId')).to.equal(false);
      expect(params.has('id')).to.equal(false);
    });
  }

  it('advances repeatedly within the STY screen and reports the end of the worklist', () => {
    visitWorkbench('&id=C123');
    cy.contains('button', /^Next$/).click();
    cy.wait('@records');
    expectConcept(456, 2);
    cy.contains('button', /^Next$/).click();
    cy.wait('@records');
    expectConcept(789, 3);

    const alert = cy.stub().as('endAlert');
    cy.on('window:alert', alert);
    cy.contains('button', /^Next$/).click();
    cy.wait('@records');
    cy.get('@endAlert').should('have.been.calledOnceWith', 'No more clusters in the worklist.');
    expectConcept(789, 3);
    cy.get('@approve.all').should('have.length', 0);
  });

  it('approves the current concept before advancing, then approves the newly loaded concept', () => {
    visitWorkbench();
    cy.contains('button', 'Approve/Next').click();
    cy.wait('@approve').its('request.url').should('include', 'conceptId=123');
    cy.wait('@records');
    expectConcept(456, 2);

    cy.contains('button', 'Approve/Next').click();
    cy.wait('@approve').its('request.url').should('include', 'conceptId=456');
    cy.wait('@records');
    expectConcept(789, 3);
  });

  it('advances through checklist records using the same hash route', () => {
    cy.intercept('POST', '/umls-server-rest/workflow/checklist/22/records?projectId=3', {
      objects: records,
      totalCount: records.length
    }).as('checklistRecords');
    visitWorkbench('&isChecklist=true');
    cy.contains('button', /^Next$/).click();
    cy.wait('@checklistRecords');
    expectConcept(456, 2);
    cy.get('@records.all').should('have.length', 0);
  });

  for (const validation of [
    { valid: false, errors: ['Approval blocked.'], warnings: [] },
    { valid: true, errors: [], warnings: ['Approval needs review.'] }
  ]) {
    it(`stays on the current concept when approval returns ${validation.errors.length ? 'errors' : 'warnings'}`, () => {
      cy.intercept('POST', '/umls-server-rest/meta/concept/approve*', validation).as('blockedApproval');
      const alert = cy.stub().as('validationAlert');
      cy.on('window:alert', alert);
      visitWorkbench();
      cy.contains('button', 'Approve/Next').click();
      cy.wait('@blockedApproval');
      cy.get('@validationAlert').should('have.been.calledOnce');
      cy.contains('.styw-concept', 'Foreign body in test site 123').should('be.visible');
      cy.location('hash').should('include', 'recordId=1');
      cy.get('@records.all').should('have.length', 0);
    });
  }
});
