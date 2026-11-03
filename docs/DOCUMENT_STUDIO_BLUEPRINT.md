# FormReady AI — Document Studio Blueprint

> Working product codename. This document describes a proposed India-focused Android app for preparing and validating files used in online applications. It is a blueprint, not a claim that every portal is already supported.

## 1. Product promise

A user chooses an application/portal (or provides its official upload instructions), selects or captures a genuine document, and gets a preview plus one-click export in the exact permitted format, dimensions, file-size range, and page limit.

The product should be **document preparation + rule validation**, not a fake-document generator and not a bot that logs into government portals.

Primary experience:
1. Choose the portal/application.
2. See the official, current upload rules and source link.
3. Capture/import the genuine photo, signature, or document.
4. Apply only transformations permitted by that portal's instructions.
5. Run validation and preview.
6. Export the prepared file(s), named clearly, or export a ZIP bundle.

## 2. Important product constraints

- Portal rules are not universal. Keep a separate versioned profile for each portal, service, state, exam, and (where relevant) recruitment notification.
- A portal's current notification/instructions override a generic preset. Never silently guess unknown rules.
- Some portals expressly reject software-edited photographs. For example, FRRO instructions state that photographs modified by Photoshop or another software/mobile app may be rejected. When a profile contains this rule, disable image editing and tell the user to submit an untouched compliant photo.
- AI may assess blur, glare, framing, orientation, page edges, and legibility. It must not invent or replace a face, signature, seal, QR code, certificate text, ID number, or other identity information.
- Never alter names, dates, marks, addresses, ID numbers, signatures, or official seals to make a document appear different.
- Never promise that a generated print layout is accepted as an official identity document.
- Do not automate government-portal login, CAPTCHA, OTP, fee payment, or final submission in the first releases. The app prepares files; the user reviews and uploads them.

## 3. Main feature modules

### A. Smart Portal Requirement Engine
- Search by portal, exam, service, department, state, or application name.
- Allow the user to paste text, import a public notification PDF, or provide a screenshot of the upload-instructions page.
- Extract: file type, minimum/maximum KB or MB, pixel width/height, aspect ratio, physical dimensions, page count, colour mode, naming rules, and restrictions on editing.
- Display the exact source URL, source title, checked date, profile version, and confidence status.
- If a requirement is missing, contradictory, or unreadable, show "Needs confirmation" rather than inventing a value.
- Support user-supplied one-off requirements without falsely labelling them as officially verified.

### B. Photo Studio
- Capture from camera or import from gallery.
- Guided framing for passport-style photos; optional face-position, blur, glare, and lighting checks.
- Crop, resize, and JPEG compression only when allowed by the selected portal profile.
- Generate the exact target pixel dimensions and file-size range; verify the saved output, not just the preview.
- Keep the untouched original available separately.
- No AI face replacement, beautification, age changes, synthetic headshots, or false background/identity changes.
- If the portal bans software modification, switch to "Original photo required" mode and do not process the image.

### C. Signature & Thumb Impression Studio
- Capture/import a real signature or thumb impression.
- Crop excess blank space, set allowed dimensions, convert to the required file type, and compress when permitted.
- Preview legibility at the portal's actual display size.
- Never generate, trace, imitate, or alter a person's signature or thumb impression.

### D. Document Scanner & PDF Workshop
- Multi-page camera scan with edge detection, perspective correction, rotation, and page reorder.
- Colour, grayscale, or black-and-white output only where allowed.
- Combine, split, rotate, reorder, and compress PDFs.
- Convert images to PDF and extract PDF pages to images.
- Check file size, page count, readability, and whether a PDF is password-protected when the portal forbids it.
- Preserve every original file. Save transformations as new copies.
- Warn that modifying a digitally signed PDF may invalidate its digital signature; keep issuer-signed originals untouched for digital submission.

