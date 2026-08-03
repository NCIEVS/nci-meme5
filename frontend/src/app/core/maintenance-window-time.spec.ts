import {
  easternDateInputValue,
  easternTimeInputValue,
  easternWallTimeToDate,
  formatEasternDate,
  formatEasternDateTime,
  formatEasternTime
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

  it('formats timestamps in Eastern daylight time', () => {
    const formatted = formatEasternDateTime('2026-06-30T17:10:45.000Z');

    expect(formatted).toContain('1:10 PM EDT');
  });

  it('formats date-only values in Eastern time', () => {
    expect(formatEasternDate('2026-06-30T17:10:45.000Z')).toContain('Jun 30');
  });

  it('formats time-only values in Eastern daylight time', () => {
    expect(formatEasternTime('2026-06-30T17:10:45.000Z')).toContain(
      '1:10 PM EDT'
    );
  });

  it('formats Eastern date and time input values', () => {
    const date = new Date(Date.UTC(2026, 6, 25, 7));

    expect(easternDateInputValue(date)).toBe('2026-07-25');
    expect(easternTimeInputValue(date)).toBe('03:00');
  });
});
