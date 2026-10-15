// Only "folder/name.webp" with lowercase letters, digits and hyphens: the icons are files of public/ (D30)
const ICON_PATH = /^[a-z0-9-]+\/[a-z0-9-]+\.webp$/

/**
 * The address of a character icon on this site, or null when the path from the API does not look like an icon of
 * public/. A path such as "/evil.example/x.webp" would otherwise become "//evil.example/x.webp", another site.
 */
export function characterIconUrl(icon: string | null): string | null {
  return icon !== null && ICON_PATH.test(icon) ? `/${icon}` : null
}
