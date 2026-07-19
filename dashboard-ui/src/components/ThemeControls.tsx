import { useEffect, useId, useRef, useState } from 'react'
import type { ColorMode, FontScaleId, PaletteId } from '../context/ThemeContext'
import { useTheme } from '../context/ThemeContext'

const COLOR_MODES: { value: ColorMode; label: string; emoji: string; hint: string }[] = [
  { value: 'light', label: 'Bright', emoji: '☀️', hint: 'Light background' },
  { value: 'dark', label: 'Dark', emoji: '🌙', hint: 'Easy on the eyes' },
  { value: 'system', label: 'System', emoji: '🖥️', hint: 'Device setting' },
]

const PALETTES: { value: PaletteId; title: string; emoji: string; desc: string }[] = [
  {
    value: 'classic',
    title: 'Classic',
    emoji: '⚡',
    desc: 'Cyan highlights (original look)',
  },
  {
    value: 'eco',
    title: 'Eco',
    emoji: '🌿',
    desc: 'Green tech look; blue for focus & power hints',
  },
]

const FONT_SCALES: { value: FontScaleId; label: string; hint: string }[] = [
  { value: 'standard', label: 'Standard', hint: 'Default size' },
  { value: 'comfortable', label: 'Comfortable', hint: '+12.5%' },
  { value: 'large', label: 'Large', hint: '+25%' },
]

/**
 * Floating display & accessibility settings (opens above a fixed corner button).
 */
export default function ThemeControls() {
  const id = useId()
  const panelId = `${id}-panel`
  const titleId = `${id}-title`
  const highContrastInputId = `${id}-high-contrast`
  const [open, setOpen] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)

  const {
    colorMode,
    setColorMode,
    palette,
    setPalette,
    highContrast,
    setHighContrast,
    fontScale,
    setFontScale,
    reduceMotion,
    setReduceMotion,
    prefersReducedMotion,
  } = useTheme()

  useEffect(() => {
    const el = dialogRef.current
    if (!el) return undefined
    if (open) {
      if (!el.open) el.showModal()
    } else if (el.open) {
      el.close()
    }
    return undefined
  }, [open])

  useEffect(() => {
    const el = dialogRef.current
    if (!el || !open) return undefined

    function onBackdropPointerDown(ev: PointerEvent) {
      if (ev.target === el) setOpen(false)
    }

    el.addEventListener('pointerdown', onBackdropPointerDown)
    return () => el.removeEventListener('pointerdown', onBackdropPointerDown)
  }, [open])

  return (
    <div className="rgf-theme-launcher">
      <dialog
        ref={dialogRef}
        id={panelId}
        className="rgf-theme-popover"
        aria-labelledby={titleId}
        onClose={() => setOpen(false)}
      >
        <div className="rgf-theme-popover-head">
          <h2 id={titleId} className="rgf-theme-popover-title">
            Display &amp; Accessibility
          </h2>
          <button
            type="button"
            className="rgf-theme-popover-close"
            onClick={() => setOpen(false)}
            aria-label="Close settings"
          >
            ×
          </button>
        </div>

        <div className="rgf-theme-popover-body">
          <fieldset className="rgf-theme-fieldset">
            <legend className="rgf-theme-section-label">Color theme</legend>
            <div className="rgf-theme-mode-grid">
              {COLOR_MODES.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  className="rgf-theme-mode-btn"
                  data-active={colorMode === opt.value ? 'true' : 'false'}
                  onClick={() => setColorMode(opt.value)}
                  aria-pressed={colorMode === opt.value}
                  title={opt.hint}
                >
                  <span className="rgf-theme-mode-emoji" aria-hidden>
                    {opt.emoji}
                  </span>
                  <span className="rgf-theme-mode-label">{opt.label}</span>
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset className="rgf-theme-fieldset">
            <legend className="rgf-theme-section-label">Accent colors</legend>
            <div className="rgf-theme-palette-grid">
              {PALETTES.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  className="rgf-theme-palette-btn"
                  data-active={palette === opt.value ? 'true' : 'false'}
                  onClick={() => setPalette(opt.value)}
                  aria-pressed={palette === opt.value}
                >
                  <span className="rgf-theme-palette-emoji" aria-hidden>
                    {opt.emoji}
                  </span>
                  <span className="rgf-theme-palette-title">{opt.title}</span>
                  <span className="rgf-theme-palette-desc">{opt.desc}</span>
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset className="rgf-theme-fieldset">
            <legend className="rgf-theme-section-label">Text size</legend>
            <div className="rgf-theme-textsize-row">
              {FONT_SCALES.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  className="rgf-theme-textsize-btn"
                  data-active={fontScale === opt.value ? 'true' : 'false'}
                  onClick={() => setFontScale(opt.value)}
                  aria-pressed={fontScale === opt.value}
                  title={opt.hint}
                >
                  <span className="rgf-theme-textsize-label">{opt.label}</span>
                  <span className="rgf-theme-textsize-hint">{opt.hint}</span>
                </button>
              ))}
            </div>
          </fieldset>

          <div className="rgf-theme-check">
            <input
              id={highContrastInputId}
              className="rgf-theme-check-input"
              type="checkbox"
              checked={highContrast}
              onChange={(e) => setHighContrast(e.target.checked)}
            />
            <label htmlFor={highContrastInputId} className="rgf-theme-check-label">
              <span className="rgf-theme-check-title">High contrast</span>
              <span className="rgf-theme-check-desc">
                Stronger borders and text for readability
              </span>
            </label>
          </div>

          <div className="rgf-theme-field">
            <label htmlFor={`${id}-motion`} className="rgf-theme-label">
              Animations &amp; transitions
            </label>
            <select
              id={`${id}-motion`}
              className="rgf-theme-select rgf-theme-select-full"
              value={reduceMotion}
              onChange={(e) => {
                const v = e.target.value
                if (v === 'system' || v === 'always' || v === 'never') {
                  setReduceMotion(v)
                }
              }}
            >
              <option value="system">
                Match system{prefersReducedMotion ? ' (reduced now)' : ''}
              </option>
              <option value="always">Reduce always</option>
              <option value="never">Allow motion</option>
            </select>
          </div>
        </div>
      </dialog>

      <button
        type="button"
        className="rgf-theme-fab"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        aria-haspopup="dialog"
      >
        <span aria-hidden className="rgf-theme-fab-icon">
          ⚙️
        </span>
        <span className="rgf-sr-only">
          {open ? 'Close display and accessibility settings' : 'Open display and accessibility settings'}
        </span>
      </button>
    </div>
  )
}
