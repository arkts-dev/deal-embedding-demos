"""Provider-native source operations. Only the portable core executes/admits them."""
import json
TURN_BOUNDARY='\nNATIVE TOOL TURN\n'

def tool(name, description, properties, required):
    return {'type':'function','function':{'name':name,'description':description,'strict':True,'parameters':{'type':'object','properties':properties,'required':required,'additionalProperties':False}}}
FILE={'type':'string','enum':['deal','dealui']}
SOURCE_TOOLS=[
 tool('write_file','Create one missing staged source file. Never overwrite an existing file; use edit_sources afterward.',{'file':FILE,'source':{'type':'string'}},['file','source']),
 tool('edit_sources','Atomically apply unique disjoint exact replacements to the current complete source pair. Host binds its revision. Preserve unrelated behavior.',{'edits':{'type':'array','items':{'type':'object','properties':{'file':FILE,'search':{'type':'string'},'replacement':{'type':'string'}},'required':['file','search','replacement'],'additionalProperties':False}}},['edits']),
 tool('read_file','Read one staged file, or an inclusive 1-based line range. Use null for both bounds to read the whole file. Range results contain numbered lines; their text is literal source. No other filesystem access.',{'file':FILE,'start_line':{'type':['integer','null'],'minimum':1},'end_line':{'type':['integer','null'],'minimum':1}},['file','start_line','end_line']),
 tool('check_sources','Compile the complete staged source pair. Source operations also trigger checking when both files exist.',{},[])
]

def obj(properties):
    return {'type':'object','properties':properties,'required':list(properties),'additionalProperties':False}
def arr(items): return {'type':'array','items':items}
TEXT={'type':'string'}
FIELD=obj({'name':TEXT,'type':TEXT})
def schema_tool(name,description,schema):
    return {'type':'function','function':{'name':name,'description':description,'strict':True,'parameters':schema}}
# Reuse the authoritative section subtree contract; provider schemas carry no admission policy.
from pathlib import Path
_SECTION_SCHEMA=json.loads((Path(__file__).resolve().parents[1]/'dependencies/deal-embedding/ui/fragment-contract.json').read_text())
def _resolve_section(value):
    if isinstance(value,list):return [_resolve_section(x) for x in value]
    if not isinstance(value,dict):return value
    if '$ref' in value:
        target=_SECTION_SCHEMA
        for part in value['$ref'].split('/')[1:]:target=target[part]
        return _resolve_section(target)
    return {k:_resolve_section(v) for k,v in value.items()}
_SECTION_CONFIG=next(x for x in _SECTION_SCHEMA['properties']['fragments']['items']['anyOf'] if x.get('properties',{}).get('kind',{}).get('enum')==['section'])['properties']['config']
HOLE_TOOLS=[
 schema_tool('submit_section','Fill only issued typed UI-section interfaces using an owned maintained subtree; never replace neighbors.',obj({'reason':TEXT,'sections':arr(obj({'id':TEXT,**_resolve_section(_SECTION_CONFIG)['properties']}))})),
 schema_tool('submit_custom','Fill only the exact issued pure function interfaces; preserve selected maintained parts.',obj({'reason':TEXT,'sections':arr(obj({'id':TEXT,'parameters':arr(FIELD),'result':TEXT,'body':TEXT,'arguments':arr(TEXT)}))})),
 tool('source_fallback','Request whole-source generation only when no retained task composition exists. Local failure or budget exhaustion never authorizes it; core enforces admission.',{'reason':TEXT},['reason'])]

def tools_for(body):
    _,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep: raise ValueError('Missing native tool turn')
    state=json.loads(state)
    if state.get('holes') is True:return HOLE_TOOLS
    if state.get('compact') is True:raise ValueError('Author-first compact model operations have been removed')
    if state.get('initialWrites') is True:return SOURCE_TOOLS
    return [t for t in SOURCE_TOOLS if t['function']['name']!='write_file']

def source_messages(body):
    prefix,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep:raise ValueError('Missing native tool turn')
    transcript=json.loads(state)['messages']
    if not isinstance(transcript,list):raise ValueError('Invalid native tool transcript')
    from gateway_transport import stable_messages
    messages=stable_messages(dict(body,input=prefix))
    if body.get('previous'):
        messages.append({'role':'user','content':'CURRENT CANONICAL SOURCE (exact decoded pair; sole repair authority)\n'+body['previous']+'\nCURRENT CHECK DIAGNOSTICS\n'+body.get('diagnostics','')})
    return messages+transcript

def initial_write_batch(body):
    # Host-authored workspace state, not a guess from transcript text or `previous`.
    _,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep:raise ValueError('Missing native tool turn')
    value=json.loads(state)
    return value.get('initialWrites') is True and value.get('compact') is not True and value.get('holes') is not True

def native_result(message):
    # Serialize only native message fields; hidden reasoning is never forwarded or retained.
    calls=message.get('tool_calls') or []
    return json.dumps({'role':'assistant','content':message.get('content') or '', 'tool_calls':calls},ensure_ascii=False)

def strip_reasoning(data):
    for choice in data.get('choices',[]):
        for key in ('reasoning','reasoning_content'):choice.get('message',{}).pop(key,None)
