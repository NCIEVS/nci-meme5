import { MaintenanceWindow } from './maintenance-window.models';

export const EASTERN_TIME_ZONE = 'America/New_York';

const easternDateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
  month: 'short',
  timeZone: EASTERN_TIME_ZONE,
  timeZoneName: 'short',
  weekday: 'short',
  year: 'numeric'
});

const easternInputFormatter = new Intl.DateTimeFormat('en-US', {
  day: '2-digit',
  hour: '2-digit',
  hourCycle: 'h23',
  minute: '2-digit',
  month: '2-digit',
  timeZone: EASTERN_TIME_ZONE,
  year: 'numeric'
});

export function dateFromLegacyValue(
  value: string | number | null | undefined
): Date | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function easternDateInputValue(date = new Date()): string {
  const parts = easternParts(date);
  return `${parts['year']}-${parts['month']}-${parts['day']}`;
}

export function easternTimeInputValue(date = new Date()): string {
  const parts = easternParts(date);
  return `${parts['hour']}:${parts['minute']}`;
}

export function easternWallTimeToDate(
  dateValue: string,
  timeValue: string
): Date | null {
  const dateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateValue);
  const timeMatch = /^(\d{2}):(\d{2})$/.exec(timeValue);

  if (!dateMatch || !timeMatch) {
    return null;
  }

  const year = Number(dateMatch[1]);
  const month = Number(dateMatch[2]);
  const day = Number(dateMatch[3]);
  const hour = Number(timeMatch[1]);
  const minute = Number(timeMatch[2]);

  if (
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > 31 ||
    hour > 23 ||
    minute > 59
  ) {
    return null;
  }

  const wallTimeAsUtc = Date.UTC(year, month - 1, day, hour, minute);
  let utcTime = wallTimeAsUtc;

  for (let i = 0; i < 3; i += 1) {
    const offsetMinutes = timeZoneOffsetMinutes(new Date(utcTime));
    const nextUtcTime = wallTimeAsUtc - offsetMinutes * 60_000;

    if (nextUtcTime === utcTime) {
      break;
    }

    utcTime = nextUtcTime;
  }

  return new Date(utcTime);
}

export function formatEasternDateTime(
  value: string | number | null | undefined
): string {
  const date = dateFromLegacyValue(value);
  return date ? easternDateTimeFormatter.format(date) : 'n/a';
}

export function formatMaintenanceWindowRange(
  window: MaintenanceWindow | null | undefined
): string {
  if (!window) {
    return 'n/a';
  }

  return `${formatEasternDateTime(window.startDate)} to ${formatEasternDateTime(
    window.endDate
  )}`;
}

function easternParts(date: Date): Record<string, string> {
  return easternInputFormatter
    .formatToParts(date)
    .reduce<Record<string, string>>((parts, part) => {
      if (part.type !== 'literal') {
        parts[part.type] = part.value;
      }

      return parts;
    }, {});
}

function timeZoneOffsetMinutes(date: Date): number {
  const formatter = new Intl.DateTimeFormat('en-US', {
    day: '2-digit',
    hour: '2-digit',
    hourCycle: 'h23',
    minute: '2-digit',
    month: '2-digit',
    timeZone: EASTERN_TIME_ZONE,
    timeZoneName: 'shortOffset',
    year: 'numeric'
  });
  const timeZoneName =
    formatter
      .formatToParts(date)
      .find((part) => part.type === 'timeZoneName')?.value ?? 'GMT';
  const match = /^GMT(?:(?<sign>[+-])(?<hours>\d{1,2})(?::(?<minutes>\d{2}))?)?$/.exec(
    timeZoneName
  );

  if (!match?.groups?.['hours']) {
    return 0;
  }

  const minutes =
    Number(match.groups['hours']) * 60 + Number(match.groups['minutes'] ?? '0');
  return match.groups['sign'] === '-' ? -minutes : minutes;
}
