import os

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from apps.ai.services.models import AttachmentAnalysisResult, ReplyPlan, SpeechAnalysis, ValidationResult
from apps.ai.services.prompts import (
    ANALYSIS_SYSTEM,
    ANALYSIS_USER,
    ATTACHMENT_ANALYSIS_SYSTEM,
    ATTACHMENT_ANALYSIS_USER,
    BODY_SYSTEM,
    BODY_USER,
    INTEGRATE_SYSTEM,
    INTEGRATE_USER,
    PLAN_SYSTEM,
    PLAN_USER,
    PROMPT_PREVIEW_SYSTEM,
    PROMPT_PREVIEW_USER,
    REPLY_PLAN_SYSTEM,
    REPLY_PLAN_USER,
    REPLY_SYSTEM,
    REPLY_USER,
    SUBJECT_SYSTEM,
    SUBJECT_USER,
    SUGGEST_SYSTEM,
    SUGGEST_USER,
    VALIDATOR_SYSTEM,
    VALIDATOR_USER,
)

_base_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.4")),
)

_subject_prompt = ChatPromptTemplate.from_messages(
    [("system", SUBJECT_SYSTEM), ("user", SUBJECT_USER)],
    template_format="jinja2",
)
_subject_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_SUBJECT_TEMPERATURE", "0.2")),
)
subject_chain = _subject_prompt | _subject_model | StrOutputParser()

_body_prompt = ChatPromptTemplate.from_messages(
    [("system", BODY_SYSTEM), ("user", BODY_USER)],
    template_format="jinja2",
)
body_chain = _body_prompt | _base_model | StrOutputParser()

_plan_prompt = ChatPromptTemplate.from_messages(
    [("system", PLAN_SYSTEM), ("user", PLAN_USER)],
    template_format="jinja2",
)
plan_chain = _plan_prompt | _base_model | StrOutputParser()

_reply_plan_prompt = ChatPromptTemplate.from_messages(
    [("system", REPLY_PLAN_SYSTEM), ("user", REPLY_PLAN_USER)],
    template_format="jinja2",
)
reply_plan_chain = _reply_plan_prompt | _base_model.with_structured_output(ReplyPlan)

_reply_body_prompt = ChatPromptTemplate.from_messages(
    [("system", REPLY_SYSTEM), ("user", REPLY_USER)],
    template_format="jinja2",
)

_reply_body_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.4")),
)

reply_body_chain = _reply_body_prompt | _reply_body_model | StrOutputParser()


_validator_prompt = ChatPromptTemplate.from_messages(
    [("system", VALIDATOR_SYSTEM), ("user", VALIDATOR_USER)],
    template_format="jinja2",
)

_validator_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=0.0,
)

validator_chain = _validator_prompt | _validator_model.with_structured_output(ValidationResult)

_prompt_preview_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", PROMPT_PREVIEW_SYSTEM),
        ("user", PROMPT_PREVIEW_USER),
    ],
    template_format="jinja2",
)

_prompt_preview_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=0.2,
)
prompt_preview_chain = _prompt_preview_prompt | _prompt_preview_model | StrOutputParser()

_analysis_prompt = ChatPromptTemplate.from_messages(
    [("system", ANALYSIS_SYSTEM), ("user", ANALYSIS_USER)],
    template_format="jinja2",
)

_analysis_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.4")),
)

analysis_chain = _analysis_prompt | _analysis_model.with_structured_output(SpeechAnalysis)

_integrate_prompt = ChatPromptTemplate.from_messages(
    [("system", INTEGRATE_SYSTEM), ("user", INTEGRATE_USER)],
    template_format="jinja2",
)

_integrate_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.4")),
)

integrate_chain = _integrate_prompt | _integrate_model.with_structured_output(SpeechAnalysis)


_attachment_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", ATTACHMENT_ANALYSIS_SYSTEM),
        ("user", ATTACHMENT_ANALYSIS_USER),
    ],
    template_format="jinja2",
)

attachment_analysis_chain = _attachment_prompt | _base_model.with_structured_output(AttachmentAnalysisResult)

_suggest_prompt = ChatPromptTemplate.from_messages(
    [("system", SUGGEST_SYSTEM), ("user", SUGGEST_USER)],
    template_format="jinja2",
)

_suggest_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.6")),
)

suggest_chain = _suggest_prompt | _suggest_model | StrOutputParser()
