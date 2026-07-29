import { appendNotificationMessage } from './notification.helpers';

describe('notification helpers', () => {
  it('deduplicates active messages with the same level and text', () => {
    const messages = appendNotificationMessage([], {
      id: 1,
      level: 'error',
      message: 'Could not load data.'
    });

    expect(
      appendNotificationMessage(messages, {
        id: 2,
        level: 'error',
        message: 'Could not load data.'
      })
    ).toBe(messages);
  });

  it('keeps messages with different text', () => {
    const messages = appendNotificationMessage(
      [
        {
          id: 1,
          level: 'error',
          message: 'Could not load data.'
        }
      ],
      {
        id: 2,
        level: 'error',
        message: 'Could not save data.'
      }
    );

    expect(messages).toEqual([
      {
        id: 1,
        level: 'error',
        message: 'Could not load data.'
      },
      {
        id: 2,
        level: 'error',
        message: 'Could not save data.'
      }
    ]);
  });
});
