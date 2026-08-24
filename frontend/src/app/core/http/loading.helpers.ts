const UI_BLOCKING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export function shouldBlockUiForRequestMethod(
  method: string,
  override: boolean | null = null
): boolean {
  if (override !== null) {
    return override;
  }

  return UI_BLOCKING_METHODS.has(method.toUpperCase());
}
