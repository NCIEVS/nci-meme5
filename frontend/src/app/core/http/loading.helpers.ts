const UI_BLOCKING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export function shouldBlockUiForRequestMethod(method: string): boolean {
  return UI_BLOCKING_METHODS.has(method.toUpperCase());
}
