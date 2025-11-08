import os

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from apps.ai.services.models import ValidationResult
from apps.ai.services.prompts import (
    BODY_SYSTEM,
    BODY_USER,
    PLAN_SYSTEM,
    PLAN_USER,
    REPLY_SYSTEM,
    REPLY_USER,
    SUBJECT_SYSTEM,
    SUBJECT_USER,
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
