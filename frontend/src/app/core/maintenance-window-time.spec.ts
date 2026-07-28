import {
  easternDateInputValue,
  easternTimeInputValue,
  easternWallTimeToDate,
  formatEasternDateTime
} from './maintenance-window-time';

describe('maintenance window time helpers', () => {
  it('interprets summer maintenance wall time as Eastern daylight time', () => {
    expect(easternWallTimeToDate('2026-07-25', '03:00')?.toISOString()).toBe(
      '2026-07-25T07:00:00.000Z'
    );
  });

  it('interprets winter maintenance wall time as Eastern standard time', () => {
    expect(easternWallTimeToDate('2026-01-25', '03:00')?.toISOString()).toBe(
      '2026-01-25T08:00:00.000Z'
    );
  });

  it('formats maintenance dates in Eastern time', () => {
    expect(formatEasternDateTime(Date.UTC(2026, 6, 25, 7))).toContain(
      'Jul 25'
    );
  });

  it('formats Eastern date and time input values', () => {
    const date = new Date(Date.UTC(2026, 6, 25, 7));

    expect(easternDateInputValue(date)).toBe('2026-07-25');
    expect(easternTimeInputValue(date)).toBe('03:00');
  });
});
