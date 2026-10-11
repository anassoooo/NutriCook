/**
 * Shown whenever the API marks a response with disclaimer = "NOT_MEDICAL_ADVICE", i.e. the user
 * has a health condition (BR-SAF-08).
 */
export default function MedicalDisclaimer({ disclaimer }: { disclaimer: string | null | undefined }) {
  if (disclaimer !== 'NOT_MEDICAL_ADVICE') return null
  return (
    <div role="note" aria-label="Medical disclaimer" className="banner-warning">
      <p>
        <span className="font-semibold">Not medical advice.</span> Plans respect the limits for your health
        conditions, but they don't replace your doctor or dietitian. Check with them before changing your
        diet.
      </p>
    </div>
  )
}
