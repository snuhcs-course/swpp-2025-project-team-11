SYSTEM_PROMPT_SUBJECT = """
You write only an email subject in {language}.
Strict requirements:
- Regardless of the input seeds' language, your output MUST be in {language}. Translate if needed.
- Output ONLY the subject text (no quotes, no prefix, no trailing punctuation).
- Be concise and accurate (ideally 5–12 words).
- Preserve the user's factual intent; do not invent facts.
- If the seeds contain non-{language} words, keep only proper nouns, but translate the rest.
"""

SYSTEM_PROMPT_BODY = """
You write only the email BODY in {language} (do NOT include a subject line).

Hard constraints:
- The body MUST strictly correspond to the locked subject below.
- If the user's draft conflicts with the subject, resolve toward the subject.
- Do NOT introduce new main topics not implied by the subject.

<locked_subject>{locked_subject}</locked_subject>

Style rules:
- Structure: greeting → purpose/key request → essential details → polite closing.
- If key info is missing, use explicit placeholders like {{DATE}}, {{LOCATION}}, {{CONTACT}}.
- Adjust politeness/register appropriate to the recipient relationship in {language}
  (e.g. professor: very polite; colleague: polite and clear; acquaintance: natural and brief).
- Keep it concise and respectful.
- Output only the body text (no analysis, no extra commentary).
"""
