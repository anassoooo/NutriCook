import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import MedicalDisclaimer from '../components/MedicalDisclaimer'

describe('MedicalDisclaimer', () => {
  it('BR_SAF_08_showsWhenTheApiMarksTheResponse', () => {
    render(<MedicalDisclaimer disclaimer="NOT_MEDICAL_ADVICE" />)
    expect(screen.getByRole('note', { name: 'Medical disclaimer' })).toHaveTextContent('Not medical advice.')
  })

  it('BR_SAF_08_hiddenWithoutAHealthCondition', () => {
    const { container } = render(<MedicalDisclaimer disclaimer={null} />)
    expect(container).toBeEmptyDOMElement()
  })
})
