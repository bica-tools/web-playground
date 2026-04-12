#!/usr/bin/env python3
"""Simple HTTP server to serve the Orbital Dashboard locally.

Usage:
    python3 serve.py          # serves on http://localhost:8080
    python3 serve.py 9000     # serves on http://localhost:9000

Open orbital-dashboard.html in your browser at the served URL.
"""
import http.server
import os
import sys


def main() -> None:
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    directory = os.path.dirname(os.path.abspath(__file__))
    os.chdir(directory)

    handler = http.server.SimpleHTTPRequestHandler
    with http.server.HTTPServer(("", port), handler) as httpd:
        print(f"Serving Orbital Dashboard at http://localhost:{port}/orbital-dashboard.html")
        print("Press Ctrl+C to stop.")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nStopped.")


if __name__ == "__main__":
    main()
