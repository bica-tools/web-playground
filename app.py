"""Reticulate web demo — FastAPI + HTMX.

Provides an interactive web interface for analyzing session types:
parse, build state space, check lattice, and render Hasse diagrams.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

# Ensure reticulate is importable from the project root.
_project_root = Path(__file__).resolve().parent.parent
_reticulate_root = _project_root / "reticulate"
if str(_reticulate_root) not in sys.path:
    sys.path.insert(0, str(_reticulate_root))

from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from reticulate import (
    LatticeResult,
    ParseError,
    build_statespace,
    check_lattice,
    check_termination,
    check_wf_parallel,
    dot_source,
    parse,
    pretty,
)

# Import benchmarks
sys.path.insert(0, str(_reticulate_root / "tests"))
from benchmarks.protocols import BENCHMARKS

# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

app = FastAPI(title="Reticulate — Session Type Analyzer")

_web_dir = Path(__file__).resolve().parent
app.mount("/static", StaticFiles(directory=str(_web_dir / "static")), name="static")
templates = Jinja2Templates(directory=str(_web_dir / "templates"))

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


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------


@app.get("/", response_class=HTMLResponse)
async def index(request: Request) -> HTMLResponse:
    """Render the main page with the input form and benchmark dropdown."""
    return templates.TemplateResponse(
        "index.html",
        {"request": request, "benchmarks": BENCHMARKS},
    )


@app.post("/analyze", response_class=HTMLResponse)
async def analyze(request: Request, type_string: str = Form(...)) -> HTMLResponse:
    """Run the full reticulate pipeline and return an HTMX fragment."""
    type_string = type_string.strip()
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
        },
    )