### E. Aadhaar & Identity Document Helper
- Import the original e-Aadhaar PDF and preserve it unchanged.
- Provide a clear route to UIDAI's official services for masked Aadhaar and official Aadhaar PVC ordering.
- Optional print-only wallet-size layout may be created from a user-provided copy, with a visible "PRINT COPY — NOT AN OFFICIAL UIDAI PVC CARD" mark. Do not imitate or fabricate UIDAI security features, holograms, microtext, or tamper-proof QR codes.
- Explain that UIDAI lists e-Aadhaar, Aadhaar letter, and UIDAI-issued PVC card as equally valid forms; UIDAI's official PVC ordering service is the route for an actual PVC card (official page lists ₹75).
- Do not replace an original digitally signed e-Aadhaar PDF with a cropped/flattened derivative for portal uploads.
- Offer a privacy prompt before showing or processing Aadhaar number, QR code, address, or date of birth.

### F. Other Document Helpers
- Organise common categories: PAN, passport, driving licence, voter ID, marksheets, degree certificates, caste, income, residence, birth certificate, bank statement/passbook, and supporting PDFs.
- Category recognition can suggest the document type, but users must confirm before any classification or data extraction.
- Keep original issued documents separate from scanned copies and print layouts.

### G. Final Upload Validator
For each output, show:
- Pass / warning / fail for file type, size, dimensions, aspect ratio, page count, and image legibility.
- A preview at actual upload dimensions.
- A clear explanation of every failed rule and a one-tap correction where allowed.
- A download/share action for individual files and an application-ready ZIP bundle.
- Output filenames such as `Photo_150x200_45KB.jpg`, without embedding personal Aadhaar/PAN numbers in filenames.

### H. Document Locker (optional, later release)
- Local-first storage, encrypted at rest, and explicit opt-in before any cloud sync.
- No automatic upload of personal documents to Gemini or any other AI service.
- Clear "delete this file" and "delete all local files" actions.
- No document contents, OCR text, Aadhaar/PAN values, or signatures in analytics or diagnostic logs.
- Do not back up sensitive document files to consumer cloud backup by default.
- Keep API keys out of source control; use a backend or platform secret mechanism for AI features.

## 4. Rule-profile data model

Each rule profile should be structured data, not instructions buried in UI code. Suggested fields:

```json
{
  "profileId": "example.portal.photo.signature",
  "portalName": "Example Portal",
  "serviceOrNotification": "Exact service / notification name",
  "documentType": "photo",
  "format": ["image/jpeg"],
  "minBytes": 20000,
  "maxBytes": 300000,
  "minWidthPx": 350,
  "maxWidthPx": 1000,
  "minHeightPx": 350,
  "maxHeightPx": 1000,
  "aspectRatio": null,
  "editingAllowed": "resize_compress_only",
  "sourceUrl": "https://official-source.example/instructions",
  "sourceTitle": "Official upload instructions",
  "checkedAt": "2026-11-03",
  "profileVersion": 1,
  "verificationStatus": "official_source_checked",
  "notes": "Notification-specific requirements take precedence."
}
```

The example values are illustrative only; production profiles must use the exact requirements found in official instructions.

## 5. Rule-source examples researched (3 November 2026)

These examples demonstrate why profiles must be separate; they are not universal presets:

- UPSC candidate FAQ: photograph/signature in JPG/JPEG; each 20 KB to 300 KB; resolution 350×350 px minimum and 1000×1000 px maximum. Photo ID must be PDF. Source: https://upsconline.gov.in/OTRP/candidate/faq.php
- SDSC SHAR recruitment help: photograph about 110×140 px and up to 40 KB; signature up to 200×50 px and 20 KB; documents as PDF, generally up to 500 KB (some certificate exception). Page shows last updated 5 October 2026. Source: https://apps.shar.gov.in/Recruitment/help.jsp
- Indian Citizenship Online upload guide: supporting documents as PDF up to 2 MB; photo and signature 100×120 px, JPG, up to 20 KB. Source: https://www.indiancitizenshiponline.gov.in/Documents/Annexure.pdf
- FRRO: instructions warn that photos edited/modified by Photoshop or another software/mobile app may be rejected; scans must be PDF and the stated photo rules apply. Source: https://indianfrro.gov.in/efrro/
- Sarathi/Parivahan: the central FAQ says JPEG for photo/signature and notes that document upload options depend on state customisation. Source: https://staging.parivahan.nic.in/parivahan/en/content/faq

These rules should be rechecked from the exact live portal and current notification whenever an application is prepared.

