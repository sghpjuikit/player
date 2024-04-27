
import json
from typing import List
from util_wrt import Writer
from util_llm import Llm, ChatIntentDetect
from util_stt import Stt
from util_actor import Actor
from util_http import HttpHandler
from http.server import BaseHTTPRequestHandler, HTTPServer
from dataclasses import dataclass
from speech_recognition.audio import AudioData
from urllib.parse import parse_qs, urlparse
import traceback


class HttpHandlerState(HttpHandler):
    def __init__(self, actors: List[Actor]):
        super().__init__("GET", "/actor")
        self.actors = actors

    def __call__(self, req: BaseHTTPRequestHandler):
        try:
            state = {}
            for actor in self.actors:
                state[actor.group] = {
                    'name': actor.name,
                    'state': actor.state(),
                    'device': actor.deviceName,
                    'events processing': None if actor.processing_event is None else [ actor._get_event_text(actor.processing_event) ],
                    'events queued': len(actor.queued()),
                    'events processed': len(actor.events_processed),
                    'last processing time': actor.processingTimeLast(),
                    'avg processing time': actor.processingTimeAvg()
                }
            data = json.dumps(state).encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'application/json')
            req.end_headers()
            req.wfile.write(data)
        except Exception as e:
            traceback.print_exc()
            req.send_error(500, f"{e}")


class HttpHandlerStateActorEvents(HttpHandler):
    def __init__(self, actors: List[Actor]):
        super().__init__("GET", "/actor-events")
        self.actors = actors

    def __call__(self, req: BaseHTTPRequestHandler):
        try:
            query_params = parse_qs(urlparse(req.path).query)
            query_params.get('actor', [''])[0]
            actor_param = query_params.get('actor', [''])[0]
            type_param = query_params.get('type', [''])[0]
            state = {}
            for actor in self.actors:
                if actor.group == actor_param:
                    if type_param == 'PROCESSING':
                        state = None if actor.processing_event is None else [ actor._get_event_text(actor.processing_event) ]
                    if type_param == 'PROCESSED':
                        state = list(map(actor._get_event_text, actor.events_processed))
                    if type_param == 'QUEUED':
                        state = list(map(actor._get_event_text, actor.queued()))

            data = json.dumps(state).encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'application/json')
            req.end_headers()
            req.wfile.write(data)
        except Exception as e:
            traceback.print_exc()
            req.send_error(500, f"{e}")


@dataclass
class HttpHandlerIntentData:
    functions: str | None
    userPrompt: str

class HttpHandlerIntent(HttpHandler):
    def __init__(self, llm: Llm, sysPrompt: str):
        super().__init__("POST", "/intent")
        self.llm = llm
        self.sysPrompt = sysPrompt

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
            f = self.llm(ChatIntentDetect(self.sysPrompt if len(body.functions)==0 else body.functions, body.userPrompt, False))
            (command, canceled, commandIterator) = f.result()
            command = command.strip().removeprefix("COM-").removesuffix("-COM").strip().replace('-', ' ')
            command = command.replace('unidentified', body.userPrompt)
            command = 'unidentified' if len(command.strip())==0 else command
            command = 'unidentified' if canceled else command
            command = command.encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(command)
        except Exception as e:
            traceback.print_exc()
            req.send_error(500, f"{e}")


class HttpHandlerStt(HttpHandler):
    def __init__(self, stt: Stt):
        super().__init__("POST", "/stt")
        self.stt = stt

    def __call__(self, req: BaseHTTPRequestHandler):
        content_length = req.headers['Content-Length']
        if content_length is None: req.send_error(400, 'Invalid input')
        if content_length is None: return

        try:
            content_length = int(req.headers['Content-Length'])
            body = req.rfile.read(content_length)
            sample_rate = int.from_bytes(body[:4], 'little')
            sample_width = int.from_bytes(body[4:6], 'little')
            audio_data = body[6:]
            f = self.stt(AudioData(audio_data, sample_rate, sample_width), False)
            text = f.result()
            text = text.encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(text)
        except Exception as e:
            traceback.print_exc()
            req.send_error(500, f"{e}")

