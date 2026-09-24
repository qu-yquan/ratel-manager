export function textToLines(value?: string): string[] {
  return (value || '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function linesToText(values?: string[]): string {
  return (values || []).join('\n');
}
