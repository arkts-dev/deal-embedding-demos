"""Jev transport adapter for the existing choice envelope; no domain policy or retries."""
import json
import math
import urllib.request

ENDPOINT = 'https://api.typesafe.ai/v1/systemone'
MARKER = '\n\nISSUED OPTIONS\n'


def payload_for(input_text, model):
    state, separator, issued_text = input_text.rpartition(MARKER)
    if not separator:
        raise ValueError('Missing issued questions')
    issued = json.loads(issued_text)
    questions = issued['decisionQuestions']
    aliases = issued['questions']
    if not isinstance(questions, dict) or not questions or set(questions) != set(aliases) or len(questions) > 32:
        raise ValueError('Invalid issued questions')
    for name, question in questions.items():
        if not isinstance(name, str) or not name or not isinstance(question, dict) or set(question) != {'type', 'instructions', 'criteria'}:
            raise ValueError('Invalid question')
        criteria = question['criteria']
        if question['type'] != 'choice' or not isinstance(question['instructions'], str) or not question['instructions'] or not isinstance(criteria, dict) or not 1 <= len(criteria) <= 255:
            raise ValueError('Invalid choice question')
        options = aliases[name]
        if not isinstance(options, list) or any(not isinstance(v, str) for v in options) or len(set(options)) != len(options) or set(criteria) != set(options):
            raise ValueError('Question options disagree')
        if any(not key or not isinstance(description, str) or not description for key, description in criteria.items()):
            raise ValueError('Missing option descriptions')
    # Source-writing guidance is not chooser state. Domain instructions are in the questions.
    _, intent_marker, disclosed_state = state.partition('\n\nUSER INTENT\n')
    return {'model': model, 'state': disclosed_state if intent_marker else state, 'questions': questions}


def normalize(data, questions):
    """Validate typed provider fields; preserve unsupported choices for DEAL policy validation."""
    answers = data['answers']
    if not isinstance(answers, dict) or set(answers) != set(questions):
        raise ValueError('Invalid answer coverage')
    selected = {}
    for name, question in questions.items():
        answer = answers[name]
        if not isinstance(answer, dict) or answer.get('type') != 'choice' or not isinstance(answer.get('choice'), str):
            raise ValueError('Invalid choice answer')
        probabilities = answer.get('probabilities')
        if not isinstance(probabilities, dict) or set(probabilities) != set(question['criteria']):
            raise ValueError('Invalid probability coverage')
        def probability(value):
            return type(value) in (int, float) and math.isfinite(value) and 0 <= value <= 1
        if not all(probability(v) for v in probabilities.values()):
            raise ValueError('Invalid probability')
        # Jev rounds displayed probabilities to two decimals; do not require exact sum=1.
        if abs(sum(probabilities.values()) - 1) > len(probabilities) * 0.005 + 1e-9:
            raise ValueError('Invalid probability distribution')
        confidence = answer.get('confidence')
        if confidence is not None and not probability(confidence):
            raise ValueError('Invalid confidence')
        # An unissued string choice is an invalid decision, not a transport failure;
        # the existing DEAL validator rejects it and controls source fallback.
        selected[name] = answer['choice']
    typed = [{'question': name, 'choice': answers[name]['choice'],
              'probabilities': [{'option': option, 'probability': value} for option, value in answers[name]['probabilities'].items()],
              'confidence': answers[name].get('confidence')} for name in questions]
    return json.dumps({'answers': selected, 'typedAnswers': typed}, separators=(',', ':'))


def infer(body, key, model='jev-latest', open_url=None):
    payload = payload_for(body['input'], model)
    request = urllib.request.Request(ENDPOINT, data=json.dumps(payload).encode(), headers={
        'Content-Type': 'application/json', 'Authorization': 'Bearer ' + key,
    })
    with (open_url or urllib.request.urlopen)(request, timeout=60) as response:
        raw = response.read(2 * 1024 * 1024 + 1)
    if len(raw) > 2 * 1024 * 1024:
        raise ValueError('Jev response limit')
    data = json.loads(raw)
    content = normalize(data, payload['questions'])
    usage = data.get('usage') or {}
    def count(value):
        return value if type(value) is int and 0 <= value <= 2147483647 else None
    result = {'content': content, 'model': model, 'metadata': {
        'provider': 'typesafe', 'in': count(usage.get('input_tokens')),
        'out': count(usage.get('output_tokens')), 'cached': None,
        'finish': 'decision',
    }}
    if body['capture']:
        result.update(request=payload, response=data)
    encoded = json.dumps(result).encode()
    if len(encoded) > 2 * 1024 * 1024:
        del result['request']; del result['response']; result['captureOmitted'] = True
        encoded = json.dumps(result).encode()
    return encoded
