from drf_spectacular.utils import OpenApiResponse, extend_schema

# 필요시 여기에 항목 추가/수정
COMMON_ERROR_RESPONSES = {
    400: OpenApiResponse(description="Bad request"),
    401: OpenApiResponse(description="Unauthorized"),
    404: OpenApiResponse(description="Not found"),
    429: OpenApiResponse(description="Rate limit exceeded"),
    500: OpenApiResponse(description="Server error"),
}


def extend_schema_with_common_errors(**kwargs):
    """
    extend_schema 래퍼.
    - responses 인자가 있으면 공통 에러를 병합하여 적용
    - responses가 없더라도 공통 에러만 추가 가능
    - 특정 코드만 빼고 싶으면 exclude_codes=[401, ...] 전달
    """
    exclude_codes = set(kwargs.pop("exclude_codes", []) or [])
    base_responses = kwargs.pop("responses", {}) or {}
    # 공통 에러에서 제외할 응답 코드 제거
    common = {
        code: resp for code, resp in COMMON_ERROR_RESPONSES.items() if code not in exclude_codes
    }
    merged = {**base_responses, **common}
    return extend_schema(responses=merged, **kwargs)