## 6. Aadhaar: correct product behaviour

UIDAI says e-Aadhaar is a password-protected electronic copy digitally signed by UIDAI and equally valid as a physical Aadhaar copy. UIDAI also describes Masked Aadhaar as showing only the last four digits. UIDAI says Aadhaar letter, e-Aadhaar, and the official PVC card are equally valid forms. The official PVC card includes security features and must be ordered through UIDAI; a home-printed layout is not that card.

Official sources:
- e-Aadhaar and Masked Aadhaar FAQ: https://uidai.gov.in/kn/aadhaar-online-services
- Aadhaar online services / PVC information: https://uidai.gov.in/hi/aadhaar-online-services
- myAadhaar: https://myaadhaar.uidai.gov.in/

## 7. Recommended technical direction for this repository

Current repository: Android native app using Kotlin, Jetpack Compose, minSdk 24, targetSdk 36. It currently contains an AI screen-automation agent, a Gemini client, floating overlay, and an Accessibility Service that reads screen UI nodes and performs user-authorised UI actions.

Recommended approach:
- Work on a separate branch and preserve `main` (the `document-studio-blueprint` branch is the planning branch).
- Build the Document Studio as a separate feature/application path; do not reuse the screen-reading Accessibility Service or floating overlay for document preparation.
- Use Android Photo Picker / Storage Access Framework for user-selected files and the system camera flow for capture.
- Keep image processing, PDF generation, compression, and validation on-device wherever possible.
- Use Gemini only to interpret public portal instructions or user-selected instruction screenshots/PDFs, with a human-readable extracted result and user confirmation. Do not send identity documents to the model by default.
- Suggested package boundaries:
  - `document/model`: file metadata, document types, processing results
  - `document/image`: photo and signature operations
  - `document/pdf`: scan, merge, split, compress, render
  - `portal/model`: versioned requirement profiles
  - `portal/parser`: instruction extraction and validation
  - `privacy`: consent, local storage, deletion, redaction warnings
  - `export`: naming, ZIP bundles, Android share sheet
- Existing AI automation code should remain separate until a clear migration decision is made.

## 8. Delivery roadmap

### Release 1 — reliable offline core
1. Hindi-first Compose interface.
2. Import/capture photo and signature.
3. Manual custom requirements: width, height, format, min/max KB.
4. Output generation and actual output validation.
5. Multi-page PDF import, reorder, rotate, merge, split, and compression.
6. Original-file preservation and local-only processing.

### Release 2 — trusted portal presets
1. Versioned official profiles for a limited set of high-demand services.
2. Source link, checked date, and exact notification selection.
3. Rule conflict/expiry warnings.
4. Batch output for photo + signature + supporting documents.

### Release 3 — AI-assisted instruction understanding
1. Parse public instructions from text, PDF, or screenshot.
2. Show extracted rules in a confirmation screen.
3. Compare parsed instructions with known official profile.
4. Add new profile only after review; never silently publish a guessed rule.

### Release 4 — document intelligence
1. Optional on-device OCR and document category suggestions.
2. Aadhaar print-only layout and masked-copy guidance.
3. Local encrypted document locker and deletion controls.
4. Accessibility, Hindi/English support, and low-network operation.

## 9. Acceptance criteria

- No file leaves the device unless the user explicitly chooses a cloud feature and sees what will be sent.
- Every portal preset displays its official source and last-checked date.
- Unknown or conflicting specifications are surfaced; no guessed "pass".
- Every exported file is re-opened and validated after creation.
- The original file is never overwritten.
- A signed original PDF remains available and is never flattened silently.
- Photo/document edits are blocked when the selected portal forbids them.
- No Accessibility Service, OTP handling, CAPTCHA solving, or automatic government-portal submission is required for the core app.

## 10. Privacy and compliance work before public launch

The app will process identity and other personal documents. Before public release, complete a privacy/security review against the Digital Personal Data Protection Act, 2023 and the notified DPDP Rules, 2025, including the applicable phased commencement timeline. Provide a clear notice, purpose limitation, consent controls, retention/deletion controls, security safeguards, and a user support/grievance process as advised by qualified Indian counsel.

Official source: https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025
