"""Bounded provider streaming telemetry. Never persist reasoning text or execute partial output."""
import hashlib
import json
import time


def stable_messages(body):
    text = body['input']
    # The trusted core emits invariant guidance/contracts before variable request data.
    # A separate invariant system message remains identical across initial/repair calls.
    boundary = '\n\nUSER INTENT\n'
    prefix, sep, variable = text.partition(boundary)
    messages = [{'role': 'system', 'content': prefix}] if sep else []
    messages.append({'role': 'user', 'content': (boundary + variable) if sep else text})
    patch = 'REPAIR PROTOCOL exact-source-patch-v1' in text
    repairing = bool(body.get('previous')) or '\nSOURCE OPERATION\nrepair\n' in text
    if (repairing or patch) and (not patch or not body.get('previous')):
        raise ValueError('Repair requires a revision-bound exact patch base')
    if patch:
        messages.append({'role': 'user', 'content': 'Return only exact-source-patch-v1 edits; preserve unaffected source. No full-source fallback.\n' + body.get('diagnostics', '')})
    return messages


def read_stream(response, started, clock=time.monotonic):
    """SSE parser for Cerebras chat chunks. Timings are observed receipt times, not CPU time."""
    content = []
    total = 0
    content_bytes = 0
    first = reasoning_first = reasoning_last = content_first = content_last = None
    finish = None
    usage = {}
    time_info = {}
    model = ''
    identifier = ''
    done = False
    chunks = 0
    event = []

    def consume(lines):
        nonlocal first, reasoning_first, reasoning_last, content_first, content_last, finish, usage, time_info, model, identifier, chunks, done, content_bytes
        data = '\n'.join(lines)
        if data == '[DONE]':
            done = True
            return
        chunk = json.loads(data)
        now = (clock() - started) * 1000
        chunks += 1
        model = chunk.get('model', model)
        identifier = chunk.get('id', identifier)
        if chunk.get('usage'):
            usage = chunk['usage']
        if chunk.get('time_info'):
            time_info = chunk['time_info']
        for choice in chunk.get('choices', []):
            if choice.get('finish_reason'):
                finish = choice['finish_reason']
            delta = choice.get('delta') or {}
            reasoning = delta.get('reasoning') or delta.get('reasoning_content')
            text = delta.get('content')
            if reasoning or text:
                if first is None:
                    first = now
            if reasoning:
                if reasoning_first is None:
                    reasoning_first = now
                reasoning_last = now
            if text:
                if not isinstance(text, str):
                    raise ValueError('Invalid streaming content')
                if content_first is None:
                    content_first = now
                content_last = now
                content.append(text)
                content_bytes += len(text.encode())
                if content_bytes > 256 * 1024:
                    raise ValueError('Model content limit')

    for raw in response:
        total += len(raw)
        if total > 8 * 1024 * 1024:
            raise ValueError('Provider stream limit')
        line = raw.decode('utf-8').rstrip('\r\n')
        if not line:
            if event:
                consume(event)
                event = []
            if done:
                break
        elif line.startswith('data:'):
            event.append(line[5:].lstrip(' '))
    if event:
        consume(event)
    if not done or finish is None:
        raise ValueError('Incomplete provider stream')
    if finish != 'stop':
        raise ValueError('Provider stream did not complete normally')
    elapsed = (clock() - started) * 1000
    phases = dict(firstOutputMs=first, firstReasoningMs=reasoning_first, lastReasoningMs=reasoning_last,
                  firstContentMs=content_first, lastContentMs=content_last, totalMs=elapsed, chunks=chunks,
                  reasoningSpanMs=None if reasoning_first is None else reasoning_last - reasoning_first,
                  contentSpanMs=None if content_first is None else content_last - content_first)
    data = {'id': identifier, 'model': model, 'choices': [{'message': {'role': 'assistant', 'content': ''.join(content)}, 'finish_reason': finish}],
            'usage': usage, 'time_info': time_info}
    return data, phases


def prefix_fingerprint(messages):
    prefix = messages[0]['content'] if messages and messages[0]['role'] == 'system' else ''
    return hashlib.sha256(prefix.encode()).hexdigest()
