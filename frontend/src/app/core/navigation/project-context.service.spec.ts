import { resolveProjectContextId } from './project-context.helpers';

describe('project context resolution', () => {
  it('uses the selected project preference when present', () => {
    expect(resolveProjectContextId('12', { '34': 'REVIEWER' })).toBe(12);
  });

  it('uses the only assigned project when no preference exists', () => {
    expect(resolveProjectContextId(null, { '34': 'REVIEWER' })).toBe(34);
  });

  it('requires an explicit selected project when multiple projects are assigned', () => {
    expect(
      resolveProjectContextId(null, {
        '34': 'REVIEWER',
        '56': 'AUTHOR'
      })
    ).toBeNull();
  });
});
