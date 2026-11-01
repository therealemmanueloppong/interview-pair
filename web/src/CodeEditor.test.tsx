import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it } from 'vitest'
import CodeEditor from './CodeEditor'

afterEach(cleanup)

function Harness({ readOnly = false, initial = '' }: { readOnly?: boolean; initial?: string }) {
  const [value, setValue] = useState(initial)
  return (
    <>
      <CodeEditor value={value} onChange={setValue} readOnly={readOnly} label="Your answer" lineNumbers />
      <button>after</button>
    </>
  )
}

describe('CodeEditor', () => {
  it('indents with four spaces when Tab is pressed', async () => {
    const u = userEvent.setup()
    render(<Harness />)
    await u.click(screen.getByLabelText('Your answer'))
    await u.keyboard('{Tab}x')
    expect(screen.getByLabelText('Your answer')).toHaveValue('    x')
  })

  it('lets Tab leave the editor after Escape, so keyboard users are not trapped', async () => {
    const u = userEvent.setup()
    render(<Harness />)
    await u.click(screen.getByLabelText('Your answer'))
    await u.keyboard('{Escape}{Tab}')
    expect(screen.getByLabelText('Your answer')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'after' })).toHaveFocus()
  })

  it('does not change a read only editor', async () => {
    const u = userEvent.setup()
    render(<Harness readOnly initial="print(1)" />)
    const area = screen.getByLabelText('Your answer')
    expect(area).toHaveAttribute('readonly')
    await u.click(area)
    await u.keyboard('abc')
    expect(area).toHaveValue('print(1)')
  })

  it('numbers every line', () => {
    const { container } = render(<Harness initial={'a\nb\nc'} />)
    expect(container.querySelector('.code-gutter')?.textContent).toBe('123')
  })
})
