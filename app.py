"""Reticulate web demo — FastAPI + HTMX.

Provides an interactive web interface for analyzing session types:
parse, build state space, check lattice, and render Hasse diagrams.

Also serves the bica.zuacaldeira.com research website with theory pages,
benchmark gallery, publications, and author information.
"""

from __future__ import annotations

import hashlib
import hmac
import logging
import os
import re
import secrets
import sys
import urllib.parse
from contextlib import asynccontextmanager
from dataclasses import dataclass
from pathlib import Path
from typing import Any

# Ensure reticulate is importable from the project root.
_project_root = Path(__file__).resolve().parent.parent
_reticulate_root = _project_root / "reticulate"
if str(_reticulate_root) not in sys.path:
    sys.path.insert(0, str(_reticulate_root))

from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from starlette.middleware.base import BaseHTTPMiddleware

from reticulate import (
    LatticeResult,
    ParseError,
    TestGenConfig,
    build_statespace,
    check_lattice,
    check_termination,
    check_wf_parallel,
    dot_source,
    generate_test_source,
    parse,
    pretty,
)

# Import benchmarks
sys.path.insert(0, str(_reticulate_root / "tests"))
from benchmarks.protocols import BENCHMARKS

logger = logging.getLogger("reticulate.web")

# ---------------------------------------------------------------------------
# Benchmark cache
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class BenchmarkCacheEntry:
    """Pre-rendered benchmark result."""

    name: str
    description: str
    type_string: str
    pretty_str: str
    num_states: int
    num_transitions: int
    num_sccs: int
    is_lattice: bool
    uses_parallel: bool
    svg_html: str
    tool_url: str


_benchmark_cache: list[BenchmarkCacheEntry] = []

# Maximum states before we refuse to render (safety guard).
_MAX_STATES = 10_000

# Strip XML preamble from SVG output so it can be inlined in HTML.
_XML_PREAMBLE_RE = re.compile(
    r"<\?xml[^?]*\?>\s*(?:<!DOCTYPE[^>]*>\s*)?(?:<!--[^-]*-->\s*)*",
    re.DOTALL,
)


def _render_svg(dot: str) -> str:
    """Render a DOT string to inline SVG via graphviz."""
    import graphviz

    svg_bytes = graphviz.Source(dot).pipe(format="svg")
    svg_str = svg_bytes.decode("utf-8")
    return _XML_PREAMBLE_RE.sub("", svg_str)


def _prerender_benchmarks() -> list[BenchmarkCacheEntry]:
    """Run the full pipeline on all benchmarks and cache results."""
    entries: list[BenchmarkCacheEntry] = []
    for b in BENCHMARKS:
        try:
            ast = parse(b.type_string)
            pretty_str = pretty(ast)
            ss = build_statespace(ast)
            result: LatticeResult = check_lattice(ss)
            dot_str = dot_source(ss, result)
            svg_html = _render_svg(dot_str)
        except Exception as exc:
            logger.warning("Failed to pre-render benchmark %s: %s", b.name, exc)
            continue

        tool_url = "/tool?type=" + urllib.parse.quote(b.type_string, safe="")
        entries.append(
            BenchmarkCacheEntry(
                name=b.name,
                description=b.description,
                type_string=b.type_string,
                pretty_str=pretty_str,
                num_states=len(ss.states),
                num_transitions=len(ss.transitions),
                num_sccs=result.num_scc,
                is_lattice=result.is_lattice,
                uses_parallel=b.uses_parallel,
                svg_html=svg_html,
                tool_url=tool_url,
            )
        )
    return entries


# ---------------------------------------------------------------------------
# Lifespan
# ---------------------------------------------------------------------------


@asynccontextmanager
async def lifespan(app: FastAPI):  # noqa: ARG001
    """Pre-render benchmarks at startup."""
    global _benchmark_cache
    logger.info("Pre-rendering %d benchmarks...", len(BENCHMARKS))
    _benchmark_cache = _prerender_benchmarks()
    logger.info("Cached %d benchmark results.", len(_benchmark_cache))
    yield


# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

app = FastAPI(title="Reticulate — Session Type Analyzer", lifespan=lifespan)

_web_dir = Path(__file__).resolve().parent
app.mount("/static", StaticFiles(directory=str(_web_dir / "static")), name="static")
templates = Jinja2Templates(directory=str(_web_dir / "templates"))

# ---------------------------------------------------------------------------
# Password gate
# ---------------------------------------------------------------------------

SITE_PASSWORD: str = os.environ.get("SITE_PASSWORD", "reticulate")
SESSION_SECRET: str = os.environ.get("SESSION_SECRET", secrets.token_hex(32))
_SESSION_TOKEN: str = hmac.new(
    SESSION_SECRET.encode(), SITE_PASSWORD.encode(), hashlib.sha256
).hexdigest()
_COOKIE_MAX_AGE: int = 30 * 24 * 60 * 60  # 30 days


class _AuthMiddleware(BaseHTTPMiddleware):
    """Redirect unauthenticated visitors to /login."""

    async def dispatch(self, request: Request, call_next):  # type: ignore[override]
        path = request.url.path
        # Allow login page and static assets through without auth
        if path == "/login" or path.startswith("/static"):
            return await call_next(request)
        token = request.cookies.get("session_token")
        if token != _SESSION_TOKEN:
            return RedirectResponse(url="/login", status_code=302)
        return await call_next(request)


app.add_middleware(_AuthMiddleware)


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------


@app.get("/login", response_class=HTMLResponse)
async def login_page(request: Request) -> HTMLResponse:
    """Login page."""
    error = request.query_params.get("error")
    return templates.TemplateResponse(
        "login.html",
        {"request": request, "error": error, "hide_nav": True},
    )


