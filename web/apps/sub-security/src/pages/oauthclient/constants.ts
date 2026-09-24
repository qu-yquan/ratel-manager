import type {EnumOption, KeyValueOption} from './types';

export function toEnumOptions<T extends string>(options: KeyValueOption<T>[] | undefined): EnumOption<T>[] {
  return (options || []).map((item) => ({
    label: item.value,
    value: item.key,
  }));
}

export function toLabelMap<T extends string>(options: KeyValueOption<T>[] | undefined): Map<T, string> {
  return new Map((options || []).map((item) => [item.key, item.value]));
}
