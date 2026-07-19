/* eslint-disable react-refresh/only-export-components -- context module exports Provider and hook */
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

const STORAGE = {
  colorMode: 'rgf-color-mode',
  palette: 'rgf-palette',
  highContrast: 'rgf-high-contrast',
  fontScale: 'rgf-font-scale',
  reduceMotion: 'rgf-reduce-motion',
} as const

const LEGACY_COMFORTABLE_TEXT_KEY = 'rgf-comfortable-text'

export type ColorMode = 'dark' | 'light' | 'system'
export type PaletteId = 'classic' | 'eco'
export type FontScaleId = 'standard' | 'comfortable' | 'large'
export type ReduceMotionPref = 'system' | 'always' | 'never'

export interface ThemeContextValue {
  colorMode: ColorMode
  setColorMode: React.Dispatch<React.SetStateAction<ColorMode>>
  resolvedTheme: 'dark' | 'light'
  palette: PaletteId
  setPalette: React.Dispatch<React.SetStateAction<PaletteId>>
  highContrast: boolean
  setHighContrast: React.Dispatch<React.SetStateAction<boolean>>
  fontScale: FontScaleId
  setFontScale: React.Dispatch<React.SetStateAction<FontScaleId>>
  reduceMotion: ReduceMotionPref
  setReduceMotion: React.Dispatch<React.SetStateAction<ReduceMotionPref>>
  prefersReducedMotion: boolean
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function readStoredColorMode(): ColorMode {
  try {
    const v = localStorage.getItem(STORAGE.colorMode)
    if (v === 'dark' || v === 'light' || v === 'system') return v
  } catch {
    /* ignore */
  }
  return 'dark'
}

function readStoredPalette(): PaletteId {
  try {
    const v = localStorage.getItem(STORAGE.palette)
    if (v === 'classic' || v === 'eco') return v
  } catch {
    /* ignore */
  }
  return 'classic'
}

function readStoredFontScale(): FontScaleId {
  try {
    const v = localStorage.getItem(STORAGE.fontScale)
    if (v === 'standard' || v === 'comfortable' || v === 'large') return v
    const legacy = localStorage.getItem(LEGACY_COMFORTABLE_TEXT_KEY)
    if (legacy === '1' || legacy === 'true') return 'comfortable'
  } catch {
    /* ignore */
  }
  return 'standard'
}

function readBool(key: string, defaultValue = false): boolean {
  try {
    const v = localStorage.getItem(key)
    if (v === '1' || v === 'true') return true
    if (v === '0' || v === 'false') return false
  } catch {
    /* ignore */
  }
  return defaultValue
}

function readReduceMotion(): ReduceMotionPref {
  try {
    const v = localStorage.getItem(STORAGE.reduceMotion)
    if (v === 'always' || v === 'never' || v === 'system') return v
  } catch {
    /* ignore */
  }
  return 'system'
}

function resolveTheme(colorMode: ColorMode, systemDark: boolean): 'dark' | 'light' {
  if (colorMode === 'system') return systemDark ? 'dark' : 'light'
  return colorMode
}

/**
 * Provides display preferences (color theme, palette, contrast, text size, motion) and syncs them
 * to `document.documentElement` for global CSS.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [colorMode, setColorMode] = useState<ColorMode>(readStoredColorMode)
  const [palette, setPalette] = useState<PaletteId>(readStoredPalette)
  const [highContrast, setHighContrast] = useState(() => readBool(STORAGE.highContrast, false))
  const [fontScale, setFontScale] = useState<FontScaleId>(readStoredFontScale)
  const [reduceMotion, setReduceMotion] = useState<ReduceMotionPref>(readReduceMotion)
  const [systemDark, setSystemDark] = useState(() => {
    if (globalThis.window === undefined) return true
    return globalThis.window.matchMedia('(prefers-color-scheme: dark)').matches
  })

  useEffect(() => {
    const mq = globalThis.window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => setSystemDark(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const [prefersReducedMotion, setPrefersReducedMotion] = useState(() => {
    if (globalThis.window === undefined) return false
    return globalThis.window.matchMedia('(prefers-reduced-motion: reduce)').matches
  })

  useEffect(() => {
    const mq = globalThis.window.matchMedia('(prefers-reduced-motion: reduce)')
    const onChange = () => setPrefersReducedMotion(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const effectiveMotionReduced =
    reduceMotion === 'always' || (reduceMotion === 'system' && prefersReducedMotion)

  useEffect(() => {
    const root = document.documentElement
    const theme = resolveTheme(colorMode, systemDark)
    root.dataset.theme = theme
    root.style.colorScheme = theme

    if (palette === 'eco') {
      root.dataset.palette = 'eco'
    } else {
      delete root.dataset.palette
    }

    if (highContrast) {
      root.dataset.a11yContrast = 'high'
    } else {
      delete root.dataset.a11yContrast
    }

    if (fontScale === 'standard') {
      delete root.dataset.fontScale
    } else {
      root.dataset.fontScale = fontScale
    }

    root.dataset.motion = effectiveMotionReduced ? 'reduce' : 'normal'

    try {
      localStorage.setItem(STORAGE.colorMode, colorMode)
      localStorage.setItem(STORAGE.palette, palette)
      localStorage.setItem(STORAGE.highContrast, highContrast ? '1' : '0')
      localStorage.setItem(STORAGE.fontScale, fontScale)
      localStorage.setItem(STORAGE.reduceMotion, reduceMotion)
    } catch {
      /* ignore */
    }
  }, [
    colorMode,
    systemDark,
    palette,
    highContrast,
    fontScale,
    reduceMotion,
    effectiveMotionReduced,
  ])

  const resolvedTheme = resolveTheme(colorMode, systemDark)

  const value = useMemo<ThemeContextValue>(
    () => ({
      colorMode,
      setColorMode,
      resolvedTheme,
      palette,
      setPalette,
      highContrast,
      setHighContrast,
      fontScale,
      setFontScale,
      reduceMotion,
      setReduceMotion,
      prefersReducedMotion,
    }),
    [
      colorMode,
      resolvedTheme,
      palette,
      highContrast,
      fontScale,
      reduceMotion,
      prefersReducedMotion,
    ],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return ctx
}
