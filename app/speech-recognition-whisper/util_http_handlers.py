
import json
from typing import List
from util_wrt import Writer
from util_llm import Llm, ChatIntentDetect
from util_actor import Actor, Event
from util_http import HttpHandler
from http.server import BaseHTTPRequestHandler, HTTPServer
from dataclasses import dataclass


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
                'device': actor.deviceName,
                'event processing': None if actor.processing_event is None else [ event_to_str(actor.processing_event) ],
                'events queued': list(map(event_to_str, actor.queued())),
                'events processed': actor.events_processed,
                'last processing time': actor.processingTimeLast(),
                'avg processing time': actor.processingTimeAvg()
            }
        data = json.dumps(state).encode('utf-8')
        req.send_response(200)
        req.send_header('Content-type', 'application/json')
        req.end_headers()
        req.wfile.write(data)


@dataclass
class HttpHandlerIntentData:
    functions: str
    userPrompt: str

class HttpHandlerIntent(HttpHandler):
    def __init__(self, llm: Llm):
        super().__init__("POST", "/intent")
        self.llm = llm

    def __call__(self, req: BaseHTTPRequestHandler):
        content_length = req.headers['Content-Length']
        if content_length is None: req.send_error(400, 'Invalid input')
        if content_length is None: return

        content_length = int(req.headers['Content-Length'])
        body = req.rfile.read(content_length)
        body = body.decode('utf-8')
        body = json.loads(body)
        body = HttpHandlerIntentData(**body)

        try:
            f = self.llm(ChatIntentDetect(body.functions, body.userPrompt, False))
            data: str = f.result()
            data = data.removeprefix("COM-").removesuffix("-COM").strip().replace('-', ' ')
            data = data.encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(data)
        except Exception as e:
            req.send_error(500, f"{e}")