@app.post("/login")
async def login_submit(password: str = Form(...)):
    """Validate password and set session cookie."""
    if password == SITE_PASSWORD:
        response = RedirectResponse(url="/", status_code=302)
        response.set_cookie(
            "session_token",
            _SESSION_TOKEN,
            httponly=True,
            samesite="lax",
            max_age=_COOKIE_MAX_AGE,
        )
        return response
    return RedirectResponse(url="/login?error=1", status_code=302)


@app.get("/logout")
async def logout():
    """Clear session cookie and redirect to login."""
    response = RedirectResponse(url="/login", status_code=302)
    response.delete_cookie("session_token")
    return response


@app.get("/", response_class=HTMLResponse)
async def index(request: Request) -> HTMLResponse:
    """Landing page with paper-style header, abstract, and stats."""
    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "active_page": "home",
            "num_benchmarks": len(_benchmark_cache),
            "total_states": sum(b.num_states for b in _benchmark_cache),
            "all_lattice": all(b.is_lattice for b in _benchmark_cache),
        },
    )


@app.get("/tool", response_class=HTMLResponse)
async def tool(request: Request, type: str | None = None) -> HTMLResponse:
    """Interactive analyzer page."""
    return templates.TemplateResponse(
        "tool.html",
        {
            "request": request,
            "active_page": "tool",
            "benchmarks": BENCHMARKS,
            "prefill": type or "",
        },
    )


@app.post("/analyze", response_class=HTMLResponse)
async def analyze(
    request: Request,
    type_string: str = Form(...),
    class_name: str = Form(""),
) -> HTMLResponse:
    """Run the full reticulate pipeline and return an HTMX fragment."""
    type_string = type_string.strip()
    class_name = class_name.strip()
    if not type_string:
        return templates.TemplateResponse(
            "fragments/error.html",
            {"request": request, "error": "Please enter a session type."},
        )

    # 1. Parse
    try:
        ast = parse(type_string)
    except ParseError as exc:
        return templates.TemplateResponse(
            "fragments/error.html",
            {"request": request, "error": f"Parse error: {exc}"},
        )

    pretty_str = pretty(ast)

    # 2. Build state space
    try:
        ss = build_statespace(ast)
    except (ValueError, RecursionError) as exc:
        return templates.TemplateResponse(
            "fragments/error.html",
            {"request": request, "error": f"State-space error: {exc}"},
        )

    if len(ss.states) > _MAX_STATES:
        return templates.TemplateResponse(
            "fragments/error.html",
            {
                "request": request,
                "error": (
                    f"State space too large ({len(ss.states)} states, "
                    f"limit is {_MAX_STATES}). Simplify the type."
                ),
            },
        )

    # 3. Lattice check
    result: LatticeResult = check_lattice(ss)

    # 4. Termination & WF-Par
    term_result = check_termination(ast)
    wf_result = check_wf_parallel(ast)

    # 5. Hasse diagram (SVG)
    svg_html = ""
    dot_str = ""
    try:
        dot_str = dot_source(ss, result)
        svg_html = _render_svg(dot_str)
    except Exception:
        svg_html = "<p>Could not render diagram (graphviz not available).</p>"

    # 6. Test generation (if class name provided)
    test_source = ""
    if class_name:
        try:
            cfg = TestGenConfig(class_name, max_revisits=2, max_paths=100)
            test_source = generate_test_source(ss, cfg, pretty_str)
        except Exception:
            test_source = ""

    return templates.TemplateResponse(
        "fragments/result.html",
        {
            "request": request,
            "pretty": pretty_str,
            "num_states": len(ss.states),
            "num_transitions": len(ss.transitions),
            "num_sccs": result.num_scc,
            "result": result,
            "term_result": term_result,
            "wf_result": wf_result,
            "svg_html": svg_html,
            "dot_source": dot_str,
            "test_source": test_source,
            "class_name": class_name,
        },
    )


@app.get("/documentation", response_class=HTMLResponse)
async def documentation(request: Request) -> HTMLResponse:
    """Merged documentation page: theory, tutorials, FAQ."""
    return templates.TemplateResponse(
        "documentation.html",
        {"request": request, "active_page": "documentation"},
    )


@app.get("/tutorials")
async def tutorials() -> RedirectResponse:
    """Redirect to documentation page (tutorials section)."""
    return RedirectResponse(url="/documentation#tutorials", status_code=301)


@app.get("/theory")
async def theory() -> RedirectResponse:
    """Redirect to documentation page (theory section)."""
    return RedirectResponse(url="/documentation#theory", status_code=301)


@app.get("/benchmarks", response_class=HTMLResponse)
async def benchmarks(request: Request) -> HTMLResponse:
    """Benchmark gallery with pre-rendered SVGs."""
    return templates.TemplateResponse(
        "benchmarks.html",
        {
            "request": request,
            "active_page": "benchmarks",
            "benchmarks": _benchmark_cache,
        },
    )


@app.get("/publications", response_class=HTMLResponse)
async def publications(request: Request) -> HTMLResponse:
    """Publications page."""
    return templates.TemplateResponse(
        "publications.html",
        {"request": request, "active_page": "publications"},
    )


@app.get("/faq")
async def faq() -> RedirectResponse:
    """Redirect to documentation page (FAQ section)."""
    return RedirectResponse(url="/documentation#faq", status_code=301)


@app.get("/about", response_class=HTMLResponse)
async def about(request: Request) -> HTMLResponse:
    """About page."""
    return templates.TemplateResponse(
        "about.html",
        {"request": request, "active_page": "about"},
    )
