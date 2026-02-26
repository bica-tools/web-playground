"""Tests for the password gate (middleware, login, logout)."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from web.app import SITE_PASSWORD, _SESSION_TOKEN, app


@pytest.fixture()
def client() -> TestClient:
    return TestClient(app)


# ---------------------------------------------------------------------------
# Middleware redirects
# ---------------------------------------------------------------------------


class TestMiddlewareRedirects:
    """Unauthenticated requests should redirect to /login."""

    def test_root_redirects(self, client: TestClient) -> None:
        resp = client.get("/", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"

    def test_tool_redirects(self, client: TestClient) -> None:
        resp = client.get("/tool", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"

    def test_theory_redirects(self, client: TestClient) -> None:
        resp = client.get("/theory", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"

    def test_benchmarks_redirects(self, client: TestClient) -> None:
        resp = client.get("/benchmarks", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"

    def test_logout_redirects(self, client: TestClient) -> None:
        resp = client.get("/logout", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"


# ---------------------------------------------------------------------------
# Allowed paths (no auth needed)
# ---------------------------------------------------------------------------


class TestAllowedPaths:
    """Login page and static assets should be accessible without auth."""

    def test_login_page_accessible(self, client: TestClient) -> None:
        resp = client.get("/login")
        assert resp.status_code == 200
        assert "Password" in resp.text

    def test_static_accessible(self, client: TestClient) -> None:
        resp = client.get("/static/style.css")
        # 200 if file exists, 404 if not — but NOT 302
        assert resp.status_code != 302


# ---------------------------------------------------------------------------
# Login flow
# ---------------------------------------------------------------------------


class TestLoginFlow:
    """POST /login with correct/incorrect password."""

    def test_wrong_password_redirects_with_error(self, client: TestClient) -> None:
        resp = client.post(
            "/login",
            data={"password": "wrong"},
            follow_redirects=False,
        )
        assert resp.status_code == 302
        assert "error=1" in resp.headers["location"]

    def test_correct_password_sets_cookie(self, client: TestClient) -> None:
        resp = client.post(
            "/login",
            data={"password": SITE_PASSWORD},
            follow_redirects=False,
        )
        assert resp.status_code == 302
        assert resp.headers["location"] == "/"
        # Cookie should be set
        assert "session_token" in resp.cookies

    def test_authenticated_access(self, client: TestClient) -> None:
        # Login first
        client.post(
            "/login",
            data={"password": SITE_PASSWORD},
            follow_redirects=False,
        )
        # Now access protected route (TestClient carries cookies)
        resp = client.get("/tool", follow_redirects=False)
        assert resp.status_code == 200

    def test_login_page_shows_error(self, client: TestClient) -> None:
        resp = client.get("/login?error=1")
        assert resp.status_code == 200
        assert "Incorrect password" in resp.text

    def test_login_page_no_error(self, client: TestClient) -> None:
        resp = client.get("/login")
        assert resp.status_code == 200
        assert "Incorrect password" not in resp.text


# ---------------------------------------------------------------------------
# Logout
# ---------------------------------------------------------------------------


class TestLogout:
    """GET /logout should clear the cookie."""

    def test_logout_clears_session(self, client: TestClient) -> None:
        # Login
        client.post(
            "/login",
            data={"password": SITE_PASSWORD},
            follow_redirects=False,
        )
        # Verify access
        resp = client.get("/tool", follow_redirects=False)
        assert resp.status_code == 200

        # Logout
        resp = client.get("/logout", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"

        # Should be blocked again
        resp = client.get("/tool", follow_redirects=False)
        assert resp.status_code == 302
        assert resp.headers["location"] == "/login"


# ---------------------------------------------------------------------------
# Nav hidden on login page
# ---------------------------------------------------------------------------


class TestLoginTemplate:
    """Login page should not show the nav bar."""

    def test_no_nav_on_login(self, client: TestClient) -> None:
        resp = client.get("/login")
        assert resp.status_code == 200
        assert "nav-links" not in resp.text

    def test_nav_on_protected_pages(self, client: TestClient) -> None:
        # Login first
        client.post(
            "/login",
            data={"password": SITE_PASSWORD},
            follow_redirects=False,
        )
        resp = client.get("/tool")
        assert resp.status_code == 200
        assert "nav-links" in resp.text
