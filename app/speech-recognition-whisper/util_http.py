
import traceback
import json
from typing import List
from urllib.parse import urlparse
from threading import Thread
from util_wrt import Writer
from util_actor import Actor, Event
from http.server import BaseHTTPRequestHandler, HTTPServer


class HttpHandler:
    def __init__(self, method: str, path: str):
        self.method = method
        self.path = path

    def __call__(req: BaseHTTPRequestHandler):
        pass


class HttpHandlerState(HttpHandler):
    def __init__(self, actors: List[Actor]):
        super().__init__("GET", "/actor")
        self.actors = actors

    def __call__(self, req: BaseHTTPRequestHandler):
        def event_to_str(e) -> str:
            if e is None: return e
            if isinstance(e, str): return e
            if isinstance(e, (int, float)): return e
            if isinstance(e, Event): return e.str()
            else: return "n/a"

        state = {}
        for actor in self.actors:
            state[actor.group] = {
                'name': actor.name,
                'state': actor.state(),
                'events queued': list(map(event_to_str, actor.queued())),
                'events processed': actor.events_processed,
                'event processing': None if actor.processing_event is None else [ event_to_str(actor.processing_event) ],
                'event last processing time': actor.processingTimeLast(),
                'event avg processing time': actor.processingTimeAvg()
            }
        data = json.dumps(state).encode('utf-8')
        req.send_response(200)
        req.send_header('Content-type', 'application/json')
        req.end_headers()
        req.wfile.write(data)


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
                requested_path = urlparse(self.path)

                # find handler by path and invoke it
                for handler in http.handlers:
                    if handler.method == method and handler.path == requested_path.path:
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
            self.write("RAW: Http server starting...")
            self.server = HTTPServer((self.serverHost, self.serverPort), HttpRequestHandler)
            self.write(f"RAW: Http server started on {self.serverHost}:{self.serverPort}")
            self.server.serve_forever()
            self.write("RAW: Http server stopped")
        except Exception as e:
            self.write("ERR: error " + str(e))
            traceback.print_exc()

    def stop(self):
        if self.server is not None: self.server.shutdown()
        self.server = None
