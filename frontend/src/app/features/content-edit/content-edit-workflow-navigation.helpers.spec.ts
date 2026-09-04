import { nextWorkflowRecordNavigation } from './content-edit-workflow-navigation.helpers';

describe('content edit workflow navigation helpers', () => {
  it('selects the next record from the loaded page', () => {
    expect(
      nextWorkflowRecordNavigation(
        2,
        [{ id: 1 }, { id: 2 }, { id: 3 }],
        1,
        10,
        3
      )
    ).toEqual({
      kind: 'record',
      recordIndex: 2
    });
  });

  it('loads the next page after the last loaded record when more records exist', () => {
    expect(
      nextWorkflowRecordNavigation(
        10,
        [
          { id: 1 },
          { id: 2 },
          { id: 3 },
          { id: 4 },
          { id: 5 },
          { id: 6 },
          { id: 7 },
          { id: 8 },
          { id: 9 },
          { id: 10 }
        ],
        1,
        10,
        11
      )
    ).toEqual({
      kind: 'page',
      page: 2
    });
  });

  it('returns null at the final record of the final page', () => {
    expect(
      nextWorkflowRecordNavigation(11, [{ id: 11 }], 2, 10, 11)
    ).toBeNull();
  });
});
