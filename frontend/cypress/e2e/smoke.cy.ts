describe('Angular 20 shell', () => {
  it('loads the migration shell', () => {
    cy.visit('/');

    cy.contains('NCI-META Angular 20').should('be.visible');
    cy.contains('Angular 20 standalone bootstrap').should('be.visible');
  });
});
