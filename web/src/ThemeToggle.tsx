import { useEffect, useState } from 'react'
export default function ThemeToggle() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('interviewpair.theme')
      if (saved === 'light' || saved === 'dark') return saved
    } catch {
      /* A blocked store still permits switching for this visit. */
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light'
  })
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    try {
      localStorage.setItem('interviewpair.theme', theme)
    } catch {
      /* In-memory preference still works. */
    }
  }, [theme])
  return (
    <button
      className="theme-toggle"
      onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
      aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
      title={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
    >
      <svg
        viewBox="0 0 24 24"
        width="18"
        height="18"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        aria-hidden="true"
      >
        {theme === 'light' ? (
          <path d="M20 15.2A8 8 0 0 1 8.8 4 8 8 0 1 0 20 15.2Z" />
        ) : (
          <>
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5" />
          </>
        )}
      </svg>
      <span>{theme === 'light' ? 'Dark' : 'Light'}</span>
    </button>
  )
}
