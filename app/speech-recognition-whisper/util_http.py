from imports import *
from typing import List
from urllib.parse import urlparse
from util_wrt import Writer
from http.server import BaseHTTPRequestHandler, HTTPServer


class HttpHandler:
    def __init__(self, method: str, path_to_match: str):
        self.method = method
        self.path_to_match = path_to_match

    def __call__(req: BaseHTTPRequestHandler):
        pass


class Http:

    def __init__(self, serverHost: str | None, serverPort: int | None, write: Writer):
        self.serverHost = serverHost
        self.serverPort = serverPort
        self.server = None
        self.write = write
        self.handlers: List[HttpHandler] = []

    def start(self):
        Thread(name='Http', target=self.start_impl, daemon=True).start()

    def start_impl(self):
        if self.serverHost is None: return
        if self.serverPort is None: return
        http = self

        class HttpRequestHandler(BaseHTTPRequestHandler):

            def do_XXX(self, method: str):
                # help

                if 'GET' == method and '/' == urlparse(self.path).path:
                    self.send_response(200)
                    self.send_header('Content-type', 'text/plain')
                    self.end_headers()
                    for handler in http.handlers:
                        self.wfile.write(f'{handler.method} {handler.path_to_match}\n'.encode('utf-8'))
                    return

                # find handler by path and invoke it
                for handler in http.handlers:
                    if handler.method == method and handler.path_to_match == urlparse(self.path).path:
                        handler(self)
                        return

                # or 404
                self.send_error(404, "Not Found")

            def log_message(self, format, *args):
                pass

            def do_GET(self): self.do_XXX("GET")

            def do_POST(self): self.do_XXX("POST")

            def do_PUT(self): self.do_XXX("PUT")

        try:
            self.write(f"RAW: Http server {self.serverHost}:{self.serverPort} starting...")
            self.server = HTTPServer((self.serverHost, self.serverPort), HttpRequestHandler)
            self.write(f"RAW: Http server started {self.serverHost}:{self.serverPort}")
            self.server.serve_forever()
            self.write("RAW: Http server stopped")
        except Exception as e:
            self.write("ERR: error " + str(e))
            print_exc()

    def stop(self):
        if self.server is not None: self.server.shutdown()
        self.server = None
