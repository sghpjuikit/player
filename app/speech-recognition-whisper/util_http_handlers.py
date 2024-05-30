import json
import time
import soundfile as sf
from imports import *
from datetime import datetime
from typing import List
from util_wrt import Writer
from util_llm import Llm, ChatIntentDetect, ChatReact
from util_stt import Stt
from util_tts import TtsBase
from util_mic import Speech
from util_actor import Actor
from util_http import HttpHandler
from http.server import BaseHTTPRequestHandler, HTTPServer
from speech_recognition.audio import AudioData
from urllib.parse import parse_qs, urlparse


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
            print_exc()
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
                        state = [{"event": e, "processed from": t1, "processed to": t2, "processed in": t3} for e, t1, t2, t3 in zip(actor.events_processed, actor.processing_times_start, actor.processing_times_stop, actor.processing_times)]
                    if type_param == 'QUEUED':
                        state = list(map(actor._get_event_text, actor.queued()))

            data = json.dumps(state).encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'application/json')
            req.end_headers()
            req.wfile.write(data)
        except Exception as e:
            print_exc()
            req.send_error(500, f"{e}")

class HttpHandlerStateActorEventsAll(HttpHandler):
    def __init__(self, actors: List[Actor]):
        super().__init__("GET", "/actor-events-all")
        self.actors = actors

    def __call__(self, req: BaseHTTPRequestHandler):
        try:
            state = {}
            state["started time"] = min(map(lambda a: a.start_time, self.actors))
            state["now"] = time.time()
            state["events"] = {}

            for actor in self.actors:
                events = list(zip(actor.events_processed, actor.processing_times_start, actor.processing_times_stop, actor.processing_times))
                if actor.processing_event is not None: events.append((actor._get_event_text(actor.processing_event), actor.processing_start, None, time.time()-actor.processing_start))
                state["events"][actor.group] = [{"event": e, "processed from": t1, "processed to": t2, "processed in": t3} for e, t1, t2, t3 in events]

            data = json.dumps(state).encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'application/json')
            req.end_headers()
            req.wfile.write(data)
        except Exception as e:
            print_exc()
            req.send_error(500, f"{e}")

@dataclass
class HttpHandlerIntentData:
    functions: str | None
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
            f = self.llm(ChatIntentDetect.normal(body.functions, body.userPrompt, False).http())
            (command, canceled) = f.result()
            command = command.strip()
            command = command.replace('unidentified', body.userPrompt)
            command = 'unidentified' if len(command.strip())==0 else command
            command = 'unidentified' if canceled else command
            command = command.encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(command)
        except Exception as e:
            print_exc()
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
            f = self.stt(Speech(datetime.now(), AudioData(audio_data, sample_rate, sample_width), datetime.now(), 'ignored'), False)
            text = f.result().text
            text = text.encode('utf-8')
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(text)
        except Exception as e:
            print_exc()
            req.send_error(500, f"{e}")


@dataclass
class HttpHandlerTtsReactData:
    event_to_react_to: str
    fallback: str

class HttpHandlerTtsReact(HttpHandler):
    def __init__(self, llm: Llm, sysPrompt: str):
        super().__init__("POST", "/tts-event")
        self.llm = llm
        self.sysPrompt = sysPrompt

    def __call__(self, req: BaseHTTPRequestHandler):
        content_length = req.headers['Content-Length']
        if content_length is None: req.send_error(400, 'Invalid input')
        if content_length is None: return

        try:
            content_length = int(req.headers['Content-Length'])
            body = req.rfile.read(content_length)
            body = body.decode('utf-8')
            body = json.loads(body)
            body = HttpHandlerTtsReactData(**body)

            f = self.llm(ChatReact(self.sysPrompt, body.event_to_react_to, body.fallback).http())
            t, cancelled = f.result()
            req.send_response(200)
            req.send_header('Content-type', 'text/plain')
            req.end_headers()
            req.wfile.write(t.encode('utf-8'))
        except Exception as e:
            print_exc()
            req.send_error(500, f"{e}")


class HttpHandlerTts(HttpHandler):
    def __init__(self, tts: TtsBase):
        super().__init__('POST', '/speech')
        self.tts: TtsBase = tts

    def __call__(self, req: BaseHTTPRequestHandler):
        if self.tts._stop: return
        try:
            content_length = int(req.headers['Content-Length'])
            body = req.rfile.read(content_length)
            text = body.decode('utf-8')

            req.send_response(200)
            req.send_header('Content-type', 'application/octet-stream')
            req.end_headers()

            # generate
            audio_chunks = []
            event = self.tts.gen(text)
            stream = req.wfile

            if event.type == 'boundary':
                pass # boundary does not have data
            
            if event.type == 'e':
                pass # e does not have data
            
            if event.type == 'p':
                pass # p does not need generation nor api calls
                
            if event.type == 'f':
                audio_data, fs = sf.read(event.audio, dtype='float32')
                chunk_size = 1024
                audio_length = len(audio_data)
                start_pos = 0
                while start_pos < audio_length:
                    if req.wfile.closed: return
                    if tts._stop: req.wfile.close()
                    if tts._stop: return
            
                    end_pos = min(start_pos + chunk_size, audio_length)
                    chunk = audio_data[start_pos:end_pos]
                    stream.write(chunk)
                    stream.flush()
                    start_pos = end_pos
                    
            if event.type == 'b':
                for wav_chunk in event.audio:
                    if req.wfile.closed: return
                    if self.tts._stop: req.wfile.close()
                    if self.tts._stop: return
                    
                    stream.write(wav_chunk)
                    stream.flush()
                        
        except Exception as e:
            print_exc()
            if not stream.closed: stream.close()
