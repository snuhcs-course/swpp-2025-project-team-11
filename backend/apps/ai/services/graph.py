from typing import Any

from langgraph.graph import END, StateGraph

from apps.ai.services.chains import subject_chain
from apps.ai.services.pii_masker import PiiMasker, make_req_id
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context

State = dict[str, Any]


def build_context_node(state: State) -> State:
    user = state["user"]
    to_emails = state["to_emails"]
    ctx = collect_prompt_context(user, to_emails)
    raw_inputs = build_prompt_inputs(ctx)
    state["raw_inputs"] = raw_inputs
    return state


def masking_node(state: State) -> State:
    raw_inputs = state["raw_inputs"]
    raw_inputs["subject"] = state.get("subject") or ""
    raw_inputs["body"] = state.get("body") or ""

    req_id = make_req_id()
    masker = PiiMasker(req_id)
    masked, mapping = masker.mask_inputs(raw_inputs)

    state["req_id"] = req_id
    state["masker"] = masker
    state["masked_inputs"] = masked
    state["mask_mapping"] = mapping
    return state


def subject_node(state: State) -> State:
    masked_inputs = state["masked_inputs"]
    locked_title = (subject_chain.invoke(masked_inputs) or "").strip()
    state["locked_title"] = locked_title
    return state


def body_prep_node(state: State) -> State:
    masked_inputs = state["masked_inputs"]
    raw_inputs = state["raw_inputs"]
    locked_title = state["locked_title"]
    plan_text = state.get("plan_text")

    locked_inputs = {
        "locked_subject": locked_title,
        "body": masked_inputs.get("body", ""),
        "language": raw_inputs.get("language"),
        "recipients": raw_inputs.get("recipients"),
        "group_name": raw_inputs.get("group_name"),
        "group_description": raw_inputs.get("group_description"),
        "prompt_text": raw_inputs.get("prompt_text"),
        "sender_role": raw_inputs.get("sender_role"),
        "recipient_role": raw_inputs.get("recipient_role"),
        "analysis": raw_inputs.get("analysis", None),
        "fewshots": raw_inputs.get("fewshots"),
        "plan_text": plan_text,
    }
    state["body_inputs"] = locked_inputs
    return state


# 그래프 구성
graph = StateGraph(State)
graph.add_node("build_context", build_context_node)
graph.add_node("masking", masking_node)
graph.add_node("subject", subject_node)
graph.add_node("body_prep", body_prep_node)

graph.set_entry_point("build_context")
graph.add_edge("build_context", "masking")
graph.add_edge("masking", "subject")
graph.add_edge("subject", "body_prep")
graph.add_edge("body_prep", END)

mail_graph = graph.compile()
